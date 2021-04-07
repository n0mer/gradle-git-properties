package com.gorylenko

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Transformer
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject

@CacheableTask
public class GenerateGitPropertiesTask extends DefaultTask {
    public static final String TASK_NAME = "generateGitProperties"

    private static final String DEFAULT_OUTPUT_DIR = "resources/main"

    private final GitPropertiesPluginExtension gitProperties

    GenerateGitPropertiesTask() {
        // Description for the task
        description = 'Generate a git.properties file.'

        this.gitProperties = project.extensions.getByType(GitPropertiesPluginExtension)

        outputs.upToDateWhen { GenerateGitPropertiesTask task ->
            // when extProperty is configured or failOnNoGitDirectory=false always execute the task
            return !task.getGitProperties().extProperty && task.getGitProperties().failOnNoGitDirectory
        }
    }

    private Map<String, String> generatedProperties

    @Inject
    ObjectFactory getObjectFactory() {
        throw new UnsupportedOperationException()
    }

    @Inject
    ProjectLayout getLayout() {
        throw new UnsupportedOperationException()
    }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public FileTree getSource() {
        File dotGitDirectory = getDotGitDirectory()
        return (dotGitDirectory == null) ? layout.files().asFileTree : layout.files(dotGitDirectory).asFileTree
    }

    @OutputFile
    public RegularFileProperty getOutput() {
        return getGitPropertiesFile()
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

        File dotGitDirectory = getDotGitDirectory()

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

    @Internal
    GitPropertiesPluginExtension getGitProperties() {
        return gitProperties
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

        logger.debug("gitProperties.gitPropertiesResourceDir=${gitProperties.gitPropertiesResourceDir}")
        logger.debug("gitProperties.gitPropertiesDir=${gitProperties.gitPropertiesDir}")
        logger.debug("gitProperties.gitPropertiesName=${gitProperties.gitPropertiesName}")

        RegularFileProperty file = getGitPropertiesFile()
        def absolutePath = file.asFile.map(new Transformer<String, File>() {
            @Override
            String transform(File f) {
                f.absolutePath
            }
        }).getOrElse("unknown")
        logger.info "git.properties location = [${absolutePath}]"

        boolean written = new PropertiesFileWriter().write(newMap, file.asFile.get(), gitProperties.force)
        if (written) {
            logger.info("Written properties to [${file}]...")
        } else {
            logger.info("Skip writing properties to [${file}] as it is up-to-date.")
        }
    }

    private File getDotGitDirectory() {
        DirectoryProperty dotGitDirectory = gitProperties.dotGitDirectory
        return new GitDirLocator(layout.projectDirectory.asFile).lookupGitDirectory(dotGitDirectory.asFile.get())
    }

    private Directory getGitPropertiesDir() {
        if (gitProperties.gitPropertiesResourceDir.present) {
            return gitProperties.gitPropertiesResourceDir.get()
        } else if (gitProperties.gitPropertiesDir.present) {
            return gitProperties.gitPropertiesDir.get()
        } else {
            return layout.buildDirectory.dir(DEFAULT_OUTPUT_DIR).get()
        }
    }

    private RegularFileProperty getGitPropertiesFile() {
        def fileProperty = objectFactory.fileProperty()
        fileProperty.set(getGitPropertiesDir().file(gitProperties.gitPropertiesName))
        return fileProperty
    }
}
