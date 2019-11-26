import jenkins.model.*
import java.net.URI
import java.util.TimeZone

def sshCmd(URI uri, String credentialId, String cmd) {
    String sshCmd = "ssh -q -o StrictHostKeyChecking=no -p ${uri.getPort()} ${uri.getUserInfo()}@${uri.getHost()}".toString()
    sshagent(credentials: [credentialId]) {
        return sh(returnStdout: true, script: "${sshCmd} \"${cmd}\"").trim()
    }
}

def copyToRemote(URI uri, String credentialId, String source, String target) {
    String remoteTarget = "${uri.getUserInfo()}@${uri.getHost()}:${target}".toString()
    sshagent(credentials: [credentialId]) {
        sh "rsync -azPq --delete --progress -e 'ssh -o StrictHostKeyChecking=no -p ${uri.getPort()}' ${source} ${remoteTarget}"
    }
}

class Context {
    String buildNode
    String buildNumber
    Boolean buildOnly
    String buildType
    Boolean gzipCompress
    String nodejsTool

    long timestamp
    String timeLabel

    String gitUrl
    String gitCredentialId
    String gitBranch
    String gitHead

    String npmRegistry

    String buildDirectory
    String buildPackageName

    URI[] deployTargets
    String deployCredentialId
    String deployDirectory
}

def prepareStage(Context context) {
    env.NODE_OPTIONS = '--max_old_space_size=4096'
    env.NODEJS_HOME = tool context.nodejsTool // install nodejs tool
    env.PATH="${env.NODEJS_HOME}/bin:${env.PATH}"  // set nodejs path
    env.TZ='UTC-8'  // set timezone
    sh 'env'  // print environment variable for debug
}

def checkoutStage(Context context) {
    checkout ([
            $class: 'GitSCM',
            branches: [[name: "*/${context.gitBranch}"]],
            userRemoteConfigs: [
                    [
                            url: context.gitUrl,
                            credentialsId: context.gitCredentialId,
                    ],
            ]
    ])
    sh 'git clean -xdf'
    context.gitHead = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
}


def installDependencyStage(Context context) {
    sh "echo 'registry=${context.npmRegistry}' > .npmrc"
    sshagent (credentials: [context.gitCredentialId]) {
        sh 'npm config set grpc-node-binary-host-mirror https://npm.taobao.org/mirrors'
        sh 'npm install @sentry/cli@1.40.0 --unsafe-perm'
        sh 'npm install --unsafe-perm'
    }
}


def buildStage(Context context) {
    // collect version information
    sh 'npx ts-node application/src/containers/VersionInfo/GitRepo.ts'
    sh 'mv commitInfo.ts application/src/containers/VersionInfo/'
    sh "sed 's/{{buildNumber}}/${context.buildNumber}/;s/{{buildCommit}}/${context.gitHead.substring(0, 9)}/;s/{{buildTime}}/${context.timestamp}/' application/src/containers/VersionInfo/versionInfo.json > versionInfo.json || true"
    sh 'mv versionInfo.json application/src/containers/VersionInfo/versionInfo.json || true'

    // use different build command for different build type
    if ('PUBLIC'.equalsIgnoreCase(context.buildType)) {
        sh 'npm run build:public'
    } else if ('STAGE'.equalsIgnoreCase(context.buildType)) {
        sh 'npm run build:ops_stage'
    } else if ('RELEASE'.equalsIgnoreCase(context.buildType)) {
        sh 'npm run build:release'
    } else {
        error("unsupport build type: ${context.buildType}")
    }

    // clean up useless file
    dir(context.buildDirectory) {
        sh "rm whiteListedId.json"
    }

    // update version info
    if (context.gzipCompress) {
        // in order to support gzip_static, we should create gz file after build, detail: FIJI-7895
        dir(context.buildDirectory) {
            sh """find . -type f -size +150c \\( -name "*.wasm" -o -name "*.css" -o -name "*.html" -o -name "*.js" -o -name "*.json" -o -name "*.map" -o -name "*.svg"  -o -name "*.xml" \\) | xargs -I{} bash -c 'gzip -9 < {} > {}.gz'"""
        }
    }

    // create tarball and archive it
    context.buildPackageName = "${context.buildType}-${context.buildNumber}-${context.gitBranch.replaceAll(/[\/\.]/, '-')}-${context.gitHead.substring(0,9)}-${context.timeLabel}.tar.gz".toString()
    sh "tar -czvf ${context.buildPackageName} -C ${context.buildDirectory} ."
    archiveArtifacts artifacts: context.buildPackageName, fingerprint: true
}

def deployStage(Context context) {
    // skip deploy stage if BUILD_ONLY is set
    if (context.buildOnly) return

    // the reason we break it into two parts is copy package to remote machine may take a long time
    // we hope that both machine updated at the same time
    // so we first copy package,
    // and then clean up the old version and unpack the new one

    // step 1: copy package to remote target
    parallel context.deployTargets.collectEntries { deployTarget -> [deployTarget.toString(), {
        // ensure dir exists
        sshCmd(deployTarget, context.deployCredentialId, "mkdir -p ${context.deployDirectory}".toString())
        // copy package to target machine
        copyToRemote(deployTarget, context.deployCredentialId, context.buildPackageName, context.deployDirectory)
    }]}

    // step 2: clean old version and unpack new one
    parallel context.deployTargets.collectEntries { deployTarget -> [deployTarget.toString(), {
        // clean old deployment
        // sshCmd(deployTarget, context.deployCredentialId,
        //    "find ${context.deployDirectory} -type f -not -name '*.tar.gz' | xargs rm".toString())
        // unpack package
        sshCmd(deployTarget, context.deployCredentialId,
                "tar -xvmf ${context.deployDirectory}/${context.buildPackageName} -C ${context.deployDirectory}".toString())
    }]}
    sh "npm run releaseToSentry || true"
}

// get started
Context context = new Context(
        buildNode: params.BUILD_NODE ?: env.BUILD_NODE,
        buildNumber: env.BUILD_NUMBER,
        buildOnly: params.BUILD_ONLY,
        buildType: params.BUILD_TYPE,
        gzipCompress: params.GZIP_COMPRESS,

        timestamp: System.currentTimeMillis(),
        timeLabel: new Date().format("yyyyMMddhhmmss", TimeZone.getTimeZone("Asia/Shanghai")),

        gitUrl: params.GIT_URL,
        gitCredentialId: params.GIT_CREDENTIAL,
        gitBranch:  params.GIT_BRANCH,

        nodejsTool: params.NODEJS_TOOL,
        npmRegistry: params.NPM_REGISTRY,

        buildDirectory: 'application/build',
        deployTargets:  params.DEPLOY_TARGETS.trim().split('\n').collect{ new URI(it) },
        deployCredentialId: params.DEPLOY_CREDENTIAL,
        deployDirectory: params.DEPLOY_DIRECTORY,
)


node(context.buildNode) {
    env.SENTRYCLI_CDNURL='https://cdn.npm.taobao.org/dist/sentry-cli'
    env.CI='false'
    stage('prepare') {prepareStage(context)}
    stage('checkout') {checkoutStage(context)}
    stage('install dependencies') {installDependencyStage(context)}
    stage('build') {buildStage(context)}
    stage('deploy') {deployStage(context)}
}
