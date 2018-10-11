package com.gorylenko.properties

import org.ajoberstar.grgit.Grgit

class ClosestTagNameProperty extends AbstractGitProperty {

    String doCall(Grgit repo) {
        return isEmpty(repo) ? '' : closestTagName(repo)
    }

    String closestTagName(Grgit repo) {
        try {

            String describe = repo.describe(longDescr: true)
            if (describe) {
                // remove commit ID
                describe = describe.substring(0, describe.lastIndexOf('-'))
                // remove commit number
                describe = describe.substring(0, describe.lastIndexOf('-'))
            }
            return describe ?: ''

        } catch (org.eclipse.jgit.api.errors.JGitInternalException e) {
            if (isShallowClone(repo)) {
                // shallow clone, use value ""
                return ''
            } else {
                throw e;
            }
        }
    }

    boolean isShallowClone(Grgit repo) {
        File shallow =  new File(repo.repository.rootDir, ".git/shallow")
        return shallow.exists()
    }
}
