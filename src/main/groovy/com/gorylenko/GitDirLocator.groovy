package com.gorylenko

import java.io.File
import org.ajoberstar.grgit.Grgit

/**
 * Encapsulates logic to locate a valid .git directory.
 *
 */
class GitDirLocator {

    private File projectBasedir

    public GitDirLocator(File projectBasedir) {
        this.projectBasedir = projectBasedir
    }

    public File lookupGitDirectory(File manuallyConfiguredDir) {
        Grgit grgit

        if (manuallyConfiguredDir && manuallyConfiguredDir.exists()) {
            try {
                grgit = Grgit.open(dir: manuallyConfiguredDir)
            } catch (Exception e) {
                // could not open the configured dotGit dir
            }
        }

        if (!grgit) {
            try {
                // Detect automatically from project dir and go up until until find .git dir or .git file
                grgit = Grgit.open(currentDir: projectBasedir)
            } catch (Exception e) {
            }
        }

        File dotGitDir

        if (grgit != null) {
            dotGitDir = grgit.repository.rootDir
            // Workaround to avoid Gradle bug on Java 11 regarding changing working directory unsuccessfully causing issues regarding relative file paths
            if (dotGitDir.exists() && !dotGitDir.absoluteFile.exists()) {
                dotGitDir = new File(projectBasedir, dotGitDir.getPath())
            }
            grgit.close()
        } else {
            dotGitDir = null
        }

        return dotGitDir
    }

}
