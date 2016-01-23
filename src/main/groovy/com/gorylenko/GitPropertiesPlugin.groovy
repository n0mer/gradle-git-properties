package com.gorylenko

import org.ajoberstar.grgit.Grgit
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.TaskAction

/**
 * @link <a href="http://www.insaneprogramming.be/blog/2014/08/15/spring-boot-info-git/">Spring Boot's info endpoint, Git and Gradle - InsaneProgramming</a>
 */
class GitPropertiesPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        project.extensions.create("gitProperties", GitPropertiesPluginExtension)
        def task = project.tasks.create('generateGitProperties', GenerateGitPropertiesTask)
        task.inputs.dir(new File(project.gitProperties.gitRepositoryRoot ?: project.rootProject.file('.'),".git"))

        def dir = project.gitProperties.gitPropertiesDir ?: new File(project.buildDir, "resources/main")
        task.outputs.file(new File(dir, "git.properties"))

        task.setGroup(BasePlugin.BUILD_GROUP)
        ensureTaskRunsOnJavaClassesTask(project, task)
    }

    private static void ensureTaskRunsOnJavaClassesTask(Project project, Task task) {
        project.getTasks().getByName(JavaPlugin.CLASSES_TASK_NAME).dependsOn(task)
    }

    static class GenerateGitPropertiesTask extends DefaultTask {
        @TaskAction
        void generate() {
            def repo = Grgit.open(dir: project.gitProperties.gitRepositoryRoot ?: project.rootProject.file('.'))
            def dir = project.gitProperties.gitPropertiesDir ?: new File(project.buildDir, "resources/main")
            def file = new File(dir, "git.properties")
            def keys = project.gitProperties.keys ?: ['git.branch', 'git.commit.id', 'git.commit.id.abbrev', 'git.commit.user.name', 'git.commit.user.email', 'git.commit.message.short', 'git.commit.message.full', 'git.commit.time']
            if (!dir.exists()) {
                dir.mkdirs()
            }
            if (!file.exists()) {
                file.createNewFile()
                logger.info "writing to [${file}]"
            }
            def map = ["git.branch"                : repo.branch.current.name
                       , "git.commit.id"           : repo.head().id
                       , "git.commit.id.abbrev"    : repo.head().abbreviatedId
                       , "git.commit.user.name"    : repo.head().author.name
                       , "git.commit.user.email"   : repo.head().author.email
                       , "git.commit.message.short": repo.head().shortMessage
                       , "git.commit.message.full" : repo.head().fullMessage
                       , "git.commit.time"         : repo.head().time.toString()]
            def props = new Properties()
            props.putAll(map.subMap(project.gitProperties.keys))
            props.store(file.newWriter(), "")
        }
    }
}

class GitPropertiesPluginExtension {
    File gitPropertiesDir
    File gitRepositoryRoot
    List keys
}
