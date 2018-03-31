package com.gorylenko

import org.ajoberstar.grgit.Grgit

class ClosestTagNameProperty extends AbstractGitProperty {

    String doCall(Grgit repo) {
        return isEmpty(repo) ? '' : closestTagName(repo)
    }

    String closestTagName(Grgit repo) {
        String describe = repo.describe(longDescr: true)
        if (describe) {
            // remove commit ID
            describe = describe.substring(0, describe.lastIndexOf('-'))
            // remove commit number
            describe = describe.substring(0, describe.lastIndexOf('-'))
        }
        return describe ?: ''
    }
}
