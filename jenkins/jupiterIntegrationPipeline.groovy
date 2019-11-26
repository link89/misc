import jenkins.model.*
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause
import java.net.URI

class Context {
    final static String SUCCESS_EMOJI = '✅ Success'
    final static String FAILURE_EMOJI = '❌ Failed'
    final static String ABORTED_EMOJI = '😱 Aborted'
    final static String UNKNOWN_EMOJI = '🤔 Unknown Error'

    final static String DOMAIN = 'fiji.gliprc.com'
    final static String RELEASE_BRANCH = 'master'
    final static String INTEGRATION_BRANCH = 'develop'

    static Boolean isStableBranch(String branch) {
        branch ==~ /^(develop)|(master)|(release.*)|(stage.*)|(hotfix.*)$/
    }

    static Boolean isIntegrationBranch(String branch) {
        branch == INTEGRATION_BRANCH
    }

    static Boolean isReleaseBranch(String branch) {
        branch == RELEASE_BRANCH
    }

    static String tagToGlipLink(String text) {
        text.replaceAll(/<a\b[^>]*?href="(.*?)"[^>]*?>(.*?)<\/a>/, '[$2]($1)')
    }

    static String tagToUrl(String text) {
        text.replaceAll(/<a\b[^>]*?href="(.*?)"[^>]*?>(.*?)<\/a>/, '$1')
    }

    static String urlToTag(String url) {
        """<a href="${url}">${url}</a>""".toString()
    }

    // jenkins
    String buildNode
    String e2eNode
    String buildUrl
    String buildNumber

    // gitlab
    String scmCredentialId
    String gitlabSourceBranch
    String gitlabTargetBranch
    String gitlabSourceNamespace
    String gitlabTargetNamespace
    String gitlabSourceRepoSshURL
    String gitlabTargetRepoSshURL
    String gitlabUserEmail
    String gitlabMergeRequestLastCommit
    String gitlabMergeRequestIid

    // nodejs
    String nodejsTool
    String npmRegistry

    // deployment
    URI deployUri
    String deployCredentialId
    String deployBaseDir

    URI lockUri
    String lockCredentialId

    // automation
    String rcCredentialId
    String e2eSiteEnv
    String e2eSeleniumServer
    String e2eBrowsers
    String e2eConcurrency
    String e2eExcludeTags
    String e2eCapabilities
    Boolean e2eEnableRemoteDashboard
    Boolean e2eEnableMockServer
    String feedbackUrl
    String autoExcludeCmd
    String influxdbUrl

    // runtime
    long timestamp = System.currentTimeMillis()
    String head
    String appHeadHash
    String juiHeadHash
    String rcuiHeadHash

    Boolean buildStatus = false
    String buildResult = UNKNOWN_EMOJI

    String buildDescription
    String saSummary
    String coverageSummary
    String coverageDiff
    String coverageDiffDetail
    String e2eReport
    String itReportUrl

    Boolean buildAppSuccess = false
    Boolean buildJuiSuccess = false
    Boolean buildRcuiSuccess = false

    Set<String> addresses = []
    Set<String> rcuiA11yReportAddresses = []
    List<String> failedStages = []

    Boolean getIsStableBranchUpdate() {
        !isMerge && isStableBranch(gitlabSourceBranch)
    }

    Boolean getIsMerge() {
        gitlabSourceBranch != gitlabTargetBranch
    }

    Boolean getIsSkipUnitTestAndStaticAnalysis() {
        // for a merge event, if target branch is not an stable branch, skip unit test
        // for a push event, skip if not an integration branch
        isMerge ? !isStableBranch(gitlabTargetBranch) : !isStableBranch(gitlabSourceBranch)
    }

    Boolean getIsSkipEndToEnd() {
        !isStableBranch(gitlabSourceBranch) && !isStableBranch(gitlabTargetBranch)
    }

    Boolean getIsSkipUpdateGitlabStatus() {
        !isMerge && !isIntegrationBranch(gitlabTargetBranch)
    }

    Boolean getIsBuildRelease() {
        isReleaseBranch(gitlabSourceBranch) ||
                isReleaseBranch(gitlabTargetBranch) ||
                gitlabSourceBranch ==~ /.*release.*/ ||
                gitlabTargetBranch ==~ /.*release.*/
    }

    Boolean getIsStageBuild() {
        !isMerge && gitlabSourceBranch.startsWith('stage')
    }

    String getSubDomain() {
        String subDomain = gitlabSourceBranch.replaceAll(/[\/\.]/, '-').toLowerCase()
        if (!isMerge) {
            if (isReleaseBranch(gitlabSourceBranch))
                return 'release'
            if (gitlabSourceBranch in ['develop', 'stage'])
                return gitlabSourceBranch
            return subDomain
        }
        return "mr-${subDomain}".toString()
    }

    String getMessageChannel() {
        if (isMerge)
            return "jupiter_mr_ci@company.glip.com"
        switch (gitlabSourceBranch) {
            case "master": return "jupiter_master_ci@company.glip.com"
            case "develop": return "jupiter_develop_ci@company.glip.com"
            default: return "jupiter_push_ci@company.glip.com"
        }
    }

    String getAppStageLinkDir() {
        "${deployBaseDir}/stage".toString()
    }

    String getAppLinkDir() {
        "${deployBaseDir}/${subDomain}".toString()
    }

    String getJuiLinkDir() {
        "${deployBaseDir}/${subDomain}-jui".toString()
    }

    String getRcuiLinkDir() {
        "${deployBaseDir}/${subDomain}-rcui".toString()
    }

    String getAppUrl() {
        "https://${subDomain}.${DOMAIN}".toString()
    }

