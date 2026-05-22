package com.gorylenko.jgit

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

/**
 * Test helper for creating and manipulating Git repositories using pure JGit.
 * This is intended for testing the JGit-based GitFacade without Grgit dependency.
 */
class JGitTestHelper implements AutoCloseable {

    private final Repository repository
    private final Git git
    private final File workDir

    /** Default author for commits */
    static final String DEFAULT_AUTHOR_NAME = "Test User"
    static final String DEFAULT_AUTHOR_EMAIL = "testuser@example.com"

    private JGitTestHelper(Repository repository, Git git, File workDir) {
        this.repository = repository
        this.git = git
        this.workDir = workDir
    }

    /**
     * Create a new Git repository in the specified directory.
     */
    static JGitTestHelper create(File directory) {
        Git git = Git.init()
                .setDirectory(directory)
                .call()
        return new JGitTestHelper(git.repository, git, directory)
    }

    /**
     * Open an existing repository (e.g., a worktree).
     */
    static JGitTestHelper open(File directory) {
        def builder = new FileRepositoryBuilder()
                .setGitDir(new File(directory, ".git"))
                .readEnvironment()
        def repository = builder.build()
        def git = new Git(repository)
        return new JGitTestHelper(repository, git, directory)
    }

    /**
     * Open a worktree directory. This handles the .git file that points to the actual git dir.
     */
    static JGitTestHelper openWorktree(File worktreeDir) {
        def gitFile = new File(worktreeDir, ".git")

        if (gitFile.isFile()) {
            // Parse .git file to find actual git dir
            def content = gitFile.text.trim()
            if (content.startsWith("gitdir:")) {
                def gitDirPath = content.substring("gitdir:".length()).trim()
                def gitDir = new File(gitDirPath)
                if (!gitDir.isAbsolute()) {
                    gitDir = new File(worktreeDir, gitDirPath)
                }

                def builder = new FileRepositoryBuilder()
                        .setGitDir(gitDir)
                        .setWorkTree(worktreeDir)
                        .readEnvironment()
                def repository = builder.build()
                def git = new Git(repository)
                return new JGitTestHelper(repository, git, worktreeDir)
            }
        }

        // Fallback to regular open
        return open(worktreeDir)
    }

    /**
     * Get the underlying JGit Repository.
     */
    Repository getRepository() {
        return repository
    }

    /**
     * Get the underlying JGit Git command interface.
     */
    Git getGit() {
        return git
    }

    /**
     * Create a file and commit it with default author.
     */
    RevCommit commitFile(String filename, String content, String message) {
        return commitFile(filename, content, message, DEFAULT_AUTHOR_NAME, DEFAULT_AUTHOR_EMAIL)
    }

    /**
     * Create a file and commit it with specified author.
     */
    RevCommit commitFile(String filename, String content, String message, String authorName, String authorEmail) {
        // Write file
        def file = new File(workDir, filename)
        file.parentFile?.mkdirs()
        file.text = content

        // Add to index
        git.add()
                .addFilepattern(filename)
                .call()

        // Commit
        def author = new PersonIdent(authorName, authorEmail)
        return git.commit()
                .setMessage(message)
                .setAuthor(author)
                .setCommitter(author)
                .call()
    }

    /**
     * Create a branch at HEAD.
     */
    void createBranch(String name) {
        git.branchCreate()
                .setName(name)
                .call()
    }

    /**
     * Create a branch and checkout to it.
     */
    void createBranchAndCheckout(String name) {
        git.checkout()
                .setCreateBranch(true)
                .setName(name)
                .call()
    }

    /**
     * Create an annotated tag at HEAD.
     */
    void createTag(String name, String message) {
        git.tag()
                .setName(name)
                .setMessage(message)
                .setAnnotated(true)
                .call()
    }

    /**
     * Create a lightweight tag at HEAD.
     */
    void createLightweightTag(String name) {
        git.tag()
                .setName(name)
                .setAnnotated(false)
                .call()
    }

    /**
     * Create a worktree at the specified directory on a new branch.
     *
     * Note: JGit doesn't have native worktree support, so we shell out to git command.
     * This is acceptable for test infrastructure.
     */
    void createWorktree(File worktreeDir, String branchName) {
        def command = ["git", "worktree", "add", "-b", branchName, worktreeDir.absolutePath]
        def process = command.execute(null, workDir)
        def stdout = new StringBuilder()
        def stderr = new StringBuilder()
        process.consumeProcessOutput(stdout, stderr)
        def exitCode = process.waitFor()

        if (exitCode != 0) {
            throw new RuntimeException("Failed to create worktree: ${stderr}")
        }
    }

    /**
     * Set a config value.
     */
    void setConfig(String section, String subsection, String name, String value) {
        repository.config.setString(section, subsection, name, value)
        repository.config.save()
    }

    /**
     * Close the repository and release resources.
     */
    @Override
    void close() {
        git?.close()
        repository?.close()
    }
}
