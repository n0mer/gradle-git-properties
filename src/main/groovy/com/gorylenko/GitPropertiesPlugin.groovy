package com.gorylenko

import groovy.transform.ToString
import java.text.SimpleDateFormat
import java.time.Instant

import org.ajoberstar.grgit.Grgit
import org.gradle.api.Action
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

import com.gorylenko.properties.BranchProperty
import com.gorylenko.properties.BuildHostProperty
import com.gorylenko.properties.BuildTimeProperty
import com.gorylenko.properties.BuildUserEmailProperty
import com.gorylenko.properties.BuildUserNameProperty
import com.gorylenko.properties.BuildVersionProperty
import com.gorylenko.properties.ClosestTagCommitCountProperty
import com.gorylenko.properties.ClosestTagNameProperty
import com.gorylenko.properties.CommitIdAbbrevProperty
import com.gorylenko.properties.CommitIdDescribeProperty
import com.gorylenko.properties.CommitIdProperty
import com.gorylenko.properties.CommitMessageFullProperty
import com.gorylenko.properties.CommitMessageShortProperty
import com.gorylenko.properties.CommitTimeProperty
import com.gorylenko.properties.CommitUserEmailProperty
import com.gorylenko.properties.CommitUserNameProperty
import com.gorylenko.properties.DirtyProperty
import com.gorylenko.properties.RemoteOriginUrlProperty
import com.gorylenko.properties.TagsProperty
import com.gorylenko.properties.TotalCommitCountProperty

class GitPropertiesPlugin implements Plugin<Project> {

    private static final String EXTENSION_NAME = "gitProperties"
    private static final String TASK_NAME = "generateGitProperties"

    private static final String GIT_PROPERTIES_FILENAME = "git.properties"
    private static final String DEFAULT_OUTPUT_DIR = "resources/main"

    private static final String KEY_GIT_BRANCH = "git.branch"
    private static final String KEY_GIT_COMMIT_ID = "git.commit.id"
    private static final String KEY_GIT_COMMIT_ID_ABBREVIATED = "git.commit.id.abbrev"
    private static final String KEY_GIT_COMMIT_USER_NAME = "git.commit.user.name"
    private static final String KEY_GIT_COMMIT_USER_EMAIL = "git.commit.user.email"
    private static final String KEY_GIT_COMMIT_SHORT_MESSAGE = "git.commit.message.short"
    private static final String KEY_GIT_COMMIT_FULL_MESSAGE = "git.commit.message.full"
    private static final String KEY_GIT_COMMIT_TIME = "git.commit.time"
    private static final String KEY_GIT_COMMIT_ID_DESCRIBE = "git.commit.id.describe"
    private static final String KEY_GIT_REMOTE_ORIGIN_URL = "git.remote.origin.url"
    private static final String KEY_GIT_TAGS = "git.tags"
    private static final String KEY_GIT_CLOSEST_TAG_NAME = "git.closest.tag.name"
    private static final String KEY_GIT_CLOSEST_TAG_COMMIT_COUNT = "git.closest.tag.commit.count"
    private static final String KEY_GIT_TOTAL_COMMIT_COUNT = "git.total.commit.count"
    private static final String KEY_GIT_DIRTY = "git.dirty"
    private static final String KEY_GIT_BUILD_USER_NAME = "git.build.user.name"
    private static final String KEY_GIT_BUILD_USER_EMAIL = "git.build.user.email"
    private static final String KEY_GIT_BUILD_TIME = "git.build.time"
    private static final String KEY_GIT_BUILD_VERSION = "git.build.version"
    private static final String KEY_GIT_BUILD_HOST = "git.build.host"

    public static final String[] KEY_ALL = [
            KEY_GIT_BRANCH,
            KEY_GIT_COMMIT_ID, KEY_GIT_COMMIT_ID_ABBREVIATED,
            KEY_GIT_COMMIT_USER_NAME, KEY_GIT_COMMIT_USER_EMAIL,
            KEY_GIT_COMMIT_SHORT_MESSAGE, KEY_GIT_COMMIT_FULL_MESSAGE,
            KEY_GIT_COMMIT_ID_DESCRIBE,
            KEY_GIT_COMMIT_TIME,
            KEY_GIT_REMOTE_ORIGIN_URL,
            KEY_GIT_TAGS,
            KEY_GIT_CLOSEST_TAG_NAME,
            KEY_GIT_CLOSEST_TAG_COMMIT_COUNT,
            KEY_GIT_TOTAL_COMMIT_COUNT,
            KEY_GIT_DIRTY,
            KEY_GIT_BUILD_USER_NAME,
            KEY_GIT_BUILD_USER_EMAIL,
            KEY_GIT_BUILD_TIME,
            KEY_GIT_BUILD_VERSION,
            KEY_GIT_BUILD_HOST
    ]

    @Override
    void apply(Project project) {

        project.extensions.create(EXTENSION_NAME, GitPropertiesPluginExtension)
        def task = project.tasks.create(TASK_NAME, GenerateGitPropertiesTask)

        task.setGroup(BasePlugin.BUILD_GROUP)
        ensureTaskRunsOnJavaClassesTask(project, task)
    }

    private static void ensureTaskRunsOnJavaClassesTask(Project project, Task task) {
        // if Java plugin is applied, execute this task automatically when "classes" task is executed
        // see https://guides.gradle.org/implementing-gradle-plugins/#reacting_to_plugins
        project.getPlugins().withType(JavaPlugin.class, new Action<JavaPlugin>() {
            public void execute(JavaPlugin javaPlugin) {
                project.getTasks().getByName(JavaPlugin.CLASSES_TASK_NAME).dependsOn(task)
            }
        })
    }

    static class GenerateGitPropertiesTask extends DefaultTask {

