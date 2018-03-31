package com.gorylenko

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
            def dir = project.gitProperties.gitPropertiesDir ?: new File(project.buildDir, DEFAULT_OUTPUT_DIR)
            return new File(dir, GIT_PROPERTIES_FILENAME)
        }

        @TaskAction
        void generate() {
            def source = getSource()
            GitPropertiesPluginExtension gitProperties = project.gitProperties
            if (!gitProperties.failOnNoGitDirectory && source.empty)
                return
            File dotGitDirectory = getDotGitDirectory(project)
            logger.info "dotGitDirectory = [${dotGitDirectory.absolutePath}]"
            def repo = Grgit.open(dir: dotGitDirectory)
            def dir = gitProperties.gitPropertiesDir ?: new File(project.buildDir, DEFAULT_OUTPUT_DIR)
            def file = new File(dir, GIT_PROPERTIES_FILENAME)
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

            def newMap = new HashMap<String, String>()
            map.subMap(keys).each{ k, v -> newMap.put(k, v.call(repo).toString() ) }
            gitProperties.customProperties.each{ k, v -> newMap.put(k, v instanceof Closure ? v.call(repo).toString() : v.toString() ) }

            // Close Grgit to avoid issues with Gradle daemon
            repo.close()

            // Writing to properties file
            boolean written = new PropertiesFileWriter().write(newMap, file, gitProperties.force)
            if (written) {
                logger.info("Written to [${file}]...")
            } else {
                logger.info("Skip writing [${file}] as it is up-to-date.")
            }

        }

        File getDotGitDirectory(Project project) {
            return new GitDirLocator(project.projectDir).lookupGitDirectory(project.gitProperties.dotGitDirectory)
        }

    }
}

class GitPropertiesPluginExtension {
    File gitPropertiesDir
    File dotGitDirectory
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
