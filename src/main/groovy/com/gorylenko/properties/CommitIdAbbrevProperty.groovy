package com.gorylenko.properties

import com.gorylenko.jgit.GitFacade

class CommitIdAbbrevProperty extends AbstractGitProperty {

    String doCall(GitFacade facade) {
        return isEmpty(facade) ? '' : facade.head().abbreviatedId
    }
}
