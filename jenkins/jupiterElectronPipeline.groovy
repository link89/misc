// This job support both triggered via manual and gitlab events.
import jenkins.model.*

import java.net.URI

class Context {
    String gitCredentialId

    String gitSourceUrl
    String gitSourceNamespace
    String gitSourceBranch

    String gitTargetUrl
    String gitTargetNamespace
    String gitTargetBranch

    String npmRegistry
    String buildUrl
    String buildDescription

    Boolean buildMacStatus = false
    Boolean buildWinStatus = false

    String head

    Boolean skipUT

    URI deployTarget
    String deployCredentialId

    String jobName
    String buildId


    Boolean useMirror
    String mirrorUrl
    String buildNumber
    String deployDir

    Boolean getIsMerge() {
        gitTargetBranch != gitSourceBranch
    }
    long timestamp

    static String tagToGlipLink(String text) {
        text.replaceAll(/<a\b[^>]*?href="(.*?)"[^>]*?>(.*?)<\/a>/, '[$2]($1)')
    }

    String getBuildResult() {
        buildMacStatus && buildWinStatus ? "Success".toString() :"Failed".toString()
    }

    String getWebHookICon() {
        buildMacStatus && buildWinStatus ? "https://img.icons8.com/cute-clipart/48/000000/ok.png".toString() : "https://img.icons8.com/color/48/000000/close-window.png".toString()
    }

    String getDownloadUrl() {
        "[Click Here](https://electron.fiji.gliprc.com/downloads-all/${deployDir}/)"
    }

    String getGlipReport() {
        List<String> lines = []
        if (buildResult)
            lines.push("**Build Result**: ${buildResult}")
        if (buildDescription)
            lines.push("**Description**: ${tagToGlipLink(buildDescription)}")
        if (buildUrl)
            lines.push("**Job**: [${jobName}#${buildId}](${buildUrl}flowGraphTable/)")
        if (deployDir)
            lines.push("**Download Url**: ${downloadUrl}")

        lines.join('\\n')
    }
}


class ElectronCIJob {
    def jenkins
    Context context
    String nodeLabel
    String nodejsHome

    String getArtifacts() {
        ""
    }

    String formatPath(String p) {
        return jenkins.sh(returnStdout: true, script: "cygpath -u '${p}' || true").trim() ?: p
    }

    void prepareStage() {
        jenkins.sh 'git clean -xdf || true'
        jenkins.sh 'git config --global user.email "pipeline@company.com"'
        jenkins.sh 'git config --global user.name "pipeline"'
        jenkins.sh "npm config set registry=${context.npmRegistry}"
        jenkins.sh 'npm list -g yarn || npm install --ignore-scripts -g yarn'
        jenkins.sh 'npm list -g lerna || npm install --ignore-scripts -g lerna'
        jenkins.sh 'yarn config set unsafe-perm true'
    }

    void checkoutStage() {
        jenkins.checkout([
                $class           : 'GitSCM',
                branches         : [[name: "${context.gitSourceNamespace}/${context.gitSourceBranch}"]],
                extensions       : [
                        [$class: 'PruneStaleBranch'],
                        [
                                $class : 'PreBuildMerge',
                                options: [
                                        fastForwardMode: 'FF',
                                        mergeRemote    : context.gitTargetNamespace,
                                        mergeTarget    : context.gitTargetBranch,
                                ]
                        ]
                ],
                userRemoteConfigs: [
                        [
                                credentialsId: context.gitCredentialId,
                                name         : context.gitTargetNamespace,
                                url          : context.gitTargetUrl,
                        ],
                        [
                                credentialsId: context.gitCredentialId,
                                name         : context.gitSourceNamespace,
                                url          : context.gitSourceUrl,
                        ]
                ]
        ])
        context.head = jenkins.sh(returnStdout: true, script: 'git rev-parse HEAD').trim().substring(0, 9)

        String versionFile = 'application/src/modules/version/browser/versionInfo.json'
        jenkins.sh "sed 's/{{buildNumber}}/${context.buildNumber}/;s/{{buildCommit}}/${context.head}/;s/{{buildTime}}/${context.timestamp}/' ${versionFile} > versionInfo.json || true"
        jenkins.sh "mv versionInfo.json ${versionFile} || true"
    }

    void installDependencyStage() {
        jenkins.sshagent(credentials: [context.gitCredentialId]) {
            jenkins.sh 'yarn install --force'
            // jenkins.sh 'rm -rf application/node_modules/electron/node_modules/@types/node/'
        }
    }

    void unitTestStage() {
        if (context.skipUT) return
        jenkins.sh 'yarn test'
    }

    void staticAnalysisStage() {
        jenkins.sh 'echo TBD'
    }

    void buildStage() {
    }