    String getJuiUrl() {
        "https://${subDomain}-jui.${DOMAIN}".toString()
    }

    String getJuiHashUrl() {
        "https://${juiLockKey}.${DOMAIN}".toString()
    }

    String getRcuiUrl() {
        "https://${subDomain}-rcui.${DOMAIN}".toString()
    }

    String getRcuiHashUrl() {
        "https://${rcuiLockKey}.${DOMAIN}".toString()
    }

    String getAppHeadHashDir() {
        "${deployBaseDir}/${appLockKey}".toString()
    }

    String getJuiHeadHashDir() {
        "${deployBaseDir}/${juiLockKey}".toString()
    }

    String getRcuiHeadHashDir() {
        "${deployBaseDir}/${rcuiLockKey}".toString()
    }

    String getStaticAnalysisLockKey() {
        "staticanalysis-${appHeadHash}".toString()
    }

    String getUnitTestLockKey() {
        "unittest-${appHeadHash}".toString()
    }

    String getAppLockKey() {
        "app${isBuildRelease ? '-release' : ''}-${appHeadHash}".toString()
    }

    String getJuiLockKey() {
        "jui-${juiHeadHash}".toString()
    }

    String getRcuiLockKey() {
        "rcui-${rcuiHeadHash}".toString()
    }

    String getEndToEndTestLog() {
        "e2e-${appHeadHash}.log".toString()
    }

    String getEndToEndReportId() {
        "runId-${appHeadHash}".toString()
    }

    String getErrorMessage() {
        failedStages.join(', ')
    }

    String getGlipReport() {
        List<String> lines = []
        if (buildResult)
            lines.push("**Build Result**: ${buildResult}")
        if (errorMessage)
            lines.push("**Failed Stages**: ${errorMessage}")
        if (buildDescription)
            lines.push("**Description**: ${tagToGlipLink(buildDescription)}")
        if (buildUrl)
            lines.push("**Job**: ${buildUrl}flowGraphTable/")
        if (saSummary)
            lines.push("**Static Analysis**: ${saSummary}")
        if (coverageSummary)
            lines.push("**Coverage Report**: ${coverageSummary}")
        if (itReportUrl)
            lines.push("**IT Report**: ${itReportUrl}")
        if (coverageDiff) {
            lines.push("**Coverage Changes**: ${coverageDiff}")
        }
        if (coverageDiffDetail)
            lines.push("**Coverage Changes Detail**: ${coverageDiffDetail}")
        if (appUrl && buildAppSuccess)
            lines.push("**Application**: ${appUrl}")
        if (juiUrl && buildJuiSuccess)
            lines.push("**Storybook**: ${juiUrl}")
        if (rcuiUrl && buildRcuiSuccess)
            lines.push("**RCUI Storybook**: ${rcuiUrl}")
        if (e2eReport)
            lines.push("**E2E Report**: ${e2eReport}")
        if (feedbackUrl)
            lines.push("**Note**: Feel free to submit [form](${feedbackUrl}) if you have problems.".toString())
        lines.join(' \n')
    }

    String getJenkinsReport() {
        List<String> lines = []
        if (buildDescription)
            lines.push("Description: ${buildDescription}")
        if (saSummary)
            lines.push("Static Analysis: ${saSummary}")
        if (coverageDiff)
            lines.push("Coverage Changes: ${coverageDiff}")
        if (itReportUrl)
            lines.push("IT Report: ${urlToTag(itReportUrl)}")
        if (appUrl && buildAppSuccess)
            lines.push("Application: ${urlToTag(appUrl)}")
        if (juiUrl && buildJuiSuccess)
            lines.push("Storybook: ${urlToTag(juiUrl)}")
        if (rcuiUrl && buildRcuiSuccess)
            lines.push("RCUI Storybook: ${urlToTag(rcuiUrl)}")
        if (e2eReport)
            lines.push("E2E Report: ${urlToTag(e2eReport)}")
        lines.join('<br>')
    }
}

class BaseJob {
    def jenkins

    // abstract method
    void addFailedStage(String name) {}

    // jenkins utils
    @NonCPS
    void cancelOldBuildOfSameCause() {
        GitLabWebHookCause currentBuildCause = jenkins.currentBuild.rawBuild.getCause(GitLabWebHookCause.class)
        if (null == currentBuildCause)
            return
        def currentCauseData = currentBuildCause.getData()

        jenkins.currentBuild.rawBuild.getParent().getBuilds().each { build ->
            if (!build.isBuilding() || jenkins.currentBuild.rawBuild.getNumber() <= build.getNumber())
                return
            GitLabWebHookCause cause = build.getCause(GitLabWebHookCause.class)
            if (null == cause)
                return
            def causeData = cause.getData()

            if (currentCauseData.sourceBranch == causeData.sourceBranch
                    && currentCauseData.sourceRepoName == causeData.sourceRepoName
                    && currentCauseData.targetBranch == causeData.targetBranch
                    && currentCauseData.targetRepoName == causeData.targetRepoName) {
                jenkins.echo "build ${build.getFullDisplayName()} is terminating"
                build.setResult(Result.ABORTED)
                build.doStop()
                for (int i = 0; i < 10; i++) {
                    if (!build.isBuilding())
                        break
                    jenkins.sleep 10
                }
            }
            return
        }
    }

    def stage(Map args, Closure block) {
        assert args.name, 'stage name is required'
        String name = args.name
        int time = (null == args.timeout) ? 1200 : args.timeout
        Boolean activity = (null == args.activity) ? true : args.activity
        jenkins.timeout(time: time, activity: activity, unit: 'SECONDS') {
            try {
                jenkins.stage(name, block)
            } catch (e) {
                addFailedStage(name)
                jenkins.error "Failed on stage ${name}\n${e.dump()}"
            }
        }
    }

