package com.gorylenko.jgit

import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

/**
 * Factory for opening JGit Repository instances.
 * Handles both regular git directories and git worktrees.
 */
class RepositoryFactory {

    /**
     * Opens a Git repository from the specified directory.
     * Handles worktree case where .git is a file pointing to actual git dir.
     *
     * @param directory the working directory (can be regular repo or worktree)
     * @return the opened Repository
     */
    static Repository open(File directory) {
        // Handle case where directory IS the .git directory (named ".git" or ends with ".git")
        if (directory.name == ".git" || directory.name.endsWith(".git")) {
            // directory is already the git dir
            if (directory.isFile()) {
                // Worktree case: .git is a file containing "gitdir: <path>"
                return openWorktree(directory.parentFile, directory)
            }
            // Regular .git directory
            return new FileRepositoryBuilder()
                    .setGitDir(directory)
                    .readEnvironment()
                    .build()
        }

        // Check if directory is a worktree gitdir (contains HEAD file and commondir file)
        def headFile = new File(directory, "HEAD")
        def commonDirFile = new File(directory, "commondir")
        if (headFile.exists() && commonDirFile.exists()) {
            // This is a worktree's gitdir (e.g., .git/worktrees/<branch-name>)
            return openWorktreeGitdir(directory, commonDirFile)
        }

        // Check if directory is a bare git directory or submodule gitdir (has HEAD but no commondir)
        // This handles: .git/modules/<submodule-name>/ directories
        if (headFile.exists() && !commonDirFile.exists()) {
            def configFile = new File(directory, "config")
            def objectsDir = new File(directory, "objects")
            if (configFile.exists() && objectsDir.exists()) {
                // This looks like a git directory (has HEAD, config, objects)
                // Try to find worktree from config
                return openSubmoduleGitdir(directory)
            }
        }

        // directory is the working tree, .git is a child
        def gitFile = new File(directory, ".git")

        if (gitFile.isFile()) {
            // Worktree case: .git is a file containing "gitdir: <path>"
            return openWorktree(directory, gitFile)
        }

        // Regular repository case
        return new FileRepositoryBuilder()
                .setGitDir(gitFile)
                .readEnvironment()
                .build()
    }

    private static Repository openWorktreeGitdir(File worktreeGitDir, File commonDirFile) {
        // Read commondir to find the main repo's git directory
        def commonDirPath = commonDirFile.text.trim()
        def mainGitDir = new File(worktreeGitDir, commonDirPath).canonicalFile

        // Try to find the worktree working directory from the gitdir file
        def gitdirFile = new File(worktreeGitDir, "gitdir")
        File workTree = null
        if (gitdirFile.exists()) {
            // gitdir file contains path to the worktree's .git file
            def worktreeGitPath = gitdirFile.text.trim()
            def worktreeGitFile = new File(worktreeGitPath)
            if (worktreeGitFile.exists()) {
                workTree = worktreeGitFile.parentFile
            }
        }

        // Open the MAIN repo for object/ref resolution
        def builder = new FileRepositoryBuilder()
                .setGitDir(mainGitDir)
                .readEnvironment()

        if (workTree != null) {
            builder.setWorkTree(workTree)
        }

        return builder.build()
    }

    /**
     * Detects if the given directory is a worktree (either working dir or gitdir)
     * and returns the worktree gitdir path, or null if not a worktree.
     *
     * This is used by GitFacade to enable worktree-specific HEAD/branch resolution.
     */
    static File detectWorktreeGitDir(File directory) {
        // Case 1: directory is already a worktree gitdir (has commondir + HEAD)
        def commonDirFile = new File(directory, "commondir")
        def headFile = new File(directory, "HEAD")
        if (commonDirFile.exists() && headFile.exists()) {
            return directory
        }

        // Case 2: directory is a working directory with .git file (worktree)
        def gitFile = new File(directory, ".git")
        if (gitFile.isFile()) {
            def content = gitFile.text.trim()
            if (content.startsWith("gitdir:")) {
                def gitDirPath = content.substring("gitdir:".length()).trim()
                def worktreeGitDir = new File(gitDirPath)
                if (!worktreeGitDir.isAbsolute()) {
                    worktreeGitDir = new File(directory, gitDirPath).canonicalFile
                }
                // Verify it's a worktree gitdir (has commondir)
                if (new File(worktreeGitDir, "commondir").exists()) {
                    return worktreeGitDir
                }
            }
        }

        // Case 3: directory has .git as the name and its a FILE (edge case)
        if (directory.name == ".git" && directory.isFile()) {
            def content = directory.text.trim()
            if (content.startsWith("gitdir:")) {
                def gitDirPath = content.substring("gitdir:".length()).trim()
                def worktreeGitDir = new File(gitDirPath)
                if (!worktreeGitDir.isAbsolute()) {
                    worktreeGitDir = new File(directory.parentFile, gitDirPath).canonicalFile
                }
                if (new File(worktreeGitDir, "commondir").exists()) {
                    return worktreeGitDir
                }
            }
        }

        return null
    }

