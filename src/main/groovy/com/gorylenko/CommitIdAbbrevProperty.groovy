package com.gorylenko

import org.ajoberstar.grgit.Grgit

class CommitIdAbbrevProperty extends AbstractGitProperty {

    String doCall(Grgit repo) {
        return isEmpty(repo) ? '' : repo.head().abbreviatedId
    }
}
