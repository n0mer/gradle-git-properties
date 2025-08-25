package com.gorylenko

import groovy.transform.ToString
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.plugins.BasePlugin
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
        project.plugins.withType(JavaPlugin) {
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
    final DirectoryProperty gitPropertiesDir
    final DirectoryProperty gitPropertiesResourceDir
    String gitPropertiesName = "git.properties"
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
        dotGitDirectory = project.objects.directoryProperty().convention(findGitDirectory(project))
    }
    
    private static Directory findGitDirectory(Project project) {
        // Start from current project and walk up to find .git directory
        Project currentProject = project
        while (currentProject != null) {
            def gitDir = currentProject.layout.projectDirectory.dir(".git")
            def gitFile = gitDir.asFile
            
            if (gitFile.exists()) {
                // Check if it's a worktree (file with gitdir: reference)
                if (gitFile.isFile()) {
                    def resolvedGitDir = resolveWorktreeGitDir(gitFile, currentProject)
                    if (resolvedGitDir != null) {
                        return resolvedGitDir
                    }
                } else if (gitFile.isDirectory()) {
                    // Regular .git directory
                    return gitDir
                }
            }
            currentProject = currentProject.parent
        }
        
        // Fallback to current project's .git directory (original behavior)
        return project.layout.projectDirectory.dir(".git")
    }
    
    private static Directory resolveWorktreeGitDir(File gitFile, Project project) {
        // Read the .git file to find the actual git directory
        def lines = gitFile.readLines()
        def gitDirLine = lines.find { it.startsWith("gitdir: ") }
        
        if (gitDirLine) {
            def gitPath = gitDirLine.substring("gitdir: ".length()).trim()
            
            // Convert to File to handle both absolute and relative paths
            File gitDir
            if (new File(gitPath).isAbsolute()) {
                gitDir = new File(gitPath)
            } else {
                // Relative path - resolve relative to the .git file's parent directory
                gitDir = new File(gitFile.parentFile, gitPath).canonicalFile
            }
            
            // Check if it's a worktree path
            def gitDirPath = gitDir.absolutePath
            def worktreesIndex = gitDirPath.lastIndexOf(File.separator + "worktrees" + File.separator)
            if (worktreesIndex > 0) {
                // Return the main git directory (before /worktrees/)
                gitDir = new File(gitDirPath.substring(0, worktreesIndex))
            }
            
            // Convert back to Directory
            return project.layout.projectDirectory.dir(gitDir.absolutePath)
        }
        
        return null
    }

    void customProperty(String name, Object value) {
        customProperties.put(name, value)
    }
}