    void mail(addresses, String subject, String body) {
        addresses.each {
            try {
                jenkins.mail to: it, subject: subject, body: body
            } catch (e) {
                println e.dump()
            }
        }
    }

    // git utils
    String getStableHash(String treeish) {
        String cmd = "git cat-file commit ${treeish} | grep -e ^tree | cut -d ' ' -f 2".toString()
        jenkins.sh(returnStdout: true, script: cmd).trim()
    }

    // ssh utils
    String ssh(URI remoteUri, String cmd) {
        String sshCmd = "ssh -q -o StrictHostKeyChecking=no -p ${remoteUri.getPort() ?: 22} ${remoteUri.getUserInfo()}@${remoteUri.getHost()}".toString()
        jenkins.sh(returnStdout: true, script: "${sshCmd} \"${cmd.replaceAll('"', '\\\\"')}\"").trim()
    }

    void scp(String source, URI targetUri, String target) {
        String remoteTarget = "${targetUri.getUserInfo()}@${targetUri.getHost()}:${target}".toString()
        jenkins.sh "scp -o StrictHostKeyChecking=no -P ${targetUri.getPort()} ${source} ${remoteTarget}"
    }

    void deployToRemote(String sourceDir, URI targetUri, String targetDir) {
        deployToRemote(sourceDir, targetUri, targetDir, "tmp-${Math.random()}.tar.gz".toString())
    }

    void deployToRemote(String sourceDir, URI targetUri, String targetDir, String tarball) {
        jenkins.sh "tar -czvf ${tarball} -C ${sourceDir} ."  // pack
        ssh(targetUri, "rm -rf ${targetDir} || true && mkdir -p ${targetDir}".toString())  // clean target
        scp(tarball, targetUri, targetDir)
        ssh(targetUri, "tar -xzmvf ${targetDir}/${tarball} -C ${targetDir} && rm ${targetDir}/${tarball} && chmod -R 755 ${targetDir}".toString())
        // unpack
    }

    void copyRemoteDir(URI remoteUri, String sourceDir, String targetDir) {
        ssh(remoteUri, "mkdir -p ${targetDir} && cp -rf ${sourceDir}/* ${targetDir}/".toString())
    }

    void createGzFiles(URI remoteUri, String dir) {
        String gzipCmd =
                """find . -type f -size +150c \\( -name "*.wasm" -o -name "*.css" -o -name "*.html" -o -name "*.js" -o -name "*.json" -o -name "*.map" -o -name "*.svg"  -o -name "*.xml" \\) | xargs -I{} bash -c 'gzip -1 < {} > {}.gz'"""
        ssh(remoteUri, "cd ${dir} && ${gzipCmd}".toString())
    }

    void removeRemoteDir(URI remoteUri, String dir) {
        ssh(remoteUri, "rm -rf ${dir}".toString())
    }

    // ssh based distributed file lock
    void lockKey(String credentialId, URI lockUri, String key) {
        jenkins.sshagent(credentials: [credentialId]) {
            ssh(lockUri, "mkdir -p ${lockUri.getPath()} && touch ${lockUri.getPath()}/${key}".toString())
        }
    }

    Boolean hasBeenLocked(String credentialId, URI lockUri, String key) {
        jenkins.sshagent(credentials: [credentialId]) {
            return 'true' == ssh(lockUri, "[ -f ${lockUri.getPath()}/${key} ] && echo 'true' || echo 'false'".toString())
        }
    }

    void writeKeyFile(String credentialId, URI lockUri, String key, String filepath) {
        if (!jenkins.fileExists(filepath)) return

        jenkins.sshagent(credentials: [credentialId]) {
            ssh(lockUri, "mkdir -p ${lockUri.getPath()}".toString())
            scp(filepath, lockUri, "${lockUri.getPath()}/${key}".toString())
        }
    }

    void readKeyFile(String credentialId, URI lockUri, String key, String filepath) {
        jenkins.sshagent(credentials: [credentialId]) {
            String text = ssh(lockUri, "cat ${lockUri.getPath()}/${key} || true".toString()) ?: ''
            jenkins.writeFile file: filepath, text: text, encoding: 'utf-8'
        }
    }
}

class JupiterJob extends BaseJob {
    final static String DEPENDENCY_LOCK = 'dependency.lock'
    final static String E2E_DIRECTORY = 'tests/e2e/testcafe'

    Context context

    void addFailedStage(String name) {
        context.failedStages.add(name)
    }

    String getJobDescription() {
        context.tagToGlipLink(jenkins.currentBuild.getDescription())
    }

    void run() {
        try {
            doRun()
            context.buildStatus = true
            context.buildResult = context.SUCCESS_EMOJI
        } finally {
            context.buildDescription = jenkins.currentBuild.getDescription()
            jenkins.currentBuild.setDescription(context.jenkinsReport)
            if (context.buildStatus) {
                context.isSkipUpdateGitlabStatus || jenkins.updateGitlabCommitStatus(name: 'jenkins', state: 'success')
            } else {
                context.isSkipUpdateGitlabStatus || jenkins.updateGitlabCommitStatus(name: 'jenkins', state: 'failed')
                if ('ABORTED' == jenkins.currentBuild.getResult()) {
                    context.buildResult = context.ABORTED_EMOJI
                } else {
                    context.buildResult = context.FAILURE_EMOJI
                }
                jenkins.echo context.dump()
                jenkins.echo jenkins.currentBuild.dump()
            }
            if (context.gitlabMergeRequestIid) {
                jenkins.addGitLabMRComment(comment: context.glipReport.replaceAll('\n', '\n\n'))
            }
            mail(context.addresses, "Jenkins Build Result: ${context.buildResult}".toString(), context.glipReport)
        }
    }

