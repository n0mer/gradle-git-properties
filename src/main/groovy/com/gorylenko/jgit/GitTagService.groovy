package com.gorylenko.jgit

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk

import java.time.Instant

/**
 * Service for Git tag operations.
 */
class GitTagService {

    private final Repository repository

    GitTagService(Repository repository) {
        this.repository = repository
    }

    /**
     * Returns a list of all tags in the repository.
     *
     * @return List of GitTag objects
     */
    List<GitTag> list() {
        def git = new Git(repository)
        try {
            def refs = git.tagList().call()
            return refs.collect { ref -> toGitTag(ref) }
        } finally {
            git.close()
        }
    }

    private GitTag toGitTag(Ref ref) {
        def tagName = ref.name.replaceFirst("^refs/tags/", "")

        RevWalk revWalk = new RevWalk(repository)
        try {
            def objectId = ref.peeledObjectId ?: ref.objectId
            RevCommit revCommit = revWalk.parseCommit(objectId)
            def commit = toGitCommit(revCommit)
            return new GitTag(name: tagName, commit: commit)
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
}
