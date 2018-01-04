package com.gorylenko

import java.text.SimpleDateFormat

import org.ajoberstar.grgit.Grgit
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

class GitPropertiesPlugin implements Plugin<Project> {
    
    private static final String EXTENSION_NAME = "gitProperties"
    private static final String TASK_NAME = "generateGitProperties"

    private static final String GIT_PROPERTIES_FILENAME = "git.properties"
    private static final String DEFAULT_OUTPUT_DIR = "resources/main"

    private static final String CHARSET = "ISO-8859-1"

    private static final String KEY_GIT_BRANCH = "git.branch"
    private static final String KEY_GIT_COMMIT_ID = "git.commit.id"
    private static final String KEY_GIT_COMMIT_ID_ABBREVIATED = "git.commit.id.abbrev"
    private static final String KEY_GIT_COMMIT_USER_NAME = "git.commit.user.name"
    private static final String KEY_GIT_COMMIT_USER_EMAIL = "git.commit.user.email"
    private static final String KEY_GIT_COMMIT_SHORT_MESSAGE = "git.commit.message.short"
    private static final String KEY_GIT_COMMIT_FULL_MESSAGE = "git.commit.message.full"
    private static final String KEY_GIT_COMMIT_TIME = "git.commit.time"
    private static final String KEY_GIT_COMMIT_ID_DESCRIBE = "git.commit.id.describe"
    private static final String KEY_GIT_DIRTY = "git.dirty"

    private static final String[] KEY_ALL = [
            KEY_GIT_BRANCH,
            KEY_GIT_COMMIT_ID, KEY_GIT_COMMIT_ID_ABBREVIATED,
            KEY_GIT_COMMIT_USER_NAME, KEY_GIT_COMMIT_USER_EMAIL,
            KEY_GIT_COMMIT_SHORT_MESSAGE, KEY_GIT_COMMIT_FULL_MESSAGE,
            KEY_GIT_COMMIT_TIME, KEY_GIT_COMMIT_ID_DESCRIBE,
            KEY_GIT_COMMIT_TIME, KEY_GIT_DIRTY
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
            def source = getSource()
            if (!project.gitProperties.failOnNoGitDirectory && source.empty)
                return
            def repo = Grgit.open(dir: source.head().parentFile)
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
                       , (KEY_GIT_COMMIT_ID_ABBREVIATED): repo.head().abbreviatedId
                       , (KEY_GIT_COMMIT_USER_NAME)     : repo.head().author.name
                       , (KEY_GIT_COMMIT_USER_EMAIL)    : repo.head().author.email
                       , (KEY_GIT_COMMIT_SHORT_MESSAGE) : repo.head().shortMessage
                       , (KEY_GIT_COMMIT_FULL_MESSAGE)  : repo.head().fullMessage
                       , (KEY_GIT_COMMIT_TIME)          : formatDate(repo.head().time, project.gitProperties.dateFormat, project.gitProperties.dateFormatTimeZone)
                       , (KEY_GIT_COMMIT_ID_DESCRIBE)   : commitIdDescribe(repo, '-dirty')
                       , (KEY_GIT_DIRTY)                : !repo.status().clean]

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

        private String commitIdDescribe(Grgit repo, String dirtyMark) {
            String describe
            if (repo.status().clean) {
                describe = repo.head().abbreviatedId
            } else {
                describe = repo.head().abbreviatedId + dirtyMark
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
    boolean failOnNoGitDirectory = true
}
