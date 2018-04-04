package com.gorylenko.properties

import java.net.URI
import java.net.URISyntaxException
import java.text.SimpleDateFormat
import java.util.Collection
import java.util.regex.Pattern

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Tag

class RemoteOriginUrlProperty extends AbstractGitProperty {

    String doCall(Grgit repo) {
        String url = repo.repository.jgit.repository.config.getString("remote", "origin", "url")
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
