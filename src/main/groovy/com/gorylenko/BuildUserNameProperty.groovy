package com.gorylenko

import org.ajoberstar.grgit.Grgit

class BuildUserNameProperty extends Closure<String> {

    BuildUserNameProperty() {
        super(null)
    }

    String doCall(Grgit repo) {
        String username = repo.repository.jgit.repository.config.getString("user", null, "name")
        return username ?: ''
    }
}
