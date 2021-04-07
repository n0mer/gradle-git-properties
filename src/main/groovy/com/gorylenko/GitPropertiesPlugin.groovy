package com.gorylenko

import groovy.transform.ToString
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

class GitPropertiesPlugin implements Plugin<Project> {

    private static final String EXTENSION_NAME = "gitProperties"
    private static final String DEFAULT_OUTPUT_DIR = "resources/main"

    @Override
    void apply(Project project) {
        def extension = project.extensions.create(EXTENSION_NAME, GitPropertiesPluginExtension, project)
        def task = project.tasks.register(GenerateGitPropertiesTask.TASK_NAME, GenerateGitPropertiesTask) {
            group = BasePlugin.BUILD_GROUP
        }

        // if Java plugin is applied, execute this task automatically when "classes" task is executed
        // see https://guides.gradle.org/implementing-gradle-plugins/#reacting_to_plugins
        project.plugins.withType(JavaBasePlugin) {
            project.tasks.named(JavaPlugin.CLASSES_TASK_NAME).configure {
                dependsOn(task)

                // if Java plugin is used, this method will be called to register gitPropertiesResourceDir to classpath
                // at the end of evaluation phase (to make sure extension values are set)
                if (extension.gitPropertiesResourceDir.present) {
                    String gitPropertiesDir = getGitPropertiesDir(extension, project.layout).asFile.absolutePath
                    def sourceSets = project.extensions.getByType(SourceSetContainer)
                    sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME).configure {
                        it.resources.srcDir(gitPropertiesDir)
                    }
                }
            }
        }
    }

    private static Directory getGitPropertiesDir(GitPropertiesPluginExtension extension, ProjectLayout layout) {
        if (extension.gitPropertiesResourceDir.present) {
            return extension.gitPropertiesResourceDir.get()
        } else if (extension.gitPropertiesDir.present) {
            return extension.gitPropertiesDir.get()
        } else {
            return layout.buildDirectory.dir(DEFAULT_OUTPUT_DIR).get()
        }
    }
}

@ToString(includeNames=true)
class GitPropertiesPluginExtension {
    @InputDirectory
    final DirectoryProperty gitPropertiesDir
    @InputDirectory
    final DirectoryProperty gitPropertiesResourceDir
    String gitPropertiesName = "git.properties"
    @InputDirectory
    final DirectoryProperty dotGitDirectory
    List keys = GitProperties.standardProperties
    Map<String, Object> customProperties = [:]
    String dateFormat = "yyyy-MM-dd'T'HH:mm:ssZ"
    String dateFormatTimeZone
    String branch
    String extProperty
    boolean failOnNoGitDirectory = true
    boolean force

    GitPropertiesPluginExtension(Project project) {
        gitPropertiesDir = project.objects.directoryProperty()
        gitPropertiesResourceDir = project.objects.directoryProperty()
        dotGitDirectory = project.objects.directoryProperty().convention(project.layout.projectDirectory.dir(".git"))
    }

    void customProperty(String name, Object value) {
        customProperties.put(name, value)
    }
}
