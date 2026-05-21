package com.gorylenko.jgit

import groovy.transform.Immutable

/**
 * Represents a Git tag.
 */
@Immutable
class GitTag {
    String name
    GitCommit commit
}