    void deployStage() {
        // dirname = base version + build number + source branch + target branch(optional) + commit sha
        String dirname = jenkins.sh(returnStdout: true, script: '''node -p "require('./package.json').version"''').trim()
        dirname += '-' + context.buildNumber
        dirname += '-' + context.gitSourceBranch.replaceAll(/[\/\.]/, '-').toLowerCase()
        if (context.isMerge) {
            String suffix = context.gitTargetBranch.replaceAll(/[\/\.]/, '-').toLowerCase()
            dirname += ('-to-' + suffix)
        }
        dirname += '-' + context.head

        context.deployDir = dirname

        URI uri = context.deployTarget
        String target = "${uri.getPath()}/${dirname}".toString()
        String remoteTarget = "${uri.getUserInfo()}@${uri.getHost()}:${target}".toString()

        // do copy
        jenkins.sshagent(credentials: [context.deployCredentialId]) {
            jenkins.sh "ssh -o StrictHostKeyChecking=no -p ${uri.getPort() ?: 22} ${uri.getUserInfo()}@${uri.getHost()} 'mkdir -p ${target}'"
            jenkins.sh "scp -o StrictHostKeyChecking=no -P ${uri.getPort() ?: 22} ${artifacts} ${remoteTarget} || true"
        }
    }

    void run() {
    }

    void doRun() {
        jenkins.node(nodeLabel) {
            nodejsHome = formatPath(jenkins.tool("jupiter-electron")) // install nodejs tool
            String envPath = "${nodejsHome}:${nodejsHome}/bin".toString()
            def envVars = [
                    // "CSC_IDENTITY_AUTO_DISCOVERY=false",
                    "DEBUG=electron-builder",
                    "PATH+NODEJS=${envPath}",
                    "NODEJS_HOME=${nodejsHome}",
                    "SENTRYCLI_CDNURL=https://cdn.npm.taobao.org/dist/sentry-cli",
                    "UV_THREADPOOL_SIZE=128",
            ]

            if (context.useMirror) {
                envVars.push("NPM_CONFIG_ELECTRON_MIRROR=${context.mirrorUrl}")
            }

            jenkins.withEnv(envVars) {
                jenkins.sh('env')
                jenkins.stage('prepare') { prepareStage() }
                jenkins.stage('checkout') { checkoutStage() }
                jenkins.stage('install dependencies') { installDependencyStage() }
                jenkins.stage('unit test') { unitTestStage() }
                jenkins.stage('static analysis') { staticAnalysisStage() }
                jenkins.stage('build') { buildStage() }
                jenkins.stage('deploy') { deployStage() }
            }
        }
    }
}

class ElectronMacJob extends ElectronCIJob {

    String getArtifacts() {
        // here we need to use gun find or else printf not work, to install: brew install findutils
        String cmd = ''' gfind ./application/dist  -maxdepth 1 -type f \\( -name "*.dmg" -or -name "*.zip" -or -name "*.yml" \\) -printf '"%p" ' '''
        jenkins.sh(returnStdout: true, script: cmd).trim()
    }


    String macCscFileId
    String macCscKeyPasswordId

    void buildStage() {
        jenkins.withCredentials([
                jenkins.file(credentialsId: macCscFileId, variable: 'CSC_LINK'),
                jenkins.string(credentialsId: macCscKeyPasswordId, variable: 'CSC_KEY_PASSWORD'),
        ]) {
            for (Integer i = 0; i < 3; i++) {
                try {
                    jenkins.sh 'yarn pack:mac > ./output.txt  2>&1'
                    return
                } catch (e) {
                    Boolean canRetry = jenkins.sh(returnStatus: true, script: 'grep "A timestamp was expected but was not found." ./output.txt') == 0
                    if (!canRetry || i + 1 == 3) {
                       jenkins.error('failed to build mac')
                    }
                    jenkins.sh 'echo "code sign timeout, retry..."'
                } finally {
                    jenkins.sh 'cat ./output.txt'
                }
            }
        }
    }

    void run() {
        try {
            doRun()
            context.buildMacStatus = true
        } catch (e) {
            context.buildMacStatus = false
            throw e
        }
    }
}

class ElectronWinJob extends ElectronCIJob {

    String getArtifacts() {
        String cmd = ''' find ./application/dist  -maxdepth 1 -type f \\( -name "*.msi" -or -name "*.exe" -or -name "*.yml" \\) -printf '"%p" ' '''
        jenkins.sh(returnStdout: true, script: cmd).trim()
    }

    String winCscFileId
    String winCscKeyPasswordId

