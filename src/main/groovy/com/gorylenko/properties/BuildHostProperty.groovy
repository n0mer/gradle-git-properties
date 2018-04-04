package com.gorylenko.properties

import java.net.InetAddress
import java.net.UnknownHostException

import org.ajoberstar.grgit.Grgit

class BuildHostProperty extends Closure<String> {

    BuildHostProperty() {
        super(null)
    }

    String doCall(Grgit repo) {
        String buildHost = null
        try {
          buildHost = InetAddress.localHost.hostName
        } catch (Exception e) {
        }
        return buildHost ?: ''
    }
}
