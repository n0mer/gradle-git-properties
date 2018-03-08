/**
 * This class is modified from GitDirLocator.java file
 * (from https://github.com/ktoso/maven-git-commit-id-plugin/blob/master/src/main/java/pl/project13/maven/git/GitDirLocator.java)
 *
 */

 package com.gorylenko

import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException

import org.gradle.api.Project

/**
 * Encapsulates logic to locate a valid .git directory.
 *
 */
class GitDirLocator {

    private Project project

    public GitDirLocator(Project project) {
        this.project = project
    }

    public File lookupGitDirectory(File manuallyConfiguredDir) {

        if (manuallyConfiguredDir != null && manuallyConfiguredDir.exists()) {

            // If manuallyConfiguredDir is a directory then we can use it as the git path.
            if (manuallyConfiguredDir.isDirectory()) {
                return manuallyConfiguredDir
            }

            // If the path exists but is not a directory it might be a git submodule
            // "gitdir" link.
            File gitDirLinkPath = processGitDirFile(manuallyConfiguredDir)

            // If the linkPath was found from the file and it exists then use it.
            if (isExistingDirectory(gitDirLinkPath)) {
                return gitDirLinkPath
            }
        }

        return findProjectGitDirectory()
    }

    /**
     * Search up all the maven parent project hierarchy until a .git directory is
     * found.
     *
     * @return File which represents the location of the .git directory or NULL if
     *         none found.
     */
    private File findProjectGitDirectory() {
        if (this.project == null) {
            return null
        }

        File basedir = project.getProjectDir()

        while (basedir != null) {
            File gitdir = new File(basedir, ".git")

            if (gitdir.exists()) {
                if (gitdir.isDirectory()) {
                    return gitdir
                } else if (gitdir.isFile()) {
                    return processGitDirFile(gitdir)
                } else {
                    return null
                }
            }
            basedir = basedir.getParentFile()
        }
        return null
    }

    /**
     * Load a ".git" git submodule file and read the gitdir path from it.
     *
     * @return File object with path loaded or null
     */
    private File processGitDirFile(File file) {
        try {
            BufferedReader reader = null

            try {
                reader = new BufferedReader(new FileReader(file))

                // There should be just one line in the file, e.g.
                // "gitdir: /usr/local/src/parentproject/.git/modules/submodule"
                String line = reader.readLine()
                if (line == null) {
                    return null
                }
                // Separate the key and the value in the string.
                String[] parts = line.split(": ")

                // If we don't have 2 parts or if the key is not gitdir then give up.
                if (parts.length != 2 || !parts[0].equals("gitdir")) {
                    return null
                }

                // All seems ok so return the "gitdir" value read from the file.
                File gitDir = new File(parts[1])
                if (gitDir.isAbsolute()) {
                    // gitdir value is an absolute path. Return as-is
                    return gitDir
                } else {
                    // gitdir value is relative.
                    return new File(file.getParentFile(), parts[1])
                }
            } catch (FileNotFoundException e) {
                return null
            } finally {
                if (reader != null) {
                    reader.close()
                }
            }
        } catch (IOException e) {
            return null
        }
    }

    private static boolean isExistingDirectory(File fileLocation) {
        return fileLocation != null && fileLocation.exists() && fileLocation.isDirectory()
    }
}