    void doRun() {
        Boolean noEndToEnd = false

        cancelOldBuildOfSameCause()
        context.isSkipUpdateGitlabStatus || jenkins.updateGitlabCommitStatus(name: 'jenkins', state: 'pending')
        // using a high performance node to build
        jenkins.node(context.isSkipUnitTestAndStaticAnalysis && !context.isMerge ? context.e2eNode : context.buildNode) {
            context.isSkipUpdateGitlabStatus || jenkins.updateGitlabCommitStatus(name: 'jenkins', state: 'running')

            // install nodejs
            String nodejsHome = jenkins.tool context.nodejsTool
            jenkins.withEnv([
                    "PATH+NODEJS=${nodejsHome}/bin",
                    'TZ=UTC-8',
                    'CI=false',
                    'SENTRYCLI_CDNURL=https://cdn.npm.taobao.org/dist/sentry-cli',
                    'ELECTRON_MIRROR=https://npm.taobao.org/mirrors/electron/',
                    'NODE_OPTIONS=--max_old_space_size=4096',
            ]) {
                stage(name: 'Collect Facts') { collectFacts() }
                stage(name: 'Checkout', timeout: 1800) { checkout() }
                stage(name: 'Install Dependencies') { installDependencies() }
                jenkins.parallel(
                        'Unit Test': {
                            stage(name: 'Unit Test') { unitTest() }
                        },
                        'Integration Test': {
                            stage(name: 'Integration Test') { integrationTest() }
                        },
                        'tsc': {
                            stage(name: 'Static Analysis: tsc') { tscCheck() }
                        },
                        'eslint': {
                            stage(name: 'Static Analysis: eslint') { esLint() }
                        },
                )
                jenkins.parallel(
                        'Build Application': {
                            stage(name: 'Build Application') { buildApp() }
                        },
                        'Build JUI': {
                            stage(name: 'Build JUI') { buildJui() }
                        },
                        'Build RCUI': {
                            stage(name: 'Build RCUI') { buildRcui() }
                        },
                )
                if (isSkipEndToEnd) {
                    noEndToEnd = isSkipEndToEnd;
                } else {
                    stashEndToEnd()
                }
            }
        }

        if (noEndToEnd) return

        // using an average node to run e2e
        jenkins.node(context.e2eNode) {
            unstashEndToEnd()
            String nodejsHome = jenkins.tool context.nodejsTool
            jenkins.withEnv([
                    "PATH+NODEJS=${nodejsHome}/bin",
            ]) {
                stage(name: 'E2E Automation') { e2eAutomation() }
            }
        }
    }

    void stashEndToEnd() {
        String tarball = "testcafe-${context.head}.tar.gz".toString()
        jenkins.dir(E2E_DIRECTORY) {
            jenkins.sh 'git clean -xdf'
        }
        jenkins.sh "tar -czvf ${tarball} ${E2E_DIRECTORY}"
        jenkins.stash name: tarball, includes: tarball
    }

    void unstashEndToEnd() {
        String tarball = "testcafe-${context.head}.tar.gz".toString()
        jenkins.sh "find ${E2E_DIRECTORY} -mindepth 1 -maxdepth 1 -not -name node_modules | xargs rm -rf"
        jenkins.unstash name: tarball
        jenkins.sh "tar -xmzvf ${tarball}"
    }

    void collectFacts() {
        // test commands
        jenkins.sh 'env'
        jenkins.sh 'uptime'
        jenkins.sh 'df -h'
        jenkins.sh 'node -v'
        jenkins.sh 'tar --version'
        jenkins.sh 'git --version'
        jenkins.sh 'rsync --version'
        jenkins.sh 'grep --version'
        jenkins.sh 'which tr'
        jenkins.sh 'which xargs'
        jenkins.sh "npm config set registry ${context.npmRegistry}"

        // install xvfb on centos if it is not exists
        jenkins.sh 'which xvfb-run || yum install gtk3-devel libXScrnSaver xorg-x11-server-Xvfb alsa-lib -y || true'
        // install diff2html-cli if it is not exists
        jenkins.sh('npm install -g @nullcc/diff2html-cli || true')

        // clean npm cache when its size exceed 6G, the unit of default du command is K, so we need to >> 20 to get G
        long npmCacheSize = Long.valueOf(jenkins.sh(returnStdout: true, script: 'du -s $(npm config get cache) | cut -f1 || true').trim() ?: 0) >> 20
        if (npmCacheSize > 6) {
            jenkins.sh 'npm cache clean --force'
        }
    }

