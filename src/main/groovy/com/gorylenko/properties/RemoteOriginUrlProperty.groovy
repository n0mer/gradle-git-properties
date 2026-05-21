package com.gorylenko.properties

import com.gorylenko.jgit.GitFacade

class RemoteOriginUrlProperty extends AbstractGitProperty {

    String doCall(GitFacade facade) {
        String url = facade.getConfig("remote", "origin", "url")
        url = removeUserInfo(url) ?: ''
        return url
    }

    private String removeUserInfo(String url) {
        String result = url
        if (url) {
            try {
                URL u = new URL(url)
                if (u.userInfo) {
                    // remove user info from url
                    result = new URL(u.protocol, u.host, u.port, u.file).toString()
                }
            } catch (Exception e) {
                // cannot parse, just skip it
            }
        }
        return result
    }

}
