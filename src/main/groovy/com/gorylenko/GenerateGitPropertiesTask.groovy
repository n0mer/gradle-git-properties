package com.gorylenko

import org.gradle.api.GradleException
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
class GenerateGitPropertiesTask extends DefaultTask {
    public static final String TASK_NAME = "generateGitProperties"

    private static final String DEFAULT_OUTPUT_DIR = "resources/main"

    GenerateGitPropertiesTask() {
        // Description for the task
        description = 'Generate a git.properties file.'

        outputs.upToDateWhen {
            // when extProperty is configured or failOnNoGitDirectory=false always execute the task
            return !gitProperties.extProperty && gitProperties.failOnNoGitDirectory
        }
    }

    private Map<String, String> generatedProperties

    @Internal
    public GitPropertiesPluginExtension getGitProperties() {
        return project.gitProperties
    }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public FileTree getSource() {
        File dotGitDirectory = getDotGitDirectory(project)
        return (dotGitDirectory == null) ? project.files().asFileTree : project.files(dotGitDirectory).asFileTree
    }

    @OutputFile
    public File getOutput() {
        return getGitPropertiesFile(project)
    }

    /**
     * To support cacheable task and UP-TO-DATE check
     */
    @Input
    public Map<String, String> getGeneratedProperties() {

        if (this.generatedProperties != null) return this.generatedProperties

        if (logger.debugEnabled) {
            logger.debug("gitProperties = ${gitProperties}")
        }

        if (!gitProperties.failOnNoGitDirectory && getSource().empty) {
            logger.info("Exiting because no Git repository found and failOnNoGitDirectory = false.")
            return [:]
        }

        File dotGitDirectory = getDotGitDirectory(project)

        if (dotGitDirectory == null) {
            throw new GradleException("No Git repository found.")
        }

        logger.info("dotGitDirectory = [${dotGitDirectory?.absolutePath}]")


        // Generate properties

        GitProperties builder = new GitProperties()
        Map<String, String> newMap = builder.generate(dotGitDirectory,
                gitProperties.keys, gitProperties.dateFormat, gitProperties.dateFormatTimeZone, gitProperties.branch,
                project.version, gitProperties.customProperties)

        if (logger.debugEnabled) {
            logger.debug("Generated Git properties  = ${newMap}")
        }
        generatedProperties = newMap

        return this.generatedProperties
    }


    @TaskAction
    void generate() {

        if (!gitProperties.failOnNoGitDirectory && getSource().empty) {
            logger.info("Exiting because no Git repository found and failOnNoGitDirectory = false.")
            return
        }

        Map<String, String> newMap = getGeneratedProperties()

        // Expose generated properties to project.ext[gitProperties.extProperty] if configured

        if (gitProperties.extProperty) {
            logger.debug("Exposing git properties model to project.ext[${gitProperties.extProperty}]")
            project.ext[gitProperties.extProperty] = new HashMap(newMap)
        }

        // Write to git.properties file

        logger.debug("gitProperties.gitPropertiesResourceDir=" + gitProperties.gitPropertiesResourceDir)
        logger.debug("gitProperties.gitPropertiesDir=" + gitProperties.gitPropertiesDir)
        logger.debug("gitProperties.gitPropertiesName=" + gitProperties.gitPropertiesName)

        File file = getGitPropertiesFile(project)
        logger.info "git.properties location = [${file?.absolutePath}]"

        boolean written = new PropertiesFileWriter().write(newMap, file, gitProperties.force)
        if (written) {
            logger.info("Written properties to [${file}]...")
        } else {
            logger.info("Skip writing properties to [${file}] as it is up-to-date.")
        }

    }

    private static File getDotGitDirectory(Project project) {
        GitPropertiesPluginExtension gitProperties = project.gitProperties
        File dotGitDirectory = gitProperties.dotGitDirectory ? project.file(gitProperties.dotGitDirectory) : null
        return new GitDirLocator(project.projectDir).lookupGitDirectory(dotGitDirectory)
    }


    public void onJavaPluginAvailable() {
        // if Java plugin is used, this method will be called to register gitPropertiesResourceDir to classpath
        // at the end of evaluation phase (to make sure extension values are set)
        logger.debug "GenerateGitPropertiesTask: found Java plugin"
        if (gitProperties.gitPropertiesResourceDir) {
            logger.debug ("gitProperties.gitPropertiesResourceDir=" + gitProperties.gitPropertiesResourceDir)
            String gitPropertiesDir = getGitPropertiesDir(project).absolutePath
            project.sourceSets.main.resources.srcDir gitPropertiesDir
            logger.info "GenerateGitPropertiesTask: added classpath entry(gitPropertiesResourceDir): ${gitPropertiesDir}"
        }
    }

    private File getGitPropertiesDir(Project project) {
        GitPropertiesPluginExtension gitProperties = project.gitProperties

        File gitPropertiesDir
        if (gitProperties.gitPropertiesResourceDir) {
            gitPropertiesDir = project.file(gitProperties.gitPropertiesResourceDir)
        } else if (gitProperties.gitPropertiesDir) {
            gitPropertiesDir = project.file(gitProperties.gitPropertiesDir)
        } else {
            gitPropertiesDir = new File(project.buildDir, DEFAULT_OUTPUT_DIR)
        }

        return gitPropertiesDir
    }

    private File getGitPropertiesFile(Project project) {
        GitPropertiesPluginExtension gitProperties = project.gitProperties
        File gitPropertiesDir = getGitPropertiesDir(project)
        File gitPropertiesFile = new File(gitPropertiesDir, gitProperties.gitPropertiesName)
        return gitPropertiesFile
    }
}