    void checkout() {
        // keep node_modules to speed up build process
        // keep a lock file to help us decide if we need to upgrade dependencies
        // jenkins.sh "git clean -xdf -e node_modules -e ${DEPENDENCY_LOCK} || true"
        jenkins.sh "git checkout -f || true"
        jenkins.sh "git clean -xdf || true"
        jenkins.checkout([
                $class           : 'GitSCM',
                branches         : [[name: "${context.gitlabSourceNamespace}/${context.gitlabSourceBranch}"]],
                extensions       : [
                        [$class: 'PruneStaleBranch'],
                        [
                                $class : 'PreBuildMerge',
                                options: [
                                        fastForwardMode: 'FF',
                                        mergeRemote    : context.gitlabTargetNamespace,
                                        mergeTarget    : context.gitlabTargetBranch,
                                ]
                        ]
                ],
                userRemoteConfigs: [
                        [
                                credentialsId: context.scmCredentialId,
                                name         : context.gitlabTargetNamespace,
                                url          : context.gitlabTargetRepoSshURL,
                        ],
                        [
                                credentialsId: context.scmCredentialId,
                                name         : context.gitlabSourceNamespace,
                                url          : context.gitlabSourceRepoSshURL,
                        ]
                ]
        ])

        // update runtime context
        // get head
        context.head = jenkins.sh(returnStdout: true, script: 'git rev-parse HEAD').trim()

        // change in tests and autoDevOps directory should not trigger application build
        // for git 1.9, there is an easy way to exclude files
        // but most slaves are centos, whose git's version is still 1.8, we use a cmd pipeline here for compatibility
        // the reason to use stableHash is if HEAD is generate via fast-forward,
        // the commit will be changed when re-running the job due to timestamp changed
        context.appHeadHash = getStableHash(
                jenkins.sh(returnStdout: true, script: '''ls -1 | grep -Ev '^(tests|autoDevOps)$' | tr '\\n' ' ' | xargs git rev-list -1 HEAD -- ''').trim()
        )
        context.juiHeadHash = getStableHash(
                jenkins.sh(returnStdout: true, script: '''git rev-list -1 HEAD -- packages/jui''').trim()
        )
        context.rcuiHeadHash = getStableHash(
                jenkins.sh(returnStdout: true, script: '''git rev-list -1 HEAD -- packages/rcui''').trim()
        )
        assert context.head && context.appHeadHash && context.juiHeadHash && context.rcuiHeadHash

        // update email address
        if (context.gitlabUserEmail)
            context.addresses.add(context.gitlabUserEmail)

        String getAuthorsCmd =
                "git rev-list '${context.gitlabTargetNamespace}/${context.gitlabTargetBranch}'..'${context.gitlabSourceNamespace}/${context.gitlabSourceBranch}' | xargs git show -s --format='%ae' | sort | uniq | grep -E -o '\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}\\b' || true".toString()
        List<String> authors = jenkins.sh(returnStdout: true, script: getAuthorsCmd).trim().split('\n')
        List<String> glipAddresses = authors.collect { it.replaceAll('company.com', 'company.glip.com') }
        context.addresses.addAll(glipAddresses)
        context.rcuiA11yReportAddresses.addAll(glipAddresses)
    }

    void installDependencies() {
        if (isSkipInstallDependency) return
        String dependencyLock = jenkins.sh(returnStdout: true, script: '''git ls-files | grep -e package.json -e package-lock.json | grep -v tests | tr '\\n' ' ' | xargs git rev-list -1 HEAD -- | xargs git cat-file commit | grep -e ^tree | cut -d ' ' -f 2 ''').trim()
        if (jenkins.fileExists(DEPENDENCY_LOCK) && jenkins.readFile(file: DEPENDENCY_LOCK, encoding: 'utf-8').trim() == dependencyLock) {
            jenkins.echo "${DEPENDENCY_LOCK} doesn't change, no need to update: ${dependencyLock}"
            // return
        }
        jenkins.sh "npm config set registry ${context.npmRegistry}"
        jenkins.sh "npm config set grpc_node_binary_host_mirror https://npm.taobao.org/mirrors/"
        jenkins.sh 'npm run fixed:version pre || true'  // suppress error
        jenkins.sshagent(credentials: [context.scmCredentialId]) {
            jenkins.sh 'npm install --unsafe-perm'
        }
        jenkins.writeFile(file: DEPENDENCY_LOCK, text: dependencyLock, encoding: 'utf-8')
        jenkins.sh 'npm run fixed:version check || true'  // suppress error
        jenkins.sh 'npm run fixed:version cache || true'  // suppress error
    }

    void tscCheck() {
        if (isSkipStaticAnalysis) return
        jenkins.sh 'npm run tsc'
    }

    void esLint() {
        if (isSkipStaticAnalysis) return
        jenkins.sh 'npm run lint-all'
    }

    void integrationTest() {
        if (isSkipIT) return
        String testCmd = "npm run test it -- -w 12"
        try {
            if (jenkins.sh(returnStatus: true, script: 'which xvfb-run') > 0) {
                jenkins.sh testCmd
            } else {
                jenkins.sh "xvfb-run -d -s '-screen 0 1920x1080x24' ${testCmd}"
            }
        } finally {
            if (jenkins.fileExists('itReport/jest_html_reporters.html')) {
                jenkins.publishHTML([
                        reportDir   : 'itReport', reportFiles: 'jest_html_reporters.html', reportName: 'IntegrationTest', reportTitles: 'IntegrationTest',
                        allowMissing: false, alwaysLinkToLastBuild: false, keepAll: true,
                ])
                context.itReportUrl = "${context.buildUrl}IntegrationTest".toString()
            }
        }
    }