    /**
     * Opens a submodule gitdir (e.g., .git/modules/<name>/).
     * Reads the worktree path from [core] worktree config if available.
     */
    private static Repository openSubmoduleGitdir(File gitDir) {
        // Try to read worktree from config
        File workTree = null
        def configFile = new File(gitDir, "config")
        if (configFile.exists()) {
            def content = configFile.text
            def match = content =~ /\[core\][^\[]*worktree\s*=\s*(.+)/
            if (match.find()) {
                def worktreePath = match.group(1).trim()
                workTree = new File(worktreePath)
                if (!workTree.isAbsolute()) {
                    workTree = new File(gitDir, worktreePath).canonicalFile
                }
            }
        }

        def builder = new FileRepositoryBuilder()
                .setGitDir(gitDir)
                .readEnvironment()

        if (workTree != null && workTree.exists()) {
            builder.setWorkTree(workTree)
        }

        return builder.build()
    }

    /**
     * Resolves .git file (worktree) to actual gitdir, or returns directory as-is.
     * Used for determining directories to watch for incremental builds.
     */
    private static File resolveDotGit(File dotGit) {
        if (dotGit == null || !dotGit.exists()) return null
        if (dotGit.isDirectory()) return dotGit
        if (!dotGit.isFile()) return null
        def content = dotGit.text?.trim()
        if (!content?.startsWith('gitdir:')) return null
        def gitPath = content.substring(7).trim()
        def gitDir = new File(gitPath)
        return gitDir.isAbsolute() ? gitDir : new File(dotGit.parentFile, gitPath).canonicalFile
    }

    /**
     * Returns list of directories to watch for git state changes.
     * For worktrees, returns both worktree gitdir AND main repo gitdir (via commondir).
     * For regular repos, returns just the git directory.
     */
    static List<File> getDirectoriesToWatch(File dotGit) {
        File gitDir = resolveDotGit(dotGit)
        if (gitDir == null || !gitDir.isDirectory()) return []

        def commonDirFile = new File(gitDir, "commondir")
        if (commonDirFile.exists()) {
            def commonDir = new File(gitDir, commonDirFile.text.trim()).canonicalFile
            return [gitDir, commonDir]
        }
        return [gitDir]
    }

    private static Repository openWorktree(File worktreeDir, File gitFile) {
        def content = gitFile.text.trim()
        if (!content.startsWith("gitdir:")) {
            throw new IllegalArgumentException("Invalid .git file format: ${gitFile}")
        }

        def gitDirPath = content.substring("gitdir:".length()).trim()
        def worktreeGitDir = new File(gitDirPath)
        if (!worktreeGitDir.isAbsolute()) {
            worktreeGitDir = new File(worktreeDir, gitDirPath).canonicalFile
        }

        // Find the main repo's git directory via commondir
        def commonDirFile = new File(worktreeGitDir, "commondir")
        if (!commonDirFile.exists()) {
            // Fallback: if no commondir, just use the worktree gitdir
            return new FileRepositoryBuilder()
                    .setGitDir(worktreeGitDir)
                    .setWorkTree(worktreeDir)
                    .readEnvironment()
                    .build()
        }

        def commonDirPath = commonDirFile.text.trim()
        def mainGitDir = new File(worktreeGitDir, commonDirPath).canonicalFile

        // Open the MAIN repo for object resolution, but set workTree to the worktree
        return new FileRepositoryBuilder()
                .setGitDir(mainGitDir)
                .setWorkTree(worktreeDir)
                .readEnvironment()
                .build()
    }
}
