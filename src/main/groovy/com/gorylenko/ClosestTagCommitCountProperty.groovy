package com.gorylenko

import org.ajoberstar.grgit.Grgit

class ClosestTagCommitCountProperty extends Closure<String>{

    ClosestTagCommitCountProperty() {
        super(null)
    }

    String doCall(Grgit repo) {
        String describe = repo.describe(longDescr: true)
        if (describe) {
            // remove commit ID
            describe = describe.substring(0, describe.lastIndexOf('-'))
            describe = describe.substring(describe.lastIndexOf('-') + 1)
        }
        return describe ?: ''
    }
}