    void unitTest() {
        if (isSkipUnitTest) return
        jenkins.sh 'npm run snapshot'
        String testCmd = "npm run test -- --coverage --coverageReporters json lcov text-summary -w 12"
        // if (context.isMerge) {
        //    testCmd = "${testCmd} --changedSince=${context.gitlabTargetNamespace}/${context.gitlabTargetBranch}"
        // }
        try {
            if (jenkins.sh(returnStatus: true, script: 'which xvfb-run') > 0) {
                jenkins.sh testCmd
            } else {
                jenkins.sh "xvfb-run -d -s '-screen 0 1920x1080x24' ${testCmd}"
            }
        } finally {
            // publish lcov-report
            String reportName = 'Coverage'
            jenkins.publishHTML([
                    reportDir   : 'coverage/lcov-report', reportFiles: 'index.html', reportName: reportName, reportTitles: reportName,
                    allowMissing: false, alwaysLinkToLastBuild: false, keepAll: true,
            ])
            context.coverageSummary = "${context.buildUrl}${reportName}".toString()
        }

        // for merge request, we should check if
        if (context.isMerge) {
            String coverageData = './coverage/coverage-final.json'
            String diffFileName = './git-minimal.diff'
            String diffScript = './scripts/git-diff-coverage.js'
            String diffCovResult = './diff-cov-result.txt'
            String diffCovReport = './diff-coverage/index.html'
            String diffCovReportDir = './diff-coverage'
            String diffCovReportName = 'DiffCoverage'

            // get diff file
            jenkins.sh "git diff --unified=0 --no-renames -G. --minimal ${context.gitlabTargetNamespace}/${context.gitlabTargetBranch} > ${diffFileName}"

            // publish diff coverage report using diff2html-cli
            jenkins.sh "mkdir -p ${diffCovReportDir}"
            jenkins.sh "cat ${diffFileName} | diff2html -c ${coverageData} -i stdin -F ${diffCovReport}"
            jenkins.publishHTML([
                    reportDir   : diffCovReportDir, reportFiles: 'index.html', reportName: diffCovReportName, reportTitles: diffCovReportName,
                    allowMissing: false, alwaysLinkToLastBuild: false, keepAll: true,
            ])
            context.coverageDiffDetail = "${context.buildUrl}${diffCovReportName}".toString()

            // test coverage rate
            if (jenkins.fileExists(diffScript)) {
                try {
                    jenkins.sh "node ${diffScript} < ${diffFileName} > ${diffCovResult}"
                } finally {
                    context.coverageDiff = jenkins.sh(returnStdout: true, script: "cat ${diffCovResult} || echo 'result is not found'").trim()
                }
            }
        }
        lockKey(context.lockCredentialId, context.lockUri, context.unitTestLockKey)
    }

    void buildApp() {
        if (!isSkipBuildApp) {
            // FIXME: move this part to build script
            jenkins.sh 'npx ts-node application/src/containers/VersionInfo/GitRepo.ts'
            jenkins.sh 'mv commitInfo.ts application/src/containers/VersionInfo/'
            jenkins.sh "sed 's/{{buildNumber}}/${context.buildNumber}/;s/{{buildCommit}}/${context.head.substring(0, 9)}/;s/{{buildTime}}/${context.timestamp}/' application/src/containers/VersionInfo/versionInfo.json > versionInfo.json || true"
            jenkins.sh 'mv versionInfo.json application/src/containers/VersionInfo/versionInfo.json || true'
            if (context.isBuildRelease) {
                jenkins.sh 'npm run build:release'
            } else {
                jenkins.dir('application') {
                    jenkins.sh 'npm run build'
                }
            }
            String sourceDir = 'application/build'
            // The reason we add this check is sometimes the build package is incomplete
            // The root cause maybe the improperly error handling in build script (maybe always exit with 0)
            if (!jenkins.fileExists("${sourceDir}/index.html"))
                jenkins.error "Build application is incomplete!"

            jenkins.sshagent(credentials: [context.deployCredentialId]) {
                removeRemoteDir(context.deployUri, context.appHeadHashDir)
                deployToRemote(sourceDir, context.deployUri, context.appHeadHashDir)
            }
            lockKey(context.lockCredentialId, context.lockUri, context.appLockKey)
        }
        // deploy to a user friendly domain directory
        jenkins.sshagent(credentials: [context.deployCredentialId]) {
            // create copy to branch name based folder
            removeRemoteDir(context.deployUri, context.appLinkDir)
            copyRemoteDir(context.deployUri, context.appHeadHashDir, context.appLinkDir)
            // create gzip file
            createGzFiles(context.deployUri, context.appLinkDir)

            // for stage build, also create link to stage folder
            if (context.isStageBuild) {
                removeRemoteDir(context.deployUri, context.appStageLinkDir)
                copyRemoteDir(context.deployUri, context.appLinkDir, context.appStageLinkDir)
            }
        }
        context.buildAppSuccess = true
    }

    void buildJui() {
        if (!isSkipBuildJui) {
            jenkins.sh 'npm run build:ui'
            String sourceDir = "packages/jui/storybook-static"
            jenkins.sshagent(credentials: [context.deployCredentialId]) {
                deployToRemote(sourceDir, context.deployUri, context.juiHeadHashDir)
            }
            // juiAutomation()
            lockKey(context.lockCredentialId, context.lockUri, context.juiLockKey)
        }
        jenkins.sshagent(credentials: [context.deployCredentialId]) {
            copyRemoteDir(context.deployUri, context.juiHeadHashDir, context.juiLinkDir)
        }
        context.buildJuiSuccess = true
    }

    void juiAutomation() {
        jenkins.dir('packages/jui') {
            jenkins.withEnv([
                    "JUI_URL=${context.juiHashUrl}",
            ]) {
                try {
                    jenkins.sh 'npm run test || true'
                } finally {
                    String tarball = "jui-snapshots-diff-${context.head}.tar.gz".toString()
                    String snapshotDir = 'src/__tests__/snapshot/__image_snapshots__/__diff_output__'
                    jenkins.sh "tar -czvf ${tarball} -C ${snapshotDir} || true"
                    if (jenkins.fileExists(tarball))
                        jenkins.archiveArtifacts artifacts: tarball, fingerprint: true
                }
            }
        }
    }

    void buildRcui() {
        if (!isSkipBuildRcui) {
            jenkins.dir('packages/rcui') {
                jenkins.sh 'npm run build:storybook'
            }
            String sourceDir = "packages/rcui/public"
            jenkins.sshagent(credentials: [context.deployCredentialId]) {
                deployToRemote(sourceDir, context.deployUri, context.rcuiHeadHashDir)
            }
            try {
                jenkins.echo "skip rcui at"
                // rcuiAccessibilityAutomation()
            } catch (e) {
            }
            lockKey(context.lockCredentialId, context.lockUri, context.rcuiLockKey)
        }
        jenkins.sshagent(credentials: [context.deployCredentialId]) {
            copyRemoteDir(context.deployUri, context.rcuiHeadHashDir, context.rcuiLinkDir)
        }
        context.buildRcuiSuccess = true
    }

