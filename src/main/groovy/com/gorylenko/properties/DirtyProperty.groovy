package com.gorylenko.properties

import com.gorylenko.jgit.GitFacade

class DirtyProperty extends AbstractGitProperty {

    String doCall(GitFacade facade) {
        return !facade.status().clean
    }
}
