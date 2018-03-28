package com.gorylenko

import org.ajoberstar.grgit.Grgit

class BuildUserEmailProperty extends Closure<String> {

    BuildUserEmailProperty() {
        super(null)
    }

    String doCall(Grgit repo) {
        String email = repo.repository.jgit.repository.config.getString("user", null, "email")
        return email ?: ''
    }
}
