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
        } catch (org.eclipse.jgit.api.errors.JGitInternalException e) {
            if (e.getCause() instanceof org.eclipse.jgit.errors.MissingObjectException) {
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

}
