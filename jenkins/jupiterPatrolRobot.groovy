// This job support both triggered via manual and gitlab events.
import jenkins.model.*

class Context {
    String gitCredentialId

    String gitSourceUrl
    String gitSourceNamespace
    String gitSourceBranch

    String gitTargetUrl
    String gitTargetNamespace
    String gitTargetBranch
    String head

    String npmRegistry
    String nodeJsTool

    String siteUrl
    String siteEnv
    String platformCredentialId
    String fixtures

    String influxdbUrl
    String autoSkipCmd

    List<String> browsers
    String seleniumServer
    String seleniumCaps
    String dateString

    String jobTag
    int concurrency
    int maxRetry
    int chuckSize
}


class AutomationJob {

    def jenkins
    String nodeLabel
    Context context


    void checkoutStage() {
        preCheckout()
        jenkins.checkout([
                $class     : 'GitSCM',
                branches   : [[name: "${context.gitSourceNamespace}/${context.gitSourceBranch}"]],
                extensions : [
                        [$class: 'CloneOption', noTags: false, reference: '', shallow: true, depth: 1000],
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
        postCheckout()
    }

    void preCheckout() {
        jenkins.sh '''
        git clean -xdf || true
        git config --global user.email "pipeline@company.com"
        git config --global user.name "pipeline"
        git config --global http.postBuffer 500M
        git config --global http.maxRequestBuffer 100M
        git config --global core.compression 0
        '''
    }

    void postCheckout() {
        context.head = jenkins.sh(returnStdout: true, script: 'git rev-parse HEAD').trim().substring(0, 9)
    }

    void installDependencyStage() {
        jenkins.sshagent(credentials: [context.gitCredentialId]) {
            jenkins.sh "npm config set registry=${context.npmRegistry}"
            jenkins.sh 'npm install minipass@2.7.0'
            jenkins.sh 'npm install --unsafe-perm --cache-min=9999'
        }
    }


    def e2eTasks() {
        jenkins.sh 'mkdir -p $SCREENSHOTS_PATH $TMPFILE_PATH'
        jenkins.sh "RUN_NAME=[Jupiter][${context.jobTag}][${context.gitSourceBranch}][${context.siteUrl}][${context.dateString}] npx ts-node create-run-id.ts"
        jenkins.writeFile file: 'capabilities.json', text: context.seleniumCaps, encoding: 'utf-8'

        jenkins.parallel context.browsers.collectEntries { String browser ->
            [browser, { jenkins.stage("${browser}") {
                // split fixtures into small chunks to improve passing rate
                List<List<String>> fixtureChunks = jenkins.sh(returnStdout: true, script: 'npx ts-node ./scripts/ls-fixtures.ts').split('\n').collect{ it.trim() }.collate(context.chuckSize)
                String testLog = "${browser}-${context.dateString}".replaceAll(/[^a-zA-Z0-9 -]/, '-')
                for (List<String> fixtures in fixtureChunks) {
                    jenkins.withEnv([
                            "FIXTURES=${fixtures.join(',')}",
                            "BROWSERS=${browser}",
                            "TEST_LOG=${testLog}.log"
                    ]) {
                        jenkins.sh 'env' // for debug
                        jenkins.withCredentials([jenkins.usernamePassword(
                                credentialsId: context.platformCredentialId,
                                usernameVariable: 'RC_PLATFORM_APP_KEY',
                                passwordVariable: 'RC_PLATFORM_APP_SECRET')]) {
                            for (int i = 0; i < context.maxRetry; i++) {
                                int status = jenkins.sh(returnStatus: true, script: 'npm run e2e')
                                jenkins.echo "exit code is ${status}"
                                if (status in [0, 3])  // expected exits
                                    break
                                jenkins.sleep 60 // wait 1 min and execute again
                            }
                        }
                    }
                }
            }}]
        }
    }


    void run() {
        jenkins.node(nodeLabel) {
            String nodejsHome = jenkins.tool(context.nodeJsTool)
            String excludeTags = jenkins.sh(returnStdout: true, script: context.autoSkipCmd.trim()).trim()
            List<String> envVars = [
                    "TZ=UTF-8",
                    "PATH+NODEJS=${nodejsHome}:${nodejsHome}/bin",
                    "UV_THREADPOOL_SIZE=128",
                    'NODE_OPTIONS=--max_old_space_size=4096',

                    "LAST_COMMIT=${context.head}",
                    "GIT_SOURCE_BRANCH=${context.gitSourceBranch}",

                    "ENABLE_REMOTE_DASHBOARD=true",
                    "ENABLE_SSL=true",
                    "ENABLE_NOTIFICATION=true",
                    "SHUFFLE_FIXTURES=true",
                    "SITE_URL=${context.siteUrl}",
                    "SITE_ENV=${context.siteEnv}",
                    "SELENIUM_SERVER=${context.seleniumServer}",
                    "CONCURRENCY=${context.concurrency}",

                    "QUARANTINE_MODE=true",
                    "QUARANTINE_FAILED_THRESHOLD=3",
                    "QUARANTINE_PASSED_THRESHOLD=1",
                    "EXCLUDE_TAGS=${excludeTags}",

                    "SCREENSHOTS_PATH=./screenshots",
                    "TMPFILE_PATH=./tmp",
                    "SCREENSHOT_WEBP_QUALITY=80",
            ]

            if (context.influxdbUrl) {
                envVars.push('ENABLE_INFLUXDB=true')
                envVars.push("INFLUXDB_URL=${context.influxdbUrl}")
            }

            jenkins.withEnv(envVars) {
                jenkins.sh('env')
                jenkins.stage('checkout') { checkoutStage() }
                jenkins.dir('tests/e2e/testcafe') {
                    jenkins.stage('install dependencies') { installDependencyStage() }
                    e2eTasks()
                }
            }
        }
    }
}


// Initiate default context variable by user data
Context context = new Context(
        gitCredentialId: params.GIT_CREDENTIAL,
        gitSourceUrl: params.GIT_URL,
        gitSourceBranch: params.GIT_BRANCH,
        gitSourceNamespace: params.GIT_NAMESPACE,

        gitTargetUrl: params.GIT_TARGET_URL,
        gitTargetBranch: params.GIT_TARGET_BRANCH,
        gitTargetNamespace: params.GIT_TARGET_NAMESPACE,

        npmRegistry: params.NPM_REGISTRY,
        nodeJsTool: params.NODEJS_TOOL,

        siteUrl:  params.SITE_URL,
        siteEnv:  params.SITE_ENV,
        fixtures: params.FIXTURES,
        platformCredentialId: params.PLATFORM_CREDENTIAL,

        influxdbUrl: params.INFLUXDB_URL,
        autoSkipCmd: params.AUTO_SKIP_CMD,

        browsers: params.BROWSERS.split('\n').collect{ it.trim() },
        seleniumServer: params.SELENIUM_SERVER,
        seleniumCaps: params.SELENIUM_CAPS,

        jobTag: params.JOB_TAG,
        concurrency: params.CONCURRENCY as Integer,
        chuckSize: params.CHUNK_SIZE as Integer,
        maxRetry: params.MAX_RETRY as Integer,
        dateString: new Date().format("yyyy-MM-dd", TimeZone.getTimeZone("Asia/Shanghai")),
)

// Override default context variable if this job is triggered by gitlab events
context.gitSourceUrl = env.gitlabSourceRepoSshUrl ?: context.gitSourceUrl
context.gitSourceBranch = env.gitlabSourceBranch ?: context.gitSourceBranch
context.gitSourceNamespace = env.gitlabSourceNamespace ?: context.gitSourceNamespace

context.gitTargetUrl = env.gitlabTargetRepoSshUrl ?: context.gitTargetUrl ?: context.gitSourceUrl
context.gitTargetBranch = env.gitlabTargetBranch ?: context.gitTargetBranch ?: context.gitSourceBranch
context.gitTargetNamespace = env.gitlabTargetNamespace ?: context.gitTargetNamespace ?: context.gitSourceNamespace

// start execution
AutomationJob job = new AutomationJob(
        jenkins: this,
        context: context,
        nodeLabel: params.BUILD_NODE?: env.BUILD_NODE,
)
job.run()
