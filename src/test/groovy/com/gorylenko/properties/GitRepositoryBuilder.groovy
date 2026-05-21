package com.gorylenko.properties

import com.gorylenko.jgit.JGitTestHelper
import org.eclipse.jgit.revwalk.RevCommit

import groovy.lang.Closure

/**
 * Test helper for building Git repositories using pure JGit.
 * Migrated from Grgit to JGit for compatibility with JGit 7.x.
 */
class GitRepositoryBuilder implements AutoCloseable {
    private File workingDirectory
    private JGitTestHelper helper

    GitRepositoryBuilder(File workingDirectory) {
        this.workingDirectory = workingDirectory
        if (new File(workingDirectory, '.git').exists()) {
            this.helper = JGitTestHelper.open(workingDirectory)
        } else {
            this.helper = JGitTestHelper.create(workingDirectory)
        }
    }

    RevCommit commitFile(String name, String content, String message) {
        return helper.commitFile(name, content, message)
    }

    void addBranch(String name) {
        helper.createBranch(name)
    }

    void addTag(String name) {
        // Use annotated tags - git describe by default only looks at annotated tags
        helper.createTag(name, "Tag ${name}")
    }

    void addBranchAndCheckout(String name) {
        helper.createBranchAndCheckout(name)
    }

    void setConfigString(final String section, final String subsection, final String name, final String value) {
        helper.setConfig(section, subsection, name, value)
    }

    void close() {
        helper?.close()
    }

    static void setupProjectDir(File projectDir, Closure closure, GitRepositoryBuilder builder = null) {
        GitRepositoryBuilder gitRepoBuilder = builder ?: new GitRepositoryBuilder(projectDir)
        try {
            closure(gitRepoBuilder)
        } finally {
            gitRepoBuilder.close()
        }
    }
}
