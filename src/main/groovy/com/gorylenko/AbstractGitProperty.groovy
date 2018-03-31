package com.gorylenko

import org.ajoberstar.grgit.Grgit

class AbstractGitProperty extends Closure<String> {

    AbstractGitProperty() {
        super(null)
    }

    boolean isEmpty(Grgit repo) {
        return ! repo.repository.jgit.repository.resolve('HEAD')
    }
}
