package com.gorylenko.properties

import org.ajoberstar.grgit.Grgit

class CommitMessageFullProperty extends AbstractGitProperty {

    String doCall(Grgit repo) {
        return isEmpty(repo) ? '' : repo.head().fullMessage
    }
}
