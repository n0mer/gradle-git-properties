package com.gorylenko.jgit

import org.eclipse.jgit.lib.Repository

/**
 * Service for Git branch operations.
 */
class GitBranch {

    private final Repository repository
    private final File worktreeGitDir  // non-null if this is a worktree

    GitBranch(Repository repository, File worktreeGitDir = null) {
        this.repository = repository
        this.worktreeGitDir = worktreeGitDir
    }

    /**
     * Returns information about the current (checked out) branch.
     *
     * @return GitBranchInfo for the current branch
     */
    GitBranchInfo current() {
        def branchName = getBranchName()
        return new GitBranchInfo(name: branchName)
    }

    private String getBranchName() {
        if (worktreeGitDir != null) {
            // For worktrees, read HEAD from the worktree gitdir
            def headFile = new File(worktreeGitDir, "HEAD")
            if (headFile.exists()) {
                def headContent = headFile.text.trim()
                if (headContent.startsWith("ref: refs/heads/")) {
                    return headContent.substring("ref: refs/heads/".length())
                }
                if (headContent.startsWith("ref: ")) {
                    return headContent.substring("ref: ".length())
                }
                // Detached HEAD - return "HEAD" (Grgit returns null, but we return "HEAD" to avoid NPE in callers)
                return "HEAD"
            }
        }

        // Check if HEAD is detached (not pointing to a branch)
        def headRef = repository.exactRef("HEAD")
        if (headRef == null || !headRef.isSymbolic()) {
            // Detached HEAD - return "HEAD" (Grgit returns null, but we return "HEAD" to avoid NPE in callers)
            return "HEAD"
        }

        return repository.branch
    }
}
