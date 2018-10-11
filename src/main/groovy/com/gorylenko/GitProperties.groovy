package com.gorylenko


import java.text.SimpleDateFormat

import org.ajoberstar.grgit.Grgit

import com.gorylenko.properties.BranchProperty
import com.gorylenko.properties.BuildHostProperty
import com.gorylenko.properties.BuildTimeProperty
import com.gorylenko.properties.BuildUserEmailProperty
import com.gorylenko.properties.BuildUserNameProperty
import com.gorylenko.properties.BuildVersionProperty
import com.gorylenko.properties.ClosestTagCommitCountProperty
import com.gorylenko.properties.ClosestTagNameProperty
import com.gorylenko.properties.CommitIdAbbrevProperty
import com.gorylenko.properties.CommitIdDescribeProperty
import com.gorylenko.properties.CommitIdProperty
import com.gorylenko.properties.CommitMessageFullProperty
import com.gorylenko.properties.CommitMessageShortProperty
import com.gorylenko.properties.CommitTimeProperty
import com.gorylenko.properties.CommitUserEmailProperty
import com.gorylenko.properties.CommitUserNameProperty
import com.gorylenko.properties.DirtyProperty
import com.gorylenko.properties.RemoteOriginUrlProperty
import com.gorylenko.properties.TagsProperty
import com.gorylenko.properties.TotalCommitCountProperty

import java.io.File
import java.util.List
import java.util.Map


class GitProperties {

    private static final String KEY_GIT_BRANCH = "git.branch"
    private static final String KEY_GIT_COMMIT_ID = "git.commit.id"
    private static final String KEY_GIT_COMMIT_ID_ABBREVIATED = "git.commit.id.abbrev"
    private static final String KEY_GIT_COMMIT_USER_NAME = "git.commit.user.name"
    private static final String KEY_GIT_COMMIT_USER_EMAIL = "git.commit.user.email"
    private static final String KEY_GIT_COMMIT_SHORT_MESSAGE = "git.commit.message.short"
    private static final String KEY_GIT_COMMIT_FULL_MESSAGE = "git.commit.message.full"
    private static final String KEY_GIT_COMMIT_TIME = "git.commit.time"
    private static final String KEY_GIT_COMMIT_ID_DESCRIBE = "git.commit.id.describe"
    private static final String KEY_GIT_REMOTE_ORIGIN_URL = "git.remote.origin.url"
    private static final String KEY_GIT_TAGS = "git.tags"
    private static final String KEY_GIT_CLOSEST_TAG_NAME = "git.closest.tag.name"
    private static final String KEY_GIT_CLOSEST_TAG_COMMIT_COUNT = "git.closest.tag.commit.count"
    private static final String KEY_GIT_TOTAL_COMMIT_COUNT = "git.total.commit.count"
    private static final String KEY_GIT_DIRTY = "git.dirty"
    private static final String KEY_GIT_BUILD_USER_NAME = "git.build.user.name"
    private static final String KEY_GIT_BUILD_USER_EMAIL = "git.build.user.email"
    private static final String KEY_GIT_BUILD_TIME = "git.build.time"
    private static final String KEY_GIT_BUILD_VERSION = "git.build.version"
    private static final String KEY_GIT_BUILD_HOST = "git.build.host"

    public Map<String, String> generate(File dotGitDirectory, List<String> keys, String dateFormat, String dateFormatTimeZone, String branch,
        Object buildVersion, Map<String, Object> customProperties) {

        // Find standard properties and custom properties to be generated

        Map properties = getStandardPropertiesMap(dateFormat, dateFormatTimeZone, branch, buildVersion).subMap(keys)
        if (customProperties) {
            properties.putAll(customProperties)
        }

        // Evaluate property values

        def result = [:]
        def repo = Grgit.open(dir: dotGitDirectory)
        try {
            properties.each{ k, v -> result.put(k, v instanceof Closure ? v.call(repo).toString() : v.toString() ) }
        } finally {
            repo.close()
        }

        return result
    }

    public static List getStandardProperties() {
        return getStandardPropertiesMap(null, null, null, null).keySet() as List
    }

    private static Map getStandardPropertiesMap(String dateFormat, String dateFormatTimeZone, String branch, Object buildVersion) {

        def map = [(KEY_GIT_BRANCH)                     : new BranchProperty(branch)
                   , (KEY_GIT_COMMIT_ID)                : new CommitIdProperty()
                   , (KEY_GIT_COMMIT_ID_ABBREVIATED)    : new CommitIdAbbrevProperty()
                   , (KEY_GIT_COMMIT_USER_NAME)         : new CommitUserNameProperty()
                   , (KEY_GIT_COMMIT_USER_EMAIL)        : new CommitUserEmailProperty()
                   , (KEY_GIT_COMMIT_SHORT_MESSAGE)     : new CommitMessageShortProperty()
                   , (KEY_GIT_COMMIT_FULL_MESSAGE)      : new CommitMessageFullProperty()
                   , (KEY_GIT_COMMIT_TIME)              : new CommitTimeProperty(dateFormat, dateFormatTimeZone)
                   , (KEY_GIT_COMMIT_ID_DESCRIBE)       : new CommitIdDescribeProperty()
                   , (KEY_GIT_REMOTE_ORIGIN_URL)        : new RemoteOriginUrlProperty()
                   , (KEY_GIT_TAGS)                     : new TagsProperty()
                   , (KEY_GIT_CLOSEST_TAG_NAME)         : new ClosestTagNameProperty()
                   , (KEY_GIT_CLOSEST_TAG_COMMIT_COUNT) : new ClosestTagCommitCountProperty()
                   , (KEY_GIT_TOTAL_COMMIT_COUNT)       : new TotalCommitCountProperty()
                   , (KEY_GIT_DIRTY)                    : new DirtyProperty()
                   , (KEY_GIT_BUILD_USER_NAME)          : new BuildUserNameProperty()
                   , (KEY_GIT_BUILD_USER_EMAIL)         : new BuildUserEmailProperty()
                   , (KEY_GIT_BUILD_TIME)               : new BuildTimeProperty(dateFormat, dateFormatTimeZone)
                   , (KEY_GIT_BUILD_VERSION)            : new BuildVersionProperty(buildVersion)
                   , (KEY_GIT_BUILD_HOST)               : new BuildHostProperty()]

        return map
    }
}
