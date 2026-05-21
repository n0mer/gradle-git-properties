package com.gorylenko.properties

import com.gorylenko.jgit.GitFacade

class CommitUserEmailProperty extends AbstractGitProperty {

    String doCall(GitFacade facade) {
        return isEmpty(facade) ? '' : facade.head().author.email
    }
}
