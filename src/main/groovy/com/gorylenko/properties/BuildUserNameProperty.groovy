package com.gorylenko.properties

import com.gorylenko.jgit.GitFacade

class BuildUserNameProperty extends Closure<String> {

    BuildUserNameProperty() {
        super(null)
    }

    String doCall(GitFacade facade) {
        String username = facade.getConfig("user", "name")
        return username ?: ''
    }
}
