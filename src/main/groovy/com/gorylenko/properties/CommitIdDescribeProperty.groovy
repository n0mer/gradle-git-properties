package com.gorylenko.properties

import org.ajoberstar.grgit.Grgit

class CommitIdDescribeProperty extends AbstractGitProperty {

    String doCall(Grgit repo) {
        return isEmpty(repo) ? '' : commitIdDescribe(repo, '-dirty')
    }

    private String commitIdDescribe(Grgit repo, String dirtyMark) {

        String describe
        try {
            describe = repo.describe()
            if (describe == null && isShallowClone(repo)) {
                // jgit 5 will return null while jgit 4 will throw exception on shallow clone
                // shallow clone, use the fallback value "<commit id>"
                describe = repo.head().abbreviatedId
            }
        } catch (org.eclipse.jgit.api.errors.JGitInternalException e) {
            if (isShallowClone(repo)) {
                // shallow clone, use the fallback value "<commit id>"
                describe = repo.head().abbreviatedId
            } else {
                throw e;
            }
        }

        if (describe && !repo.status().clean) {
            describe += dirtyMark
        }
        return describe ?: ''
    }

    boolean isShallowClone(Grgit repo) {
        File shallow =  new File(repo.repository.rootDir, ".git/shallow")
        return shallow.exists()
    }

}