    void rcuiAccessibilityAutomation() {
        jenkins.dir('packages/rcui/tests/testcafe') {
            String rcuiResultDir = 'rcui-result'
            jenkins.withEnv([
                    "TEST_URL=${context.rcuiHashUrl}",
                    "FILE_PATH=${rcuiResultDir}",
                    "SELENIUM_SERVER=${context.e2eSeleniumServer}",
            ]) {
                jenkins.sh 'env'
                jenkins.sh "npm config set registry ${context.npmRegistry}"
                jenkins.sh 'npm install --only=dev'
                jenkins.sh 'npm run test'
                String reportName = 'RCUI-Accessibility'

                jenkins.publishHTML([
                        reportDir   : "${rcuiResultDir}/html", reportFiles: 'accessibility-check.html', reportName: reportName, reportTitles: reportName,
                        allowMissing: false, alwaysLinkToLastBuild: false, keepAll: true,
                ])
                String rcuiReportUrl = "${context.buildUrl}${reportName}".toString()
                mail(context.rcuiA11yReportAddresses, "rcui accessibility automation result", createRcuiReport(rcuiResultDir, rcuiReportUrl))
            }
        }
    }

    String createRcuiReport(String rcuiResultDir, String rcuiReportUrl) {
        jenkins.dir(rcuiResultDir) {
            String passedComponents = jenkins.readFile(file: 'allPass.txt', encoding: 'utf-8').trim()
            String failedComponents = jenkins.readFile(file: 'noAllPass.txt', encoding: 'utf-8').trim()
            if (passedComponents) {
                passedComponents = passedComponents.split('\n').collect {
                    "${context.SUCCESS_EMOJI} ${it}".toString()
                }.join('\n')
            }
            if (failedComponents) {
                failedComponents = failedComponents.split('\n').collect {
                    "${context.FAILURE_EMOJI} ${it}".toString()
                }.join('\n')
            }
            return [
                    "[**RCUI Accessibility Report**](${rcuiReportUrl})".toString(),
                    "**Build Summary:** ${jobDescription}".toString(),
                    "**Build URL:** [here](${context.buildUrl})".toString(),
                    "**RCUI Storybook:** [here](${context.rcuiHashUrl})".toString(),
                    '**Components:**',
                    passedComponents,
                    failedComponents,
            ].join('\n')
        }
    }

    void e2eAutomation() {
        String hostname = jenkins.sh(returnStdout: true, script: 'hostname -f').trim()
        String startTime = jenkins.sh(returnStdout: true, script: "TZ=UTC-8 date +'%F %T'").trim()

        if (context.autoExcludeCmd) {
            context.e2eExcludeTags += ',' + jenkins.sh(returnStdout: true, script: context.autoExcludeCmd.trim()).trim()
        }

        List<String> envVars = [
                "HOST_NAME=${hostname}",
                "SITE_URL=${context.appUrl}",
                "SITE_ENV=${context.e2eSiteEnv}",
                "SELENIUM_SERVER=${context.e2eSeleniumServer}",
                "ENABLE_REMOTE_DASHBOARD=${context.e2eEnableRemoteDashboard}",
                "ENABLE_MOCK_SERVER=${context.e2eEnableMockServer}",
                "BROWSERS=${context.e2eBrowsers}",
                "CONCURRENCY=${context.e2eConcurrency}",
                "EXCLUDE_TAGS=${context.e2eExcludeTags}",
                "BRANCH=${context.gitlabSourceBranch}",
                "TESTS_LOG=${context.endToEndTestLog}",
                "ACTION=ON_MERGE",
                "SCREENSHOTS_PATH=./screenshots",
                "TMPFILE_PATH=./tmp",
                "DEBUG_MODE=false",
                "STOP_ON_FIRST_FAIL=true",
                "SKIP_JS_ERROR=true",
                "SKIP_CONSOLE_ERROR=true",
                "SKIP_CONSOLE_WARN=true",
                "SCREENSHOT_WEBP_QUALITY=80",
                "QUARANTINE_MODE=true",
                "QUARANTINE_FAILED_THRESHOLD=3",
                "QUARANTINE_PASSED_THRESHOLD=1",
                "DEBUG=axios",
                "ENABLE_SSL=true",
                "ENABLE_NOTIFICATION=true",
                "RUN_NAME=[Jupiter][Pipeline][Merge][${startTime}][${context.gitlabSourceBranch}][${context.head}]",
        ]

        if (context.influxdbUrl && context.isStableBranchUpdate) {
            envVars.push('ENABLE_INFLUXDB=true')
            envVars.push("INFLUXDB_URL=${context.influxdbUrl}")
        }

        jenkins.withEnv(envVars) {
            jenkins.dir(E2E_DIRECTORY) {
                jenkins.sh 'env'  // for debug
                jenkins.writeFile file: 'capabilities.json', text: context.e2eCapabilities, encoding: 'utf-8'
                jenkins.sh "mkdir -p screenshots tmp"
                jenkins.sh "npm config set registry ${context.npmRegistry}"
                jenkins.sshagent(credentials: [context.scmCredentialId]) {
                    jenkins.sh 'npm install minipass@2.7.0'
                    jenkins.sh 'npm install --unsafe-perm'
                }

                if (context.e2eEnableRemoteDashboard) {
                    readKeyFile(context.lockCredentialId, context.lockUri, context.endToEndReportId, 'runId')
                    jenkins.sh 'npx ts-node create-run-id.ts'
                    writeKeyFile(context.lockCredentialId, context.lockUri, context.endToEndReportId, 'runId')
                    context.e2eReport = jenkins.sh(returnStdout: true, script: 'cat reportUrl || true').trim()
                }

                jenkins.withCredentials([jenkins.usernamePassword(
                        credentialsId: context.rcCredentialId,
                        usernameVariable: 'RC_PLATFORM_APP_KEY',
                        passwordVariable: 'RC_PLATFORM_APP_SECRET')]) {
                    try {
                        readKeyFile(context.lockCredentialId, context.lockUri, context.endToEndTestLog, context.endToEndTestLog)
                        jenkins.sh "npm run e2e"
                    } finally {
                        writeKeyFile(context.lockCredentialId, context.lockUri, context.endToEndTestLog, context.endToEndTestLog)
                        if (!context.e2eEnableRemoteDashboard) {
                            jenkins.sh "tar -czvf allure.tar.gz -C ./allure/allure-results . || true"
                            jenkins.archiveArtifacts artifacts: 'allure.tar.gz', fingerprint: true
                        }
                    }
                }
            }
        }
    }