        GenerateGitPropertiesTask() {
            // Description for the task
            description = 'Generate a git.properties file.'
        }

        @InputFiles
        public FileTree getSource() {
            File dotGitDirectory = getDotGitDirectory(project)
            return (dotGitDirectory == null) ? project.files().asFileTree : project.files(dotGitDirectory).asFileTree
        }

        @OutputFile
        public File getOutput() {
            return getGitPropertiesFile(project)
        }

        @TaskAction
        void generate() {
            GitPropertiesPluginExtension gitProperties = project.gitProperties

            if (logger.debugEnabled)
                logger.debug("gitProperties = ${gitProperties}")

            if (!gitProperties.failOnNoGitDirectory && getSource().empty) {
                logger.info("Exiting because no Git repository found and failOnNoGitDirectory = true.")
                return
            }

            File dotGitDirectory = getDotGitDirectory(project)
            logger.info "dotGitDirectory = [${dotGitDirectory?.absolutePath}]"

            Map map = getProperties(gitProperties)
            if (logger.debugEnabled)
                logger.debug("Git properties to be generated = ${map.keySet()}")

            Map<String, String> newMap = generateProperties(map, dotGitDirectory)
            if (logger.debugEnabled)
                logger.debug("Generated Git properties  = ${newMap}")

            // Writing to properties file
            File file = getGitPropertiesFile(project)
            logger.info "git.properties location = [${file?.absolutePath}]"

            boolean written = new PropertiesFileWriter().write(newMap, file, gitProperties.force)
            if (written) {
                logger.info("Written properties to [${file}]...")
            } else {
                logger.info("Skip writing properties to [${file}] as it is up-to-date.")
            }

        }

        File getDotGitDirectory(Project project) {
            File dotGitDirectory = project.gitProperties.dotGitDirectory ? project.file(project.gitProperties.dotGitDirectory) : null
            return new GitDirLocator(project.projectDir).lookupGitDirectory(dotGitDirectory)
        }

        File getGitPropertiesFile(Project project) {
            File gitPropertiesDir = project.gitProperties.gitPropertiesDir ? project.file(project.gitProperties.gitPropertiesDir) : new File(project.buildDir, DEFAULT_OUTPUT_DIR)
            File gitPropertiesFile = new File(gitPropertiesDir, GIT_PROPERTIES_FILENAME)
            return gitPropertiesFile
        }

        Map getProperties(GitPropertiesPluginExtension gitProperties) {
            def keys = gitProperties.keys

            def map = [(KEY_GIT_BRANCH)                     : new BranchProperty()
                       , (KEY_GIT_COMMIT_ID)                : new CommitIdProperty()
                       , (KEY_GIT_COMMIT_ID_ABBREVIATED)    : new CommitIdAbbrevProperty()
                       , (KEY_GIT_COMMIT_USER_NAME)         : new CommitUserNameProperty()
                       , (KEY_GIT_COMMIT_USER_EMAIL)        : new CommitUserEmailProperty()
                       , (KEY_GIT_COMMIT_SHORT_MESSAGE)     : new CommitMessageShortProperty()
                       , (KEY_GIT_COMMIT_FULL_MESSAGE)      : new CommitMessageFullProperty()
                       , (KEY_GIT_COMMIT_TIME)              : new CommitTimeProperty(gitProperties.dateFormat, gitProperties.dateFormatTimeZone)
                       , (KEY_GIT_COMMIT_ID_DESCRIBE)       : new CommitIdDescribeProperty()
                       , (KEY_GIT_REMOTE_ORIGIN_URL)        : new RemoteOriginUrlProperty()
                       , (KEY_GIT_TAGS)                     : new TagsProperty()
                       , (KEY_GIT_CLOSEST_TAG_NAME)         : new ClosestTagNameProperty()
                       , (KEY_GIT_CLOSEST_TAG_COMMIT_COUNT) : new ClosestTagCommitCountProperty()
                       , (KEY_GIT_TOTAL_COMMIT_COUNT)       : new TotalCommitCountProperty()
                       , (KEY_GIT_DIRTY)                    : new DirtyProperty()
                       , (KEY_GIT_BUILD_USER_NAME)          : new BuildUserNameProperty()
                       , (KEY_GIT_BUILD_USER_EMAIL)         : new BuildUserEmailProperty()
                       , (KEY_GIT_BUILD_TIME)               : new BuildTimeProperty(Instant.now(), gitProperties.dateFormat, gitProperties.dateFormatTimeZone)
                       , (KEY_GIT_BUILD_VERSION)            : new BuildVersionProperty(project.version)
                       , (KEY_GIT_BUILD_HOST)               : new BuildHostProperty()]

            map = map.subMap(keys)

            if (gitProperties.customProperties)
                map.putAll(gitProperties.customProperties)

            return map
        }

        Map<String, String> generateProperties(Map map, File dotGitDirectory) {
            def newMap = new HashMap<String, String>()
            def repo = Grgit.open(dir: dotGitDirectory)

            try {
                map.each{ k, v -> newMap.put(k, v instanceof Closure ? v.call(repo).toString() : v.toString() ) }
            } finally {
                // Close Grgit to avoid issues with Gradle daemon
                repo.close()
            }

            return newMap
        }
    }
}

@ToString(includeNames=true)
class GitPropertiesPluginExtension {
    def gitPropertiesDir
    def dotGitDirectory
    List keys = GitPropertiesPlugin.KEY_ALL.toList()
    Map<String, Object> customProperties = new HashMap<String, Object>()
    String dateFormat = "yyyy-MM-dd'T'HH:mm:ssZ"
    String dateFormatTimeZone
    boolean failOnNoGitDirectory = true
    boolean force

    void customProperty(String name, Object value) {
        customProperties.put(name, value)
    }
}
