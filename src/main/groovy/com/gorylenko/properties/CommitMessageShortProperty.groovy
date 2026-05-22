package com.gorylenko.properties

import com.gorylenko.jgit.GitFacade

class CommitMessageShortProperty extends AbstractGitProperty {

    String doCall(GitFacade facade) {
        return isEmpty(facade) ? '' : facade.head().shortMessage
    }
}
