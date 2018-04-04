package com.gorylenko.properties

import org.ajoberstar.grgit.Grgit

class ClosestTagCommitCountProperty extends AbstractGitProperty {

    String doCall(Grgit repo) {
        return isEmpty(repo) ? '' : closestTagCommitCount(repo)
    }

    String closestTagCommitCount(Grgit repo) {
        String describe = repo.describe(longDescr: true)
        if (describe) {
            // remove commit ID
            describe = describe.substring(0, describe.lastIndexOf('-'))
            describe = describe.substring(describe.lastIndexOf('-') + 1)
        }
        return describe ?: ''
    }
}
