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
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
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

    private final ObjectFactory objectFactory;
    private final ProjectLayout layout;
    private final RegularFileProperty outputFileProperty
    private final DirectoryProperty dotGitDirectoryProperty
    private final GitPropertiesPluginExtension gitProperties
    private final Property<String> versionProperty;

    @Inject
    GenerateGitPropertiesTask(ObjectFactory objectFactory, ProjectLayout projectLayout) {
        // Description for the task
        description = 'Generate a git.properties file.'

        this.objectFactory = objectFactory
        this.layout = projectLayout
        this.gitProperties = project.extensions.getByType(GitPropertiesPluginExtension)
        this.outputFileProperty = getGitPropertiesFile();
        this.versionProperty = objectFactory.property(String.class);
        versionProperty.set(getProject().getVersion().toString());

        dotGitDirectoryProperty = objectFactory.directoryProperty()


        outputs.upToDateWhen { GenerateGitPropertiesTask task ->
            // when extProperty is configured or failOnNoGitDirectory=false always execute the task
            return !task.getGitProperties().extProperty && task.getGitProperties().failOnNoGitDirectory
        }
    }

    private Map<String, String> generatedProperties

    @OutputFile
    public RegularFileProperty getOutput() {
        return this.outputFileProperty
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
        dotGitDirectoryProperty.set(new GitDirLocator(layout.projectDirectory.asFile).lookupGitDirectory(this.gitProperties.dotGitDirectory.asFile.get()))

        if (!gitProperties.failOnNoGitDirectory && !dotGitDirectoryProperty.present) {
            logger.info("Exiting because no Git repository found and failOnNoGitDirectory = false.")
            return [:]
        }

        File dotGitDirectory = this.dotGitDirectoryProperty.getAsFile().getOrNull()

        if (dotGitDirectory == null) {
            throw new GradleException("No Git repository found.")
        }

        logger.info("dotGitDirectory = [${dotGitDirectory?.absolutePath}]")


        // Generate properties

        GitProperties builder = new GitProperties()
        Map<String, String> newMap = builder.generate(dotGitDirectory,
                gitProperties.keys, gitProperties.dateFormat, gitProperties.dateFormatTimeZone, gitProperties.branch,
                versionProperty.get(), gitProperties.customProperties)

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
        Map<String, String> newMap = getGeneratedProperties()
        // Write to git.properties file

        logger.debug("gitProperties.gitPropertiesResourceDir=${gitProperties.gitPropertiesResourceDir}")
        logger.debug("gitProperties.gitPropertiesDir=${gitProperties.gitPropertiesDir}")
        logger.debug("gitProperties.gitPropertiesName=${gitProperties.gitPropertiesName}")
        def file = outputFileProperty
        def absolutePath = outputFileProperty.get().asFile.absolutePath
        logger.info "git.properties location = [${absolutePath}]"

        boolean written = new PropertiesFileWriter().write(newMap, file.asFile.get(), gitProperties.force)
        if (written) {
            logger.info("Written properties to [${file}]...")
        } else {
            logger.info("Skip writing properties to [${file}] as it is up-to-date.")
        }
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