    Boolean getIsSkipInstallDependency() {
        isSkipUnitTest && isSkipStaticAnalysis && isSkipBuildApp && isSkipBuildJui && isSkipBuildRcui
    }

    Boolean getIsSkipUnitTest() {
        context.isSkipUnitTestAndStaticAnalysis || (context.isMerge && hasBeenLocked(context.deployCredentialId, context.lockUri, context.unitTestLockKey))
    }

    Boolean getIsSkipIT() {
        context.isSkipUnitTestAndStaticAnalysis
    }

    Boolean getIsSkipStaticAnalysis() {
        context.isSkipUnitTestAndStaticAnalysis || hasBeenLocked(context.deployCredentialId, context.lockUri, context.staticAnalysisLockKey)
    }

    Boolean getIsSkipBuildApp() {
        hasBeenLocked(context.deployCredentialId, context.lockUri, context.appLockKey)
    }

    Boolean getIsSkipBuildJui() {
        hasBeenLocked(context.deployCredentialId, context.lockUri, context.juiLockKey)
    }

    Boolean getIsSkipBuildRcui() {
        hasBeenLocked(context.deployCredentialId, context.lockUri, context.rcuiLockKey)
    }

    Boolean getIsSkipEndToEnd() {
        context.isSkipEndToEnd
    }
}

// Get started!
Context context = new Context(
        buildNode: params.BUILD_NODE ?: env.BUILD_NODE,
        e2eNode: params.E2E_NODE ?: env.E2E_NODE,
        buildUrl: env.BUILD_URL,
        buildNumber: env.BUILD_NUMBER,

        nodejsTool: params.NODEJS_TOOL,
        npmRegistry: params.NPM_REGISTRY,

        scmCredentialId: params.SCM_CREDENTIAL,
        gitlabSourceBranch: env.gitlabSourceBranch,
        gitlabTargetBranch: env.gitlabTargetBranch,
        gitlabSourceNamespace: env.gitlabSourceNamespace,
        gitlabTargetNamespace: env.gitlabTargetNamespace,
        gitlabSourceRepoSshURL: env.gitlabSourceRepoSshURL,
        gitlabTargetRepoSshURL: env.gitlabTargetRepoSshURL,
        gitlabUserEmail: env.gitlabUserEmail,
        gitlabMergeRequestLastCommit: env.gitlabMergeRequestLastCommit,
        gitlabMergeRequestIid: env.gitlabMergeRequestIid,

        deployCredentialId: params.DEPLOY_CREDENTIAL,
        deployUri: new URI(params.DEPLOY_URI),
        deployBaseDir: params.DEPLOY_BASE_DIR,
        lockUri: new URI(params.LOCK_URI),
        lockCredentialId: params.LOCK_CREDENTIAL,

        rcCredentialId: params.E2E_RC_CREDENTIAL,
        e2eSiteEnv: params.E2E_SITE_ENV,
        e2eSeleniumServer: params.E2E_SELENIUM_SERVER,
        e2eBrowsers: params.E2E_BROWSERS,
        e2eConcurrency: params.E2E_CONCURRENCY,
        e2eExcludeTags: params.E2E_EXCLUDE_TAGS ?: '',
        e2eEnableRemoteDashboard: params.E2E_ENABLE_REMOTE_DASHBOARD,
        e2eEnableMockServer: params.E2E_ENABLE_MOCK_SERVER,
        e2eCapabilities: params.E2E_CAPABILITIES,
        feedbackUrl: params.FEEDBACK_URL,
        autoExcludeCmd: params.AUTO_EXCLUDE_CMD,
        influxdbUrl: params.INFLUXDB_URL,
)

context.rcuiA11yReportAddresses.addAll(params.RCUI_A11Y_REPORT_ADDRESSES.split('\n'))
context.gitlabSourceBranch = context.gitlabSourceBranch ?: params.GITLAB_BRANCH
context.gitlabTargetBranch = context.gitlabTargetBranch ?: context.gitlabSourceBranch
context.gitlabSourceNamespace = context.gitlabSourceNamespace ?: params.GITLAB_NAMESPACE
context.gitlabTargetNamespace = context.gitlabTargetNamespace ?: context.gitlabSourceNamespace
context.gitlabSourceRepoSshURL = context.gitlabSourceRepoSshURL ?: params.GITLAB_SSH_URL
context.gitlabTargetRepoSshURL = context.gitlabTargetRepoSshURL ?: context.gitlabSourceRepoSshURL
context.addresses.add(context.messageChannel)

JupiterJob job = new JupiterJob(jenkins: this, context: context)
job.run()
