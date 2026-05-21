package com.gorylenko.properties

import com.gorylenko.jgit.GitFacade

class AbstractGitProperty extends Closure<String> {

    AbstractGitProperty() {
        super(null)
    }

    boolean isEmpty(GitFacade facade) {
        return facade.isEmpty()
    }
}
