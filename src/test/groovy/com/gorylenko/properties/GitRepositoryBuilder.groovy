package com.gorylenko.properties

import java.io.File
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Person

import groovy.lang.Closure

class GitRepositoryBuilder implements AutoCloseable {
    private File workingDirectory
    private Grgit grgit

    Person user = new Person('testuser', 'testuser@example.com')

    GitRepositoryBuilder(File workingDirectory) {

        this.workingDirectory = workingDirectory
        if (new File(workingDirectory, '.git').exists()) {
            this.grgit = Grgit.open(dir: workingDirectory)
        } else {
            this.grgit = Grgit.init(dir: workingDirectory)
        }

    }

    Commit commitFile(String name, String content, String message) {
        new File(workingDirectory, name).text = content
        grgit.add(patterns: [name])
        Commit commit = grgit.commit(message: message, author: user, committer: user)
        return commit
    }

    void addBranch(String name) {
        grgit.branch.add(name: name)
    }

    void addTag(String name) {
        grgit.tag.add (name: name)
    }

    void addBranchAndCheckout(String name) {
        grgit.checkout(branch: name, createBranch: true)
    }

    void setConfigString(final String section, final String subsection, final String name, final String value) {
        grgit.repository.jgit.repository.config.setString(section, subsection, name, value)
        grgit.repository.jgit.repository.config.save()
    }

    void close() {
        grgit?.close()
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