    void prepareStage() {
        super.prepareStage()
        // I have  to patch git.js for cygwin due to a known issue: https://github.com/npm/npm/issues/7357
        // Other solutions like git-bash, powershell, cmd have already been tested.
        // It turns out cygwin is our best choice to work with jenkins.
        // That's the reason I choose to hack npm instead.
        // You should be very cautious with following code and update them once npm has incompatible upgrade.
        String patchFile = 'npm-cygwin-patch.diff'
        String patch = '''  gitArgs = gitArgs.map(arg => arg.replace(/\\\\/g, '/').replace(/^([A-Za-z])\\:\\//, '/cygdrive/$1/')) /* npm cygwin patch */\n'''
        String gitJs = "${nodejsHome}/node_modules/npm/node_modules/pacote/lib/util/git.js".toString()
        jenkins.writeFile file: patchFile, text: patch, encoding: 'utf-8'
        jenkins.sh """ sed -i "/npm cygwin patch/d" ${gitJs} """
        jenkins.sh """ sed -i "/^function execGit/r ${patchFile}" ${gitJs} """
        jenkins.sh "cat ${gitJs}"
    }

    void buildStage() {
        jenkins.withCredentials([
                jenkins.file(credentialsId: winCscFileId, variable: 'CSC_LINK'),
                jenkins.string(credentialsId: winCscKeyPasswordId, variable: 'CSC_KEY_PASSWORD'),
        ]) {
            jenkins.sh 'yarn pack:win'
        }
    }

    void run() {
        try {
            doRun()
            context.buildWinStatus = true
        } catch (e) {
            context.buildWinStatus = false
            throw e
        }
    }
}

// Initiate default context variable by user data
Context context = new Context(
        gitCredentialId: params.GIT_CREDENTIAL,
        gitSourceUrl: params.GIT_URL,
        gitSourceBranch:  params.GIT_BRANCH,
        gitSourceNamespace: params.GIT_NAMESPACE,

        gitTargetUrl: params.GIT_TARGET_URL,
        gitTargetBranch:  params.GIT_TARGET_BRANCH,
        gitTargetNamespace: params.GIT_TARGET_NAMESPACE,

        npmRegistry: params.NPM_REGISTRY,
        buildUrl                    : env.BUILD_URL,

        deployTarget: new URI(params.DEPLOY_TARGET),
        deployCredentialId: params.DEPLOY_CREDENTIAL,

        useMirror: params.USE_MIRROR,
        mirrorUrl: params.MIRROR_URL,
        jobName: env.JOB_NAME,
        buildId: env.BUILD_ID,
)

// Override default context variable if this job is triggered by gitlab events
context.gitSourceUrl       = env.gitlabSourceRepoSshUrl?: context.gitSourceUrl
context.gitSourceBranch    = env.gitlabSourceBranch ?: context.gitSourceBranch
context.gitSourceNamespace = env.gitlabSourceNamespace ?: context.gitSourceNamespace

context.gitTargetUrl       = env.gitlabTargetRepoSshUrl?: context.gitTargetUrl ?: context.gitSourceUrl
context.gitTargetBranch    = env.gitlabTargetBranch ?: context.gitTargetBranch ?: context.gitSourceBranch
context.gitTargetNamespace = env.gitlabTargetNamespace ?: context.gitTargetNamespace ?: context.gitSourceNamespace

context.skipUT = params.SKIP_UT
context.buildNumber = env.BUILD_NUMBER
context.timestamp =  System.currentTimeMillis()


class JupiterJob {
    Context context
    def jenkins
    def params

    void sendResultToGlip() {
        jenkins.sh "curl webhook (pseudo)"
    }

    void updateGitlabStatus(String state) {
        context.isMerge && jenkins.updateGitlabCommitStatus(name: 'jenkins', state: state)
    }

    void run() {
        // get started
        updateGitlabStatus('pending')
        def jobs = []
        if (params.WIN_BUILD_NODE) {
            jobs.push(
                    new ElectronWinJob(
                            winCscFileId: params.WIN_CSC_FILE,
                            winCscKeyPasswordId: params.WIN_CSC_PASSWORD,
                            nodeLabel: params.WIN_BUILD_NODE,
                            context: context,
                            jenkins: jenkins)
            )
        } else {
            context.buildWinStatus = true
        }

        if (params.MAC_BUILD_NODE) {
            jobs.push(
                    new ElectronMacJob(
                            macCscFileId: params.MAC_CSC_FILE,
                            macCscKeyPasswordId: params.MAC_CSC_PASSWORD,
                            nodeLabel: params.MAC_BUILD_NODE,
                            context: context,
                            jenkins: jenkins)
            )
        } else {
            context.buildMacStatus = true
        }

        jenkins.node(params.WIN_BUILD_NODE ? params.WIN_BUILD_NODE : params.MAC_BUILD_NODE) {
            try {
                updateGitlabStatus('running')
                jenkins.parallel jobs.collectEntries { job ->
                    [job.nodeLabel, {
                        job.run()
                    }]
                }
            } finally {
                context.buildDescription = jenkins.currentBuild.getDescription()

                if (context.buildMacStatus && context.buildWinStatus) {
                    updateGitlabStatus('success')
                } else {
                    updateGitlabStatus('failed')
                }
                sendResultToGlip()
            }
        }
    }
}

JupiterJob job = new JupiterJob(jenkins: this, context: context, params: params)
job.run()
