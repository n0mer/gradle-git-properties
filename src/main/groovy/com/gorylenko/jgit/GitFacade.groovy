package com.gorylenko.jgit

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.JGitInternalException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk

import org.eclipse.jgit.lib.ObjectId

import java.time.Instant

/**
 * High-level facade for Git operations using JGit.
 * This is the main API for accessing git repository information.
 */
class GitFacade implements AutoCloseable {

    private final Repository repository
    private final GitBranch branchService
    private final GitTagService tagService
    private final File worktreeGitDir  // non-null if this is a worktree

    private GitFacade(Repository repository, File worktreeGitDir = null) {
        this.repository = repository
        this.worktreeGitDir = worktreeGitDir
        this.branchService = new GitBranch(repository, worktreeGitDir)
        this.tagService = new GitTagService(repository)
    }

    /**
     * Returns the branch service for branch operations.
     */
    GitBranch getBranch() {
        return branchService
    }

    /**
     * Returns the tag service for tag operations.
     */
    GitTagService getTag() {
        return tagService
    }

    /**
     * Returns the current status of the working tree.
     *
     * @return GitStatus with clean/dirty state
     */
    GitStatus status() {
        def git = new Git(repository)
        try {
            def jgitStatus = git.status().call()
            // Clean means no staged, modified, or untracked files
            // This matches Grgit's behavior:
            // - staged: added, changed, removed
            // - unstaged: untracked, modified, missing
            def isClean = jgitStatus.added.isEmpty() &&
                    jgitStatus.changed.isEmpty() &&
                    jgitStatus.removed.isEmpty() &&
                    jgitStatus.untracked.isEmpty() &&
                    jgitStatus.modified.isEmpty() &&
                    jgitStatus.missing.isEmpty() &&
                    jgitStatus.conflicting.isEmpty()
            return new GitStatus(clean: isClean)
        } finally {
            git.close()
        }
    }

    /**
     * Returns the output of git describe.
     *
     * @param options Optional map with: tags (boolean) - include lightweight tags,
     *                longDescr (boolean) - always output long format
     * @return describe output string, or null if no tags found
     */
    String describe(Map options = [:]) {
        def git = new Git(repository)
        try {
            def cmd = git.describe()
            if (options.tags) {
                cmd.setTags(true)
            }
            if (options.longDescr) {
                cmd.setLong(true)
            }
            return cmd.call()
        } catch (JGitInternalException e) {
            // No tags found
            return null
        } finally {
            git.close()
        }
    }

    /**
     * Opens a Git repository from the specified directory.
     *
     * @param directory the working directory (can be regular repo or worktree)
     * @return GitFacade instance
     */
    static GitFacade open(File directory) {
        def worktreeGitDir = RepositoryFactory.detectWorktreeGitDir(directory)
        def repository = RepositoryFactory.open(directory)
        return new GitFacade(repository, worktreeGitDir)
    }

    /**
     * Checks if the repository is empty (has no commits).
     *
     * @return true if no commits exist, false otherwise
     */
    boolean isEmpty() {
        def head = resolveHead()
        return head == null
    }

    /**
     * Resolves HEAD, handling worktree case where HEAD is in a different location.
     */
    private ObjectId resolveHead() {
        if (worktreeGitDir != null) {
            // For worktrees, read HEAD from the worktree gitdir
            def headFile = new File(worktreeGitDir, "HEAD")
            if (headFile.exists()) {
                def headContent = headFile.text.trim()
                if (headContent.startsWith("ref: ")) {
                    // Symbolic ref like "ref: refs/heads/feature-branch"
                    def refName = headContent.substring("ref: ".length())
                    return repository.resolve(refName)
                }
                // Direct SHA
                return ObjectId.fromString(headContent)
            }
        }
        return repository.resolve(Constants.HEAD)
    }

    /**
     * Returns the HEAD commit, or null if the repository is empty.
     *
     * @return GitCommit for HEAD, or null
     */
    GitCommit head() {
        def headId = resolveHead()
        if (headId == null) {
            return null
        }

        RevWalk revWalk = new RevWalk(repository)
        try {
            RevCommit revCommit = revWalk.parseCommit(headId)
            return toGitCommit(revCommit)
        } finally {
            revWalk.close()
        }
    }

    private GitCommit toGitCommit(RevCommit revCommit) {
        def authorIdent = revCommit.authorIdent
        def author = new GitPerson(
                name: authorIdent.name,
                email: authorIdent.emailAddress
        )

        def abbreviatedId = repository.newObjectReader().abbreviate(revCommit.id, 7).name()

        return new GitCommit(
                id: revCommit.name,
                abbreviatedId: abbreviatedId,
                author: author,
                dateTime: Instant.ofEpochSecond(revCommit.commitTime),
                shortMessage: revCommit.shortMessage,
                fullMessage: revCommit.fullMessage
        )
    }

    /**
     * Returns the raw JGit Repository for escape hatch operations.
     * Use this when the facade doesn't expose needed functionality.
     *
     * @return The underlying JGit Repository
     */
    Repository getJgit() {
        return repository
    }

    /**
     * Returns a JGit Git object for escape hatch command execution.
     * Use this when the facade doesn't expose needed functionality.
     * Note: The caller is responsible for closing this Git object.
     *
     * @return A new JGit Git command interface
     */
    Git getJgitCommands() {
        return new Git(repository)
    }

    /**
     * Reads a git config value (no subsection).
     *
     * @param section the config section (e.g., "user", "core")
     * @param name the config key name (e.g., "name", "email")
     * @return the config value, or null if not set
     */
    String getConfig(String section, String name) {
        return repository.config.getString(section, null, name)
    }

    /**
     * Reads a git config value with subsection.
     *
     * @param section the config section (e.g., "remote", "branch")
     * @param subsection the subsection (e.g., "origin", "master")
     * @param name the config key name (e.g., "url", "fetch")
     * @return the config value, or null if not set
     */
    String getConfig(String section, String subsection, String name) {
        return repository.config.getString(section, subsection, name)
    }

    /**
     * Returns the commit log (history) starting from HEAD.
     *
     * @param options Optional map with: maxCommits (int) - limit number of commits returned
     * @return List of GitCommit objects, most recent first. Empty list if no commits.
     */
    List<GitCommit> log(Map options = [:]) {
        def headId = resolveHead()
        if (headId == null) {
            return []
        }

        def git = new Git(repository)
        try {
            def logCmd = git.log()
            if (options.maxCommits) {
                logCmd.setMaxCount(options.maxCommits as int)
            }
            def commits = []
            for (RevCommit revCommit : logCmd.call()) {
                commits.add(toGitCommit(revCommit))
            }
            return commits
        } finally {
            git.close()
        }
    }

    /**
     * Closes the underlying repository and releases resources.
     */
    @Override
    void close() {
        repository?.close()
    }
}
