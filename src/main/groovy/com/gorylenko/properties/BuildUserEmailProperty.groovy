package com.gorylenko.properties

import com.gorylenko.jgit.GitFacade

class BuildUserEmailProperty extends Closure<String> {

    BuildUserEmailProperty() {
        super(null)
    }

    String doCall(GitFacade facade) {
        String email = facade.getConfig("user", "email")
        return email ?: ''
    }
}
