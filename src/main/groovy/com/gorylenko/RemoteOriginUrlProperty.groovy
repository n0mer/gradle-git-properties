/**
 * Below code is modified from
 * https://github.com/ktoso/maven-git-commit-id-plugin/blob/master/src/main/java/pl/project13/maven/git/JGitProvider.java
 */
package com.gorylenko

import java.net.URI
import java.net.URISyntaxException
import java.text.SimpleDateFormat
import java.util.Collection
import java.util.regex.Pattern

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Tag
import org.apache.http.client.utils.URIBuilder

class RemoteOriginUrlProperty extends Closure<String>{
    static final Pattern GIT_SCP_FORMAT = Pattern.compile("^([a-zA-Z0-9_.+-])+@(.*)|^\\[([^\\]])+\\]:(.*)|^file:/{2,3}(.*)");

    RemoteOriginUrlProperty() {
        super(null)
    }

    String doCall(Grgit repo) {
        String url = repo.repository.jgit.repository.config.getString("remote", "origin", "url")
        url = stripCredentialsFromOriginUrl(url) ?: ''
        return url
    }


    private String stripCredentialsFromOriginUrl(String gitRemoteString) {

        // The URL might be null if the repo hasn't set a remote
        if (gitRemoteString == null) {
            return gitRemoteString;
        }

        // Remotes using ssh connection strings in the 'git@github' format aren't
        // proper URIs and won't parse . Plus since you should be using SSH keys,
        // credentials like are not in the URL.
        if (GIT_SCP_FORMAT.matcher(gitRemoteString).matches()) {
            return gitRemoteString;
        }

        // At this point, we should have a properly formatted URL
        URI original = new URI(gitRemoteString);
        String userInfoString = original.getUserInfo();
        if (null == userInfoString) {
            return gitRemoteString;
        }

        URIBuilder b = new URIBuilder(gitRemoteString);
        String[] userInfo = userInfoString.split(":");
        // Build a new URL from the original URL, but nulling out the password
        // component of the userinfo. We keep the username so that ssh uris such
        // ssh://git@github.com will retain 'git@'.
        b.setUserInfo(userInfo[0]);
        return b.build().toString();
    }
}
