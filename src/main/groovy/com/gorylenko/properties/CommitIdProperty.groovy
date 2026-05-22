package com.gorylenko.properties

import com.gorylenko.jgit.GitFacade

class CommitIdProperty extends AbstractGitProperty {

    String doCall(GitFacade facade) {
        return isEmpty(facade) ? '' : facade.head().id
    }
}
