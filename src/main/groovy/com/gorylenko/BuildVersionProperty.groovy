package com.gorylenko

import org.ajoberstar.grgit.Grgit

class BuildVersionProperty extends Closure<String> {
    Object version
    BuildVersionProperty(Object version) {
        super(null)
        this.version = version
    }

    String doCall(Grgit repo) {
        return "$version"
    }
}
