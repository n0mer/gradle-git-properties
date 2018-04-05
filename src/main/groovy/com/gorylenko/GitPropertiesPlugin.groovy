package com.gorylenko

import groovy.transform.ToString

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
}

@ToString(includeNames=true)
class GitPropertiesPluginExtension {
    def gitPropertiesDir
    def dotGitDirectory
    List keys = GitProperties.standardProperties
    Map<String, Object> customProperties = [:]
    String dateFormat = "yyyy-MM-dd'T'HH:mm:ssZ"
    String dateFormatTimeZone
    String extProperty
    boolean failOnNoGitDirectory = true
    boolean force

    void customProperty(String name, Object value) {
        customProperties.put(name, value)
    }
}
