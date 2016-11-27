package com.gorylenko

import org.ajoberstar.grgit.Grgit
import org.apache.http.client.utils.URIBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileTree
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import java.text.SimpleDateFormat
import java.util.regex.Pattern

/**
 * @link <a href="http://www.insaneprogramming.be/blog/2014/08/15/spring-boot-info-git/">Spring Boot's info endpoint, Git and Gradle - InsaneProgramming</a>
 */
class GitPropertiesPlugin implements Plugin<Project> {

    private static final String EXTENSION_NAME = "gitProperties"
    private static final String TASK_NAME = "generateGitProperties"

    private static final String GIT_PROPERTIES_FILENAME = "git.properties"
    private static final String DEFAULT_OUTPUT_DIR = "resources/main"

    private static final String CHARSET = "UTF-8"

    private static final String KEY_GIT_BRANCH = "git.branch"
    private static final String KEY_GIT_REMOTE_URL = "git.remote.url"
    private static final String KEY_GIT_COMMIT_ID = "git.commit.id"
    private static final String KEY_GIT_COMMIT_ID_ABBREVIATED = "git.commit.id.abbrev"
    private static final String KEY_GIT_COMMIT_USER_NAME = "git.commit.user.name"
    private static final String KEY_GIT_COMMIT_USER_EMAIL = "git.commit.user.email"
    private static final String KEY_GIT_COMMIT_SHORT_MESSAGE = "git.commit.message.short"
    private static final String KEY_GIT_COMMIT_FULL_MESSAGE = "git.commit.message.full"
    private static final String KEY_GIT_COMMIT_TIME = "git.commit.time"
    private static final String[] KEY_ALL = [
            KEY_GIT_BRANCH, KEY_GIT_REMOTE_URL,
            KEY_GIT_COMMIT_ID, KEY_GIT_COMMIT_ID_ABBREVIATED,
            KEY_GIT_COMMIT_USER_NAME, KEY_GIT_COMMIT_USER_EMAIL,
            KEY_GIT_COMMIT_SHORT_MESSAGE, KEY_GIT_COMMIT_FULL_MESSAGE,
            KEY_GIT_COMMIT_TIME
    ]

    @Override
    void apply(Project project) {

        project.extensions.create(EXTENSION_NAME, GitPropertiesPluginExtension)
        def task = project.tasks.create(TASK_NAME, GenerateGitPropertiesTask)

        task.setGroup(BasePlugin.BUILD_GROUP)
        ensureTaskRunsOnJavaClassesTask(project, task)
    }

    private static void ensureTaskRunsOnJavaClassesTask(Project project, Task task) {
        project.plugins.apply JavaPlugin
        project.getTasks().getByName(JavaPlugin.CLASSES_TASK_NAME).dependsOn(task)
    }

    /**
     * The method below is from https://github.com/ktoso/maven-git-commit-id-plugin
     */
    /**
     * Regex to check for SCP-style SSH+GIT connection strings such as 'git@github.com'
     */
    static final Pattern GIT_SCP_FORMAT = Pattern.compile("^([a-zA-Z0-9_.+-])+@(.*)");
    /**
     * If the git remote value is a URI and contains a user info component, strip the password from it if it exists.
     *
     * @param gitRemoteString The value of the git remote
     * @return
     */
    private static String stripCredentialsFromOriginUrl(String gitRemoteString) {

        // The URL might be null if the repo hasn't set a remote
        if (gitRemoteString == null) {
            return gitRemoteString;
        }

        // Remotes using ssh connection strings in the 'git@github' format aren't
        // proper URIs and won't parse . Plus since you should be using SSH keys,
        // credentials like are not in the URL.
        if (GIT_SCP_FORMAT.matcher(gitRemoteString).matches()) {
            return gitRemoteString;
        }
        // At this point, we should have a properly formatted URL
        try {
            URI original = new URI(gitRemoteString);
            String userInfoString = original.getUserInfo();
            if (null == userInfoString) {
                return gitRemoteString;
            }
            URIBuilder b = new URIBuilder(gitRemoteString);
            String[] userInfo = userInfoString.split(":");
            // Build a new URL from the original URL, but nulling out the password
            // component of the userinfo. We keep the username so that ssh uris such
            // ssh://git@github.com will retain 'git@'.
            b.setUserInfo(userInfo[0]);
            return b.build().toString();

        } catch (URISyntaxException e) {
            return null
        }
    }

    static class GenerateGitPropertiesTask extends DefaultTask {

        @InputFiles
        public FileTree getSource() {
            return project.files(new File(project.gitProperties.gitRepositoryRoot ?: project.rootProject.file('.'), ".git")).getAsFileTree()
        }

        @OutputFile
        public File getOutput() {
            def dir = project.gitProperties.gitPropertiesDir ?: new File(project.buildDir, DEFAULT_OUTPUT_DIR)
            return new File(dir, GIT_PROPERTIES_FILENAME)
        }

        @TaskAction
        void generate() {
            def repo = Grgit.open(dir: project.gitProperties.gitRepositoryRoot ?: project.rootProject.file('.'))
            def dir = project.gitProperties.gitPropertiesDir ?: new File(project.buildDir, DEFAULT_OUTPUT_DIR)
            def file = new File(dir, GIT_PROPERTIES_FILENAME)
            def keys = project.gitProperties.keys ?: KEY_ALL
            if (!dir.exists()) {
                dir.mkdirs()
            }
            if (file.exists()) {
                assert file.delete()
            }
            assert file.createNewFile()
            logger.info "writing to [${file}]"
            def map = [(KEY_GIT_BRANCH)                 : repo.branch.current.name
                       , (KEY_GIT_COMMIT_ID)            : repo.head().id
                       , (KEY_GIT_REMOTE_URL)           : stripCredentialsFromOriginUrl(repo.remote.list().first().url)
                       , (KEY_GIT_COMMIT_ID_ABBREVIATED): repo.head().abbreviatedId
                       , (KEY_GIT_COMMIT_USER_NAME)     : repo.head().author.name
                       , (KEY_GIT_COMMIT_USER_EMAIL)    : repo.head().author.email
                       , (KEY_GIT_COMMIT_SHORT_MESSAGE) : repo.head().shortMessage
                       , (KEY_GIT_COMMIT_FULL_MESSAGE)  : repo.head().fullMessage
                       , (KEY_GIT_COMMIT_TIME)          : formatDate(repo.head().time, project.gitProperties.dateFormat, project.gitProperties.dateFormatTimeZone)]

            file.withWriter(CHARSET) { w ->
                map.subMap(keys).each { key, value ->
                    w.writeLine "$key=$value"
                }
            }
        }

        private String formatDate(long timestamp, String dateFormat, String timezone) {
            String date
            if (dateFormat) {
                def sdf = new SimpleDateFormat(dateFormat)
                if (timezone) {
                    sdf.setTimeZone(TimeZone.getTimeZone(timezone))
                }
                date = sdf.format(new Date(timestamp * 1000L))
            } else {
                date = timestamp.toString()
            }
        }
    }
}

class GitPropertiesPluginExtension {
    File gitPropertiesDir
    File gitRepositoryRoot
    List keys
    String dateFormat
    String dateFormatTimeZone
}
