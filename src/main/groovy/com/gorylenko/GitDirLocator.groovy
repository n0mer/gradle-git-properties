package com.gorylenko

import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import org.ajoberstar.grgit.Grgit
import org.gradle.api.Project

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
            grgit.close()
        } else {
            dotGitDir = null
        }

        return dotGitDir
    }

}
