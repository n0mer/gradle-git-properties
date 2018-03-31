package com.gorylenko

import org.ajoberstar.grgit.Grgit
import org.eclipse.jgit.api.errors.NoHeadException

class TotalCommitCountProperty extends AbstractGitProperty {

    String doCall(Grgit repo) {
        return isEmpty(repo) ? '0' : repo.log().size().toString()
    }
}
