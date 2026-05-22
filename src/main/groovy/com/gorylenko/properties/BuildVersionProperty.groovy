package com.gorylenko.properties

import com.gorylenko.jgit.GitFacade

class BuildVersionProperty extends Closure<String> {
    Object version
    BuildVersionProperty(Object version) {
        super(null)
        this.version = version
    }

    String doCall(GitFacade facade) {
        return "$version"
    }
}
