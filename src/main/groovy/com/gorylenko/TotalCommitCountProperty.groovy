package com.gorylenko

import org.ajoberstar.grgit.Grgit
import org.eclipse.jgit.api.errors.NoHeadException

class TotalCommitCountProperty extends Closure<String>{

    TotalCommitCountProperty() {
        super(null)
    }

    String doCall(Grgit repo) {
        int count
        try {
            count = repo.log().size()
        } catch (NoHeadException e) {
            // empty repo
            count = 0;
        }
        return count.toString()
    }
}
