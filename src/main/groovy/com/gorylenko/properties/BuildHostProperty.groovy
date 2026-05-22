package com.gorylenko.properties

import java.net.InetAddress

import com.gorylenko.jgit.GitFacade

class BuildHostProperty extends Closure<String> {

    BuildHostProperty() {
        super(null)
    }

    String doCall(GitFacade facade) {
        return getHostName()
    }

    private String getHostName() {
        String buildHost = null
        try {
          buildHost = InetAddress.localHost.hostName
        } catch (Exception e) {
        }
        return buildHost ?: ''
    }
}
