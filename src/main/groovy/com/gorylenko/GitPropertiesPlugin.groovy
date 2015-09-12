package com.gorylenko

import org.ajoberstar.grgit.Grgit
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.TaskAction

import java.text.SimpleDateFormat

/**
 * @link <a href="http://www.insaneprogramming.be/blog/2014/08/15/spring-boot-info-git/">Spring Boot's info endpoint, Git and Gradle - InsaneProgramming</a>
 */
class GitPropertiesPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def task = project.tasks.create('generateGitProperties', GenerateGitPropertiesTask)
        task.setGroup(BasePlugin.BUILD_GROUP)
        ensureTaskRunsOnJavaClassesTask(project, task)
    }

    private static void ensureTaskRunsOnJavaClassesTask(Project project, Task task) {
        project.getTasks().getByName(JavaPlugin.CLASSES_TASK_NAME).dependsOn(task)
    }

    static class GenerateGitPropertiesTask extends DefaultTask {
        @TaskAction
        void generate() {
            def repo = Grgit.open(dir: project.rootProject.file('.'))
            def dir = new File(project.buildDir, "resources/main")
            def file = new File(project.buildDir, "resources/main/git.properties")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            if (!file.exists()) {
                file.createNewFile()
            }
            def map = ["git.branch"                : repo.branch.current.name
                       , "git.commit.id"           : repo.head().id
                       , "git.commit.id.abbrev"    : repo.head().abbreviatedId
                       , "git.commit.user.name"    : repo.head().author.name
                       , "git.commit.user.email"   : repo.head().author.email
                       , "git.commit.message.short": repo.head().shortMessage
                       , "git.commit.message.full" : repo.head().fullMessage
                       , "git.commit.time"         : new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(repo.head().time)]
            def props = new Properties()
            props.putAll(map)
            props.store(file.newWriter(), "")
        }
    }
}
