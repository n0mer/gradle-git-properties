package com.gorylenko.jgit

import groovy.transform.Immutable

import java.time.Instant

/**
 * Represents a Git commit with its metadata.
 */
@Immutable
class GitCommit {
    String id
    String abbreviatedId
    GitPerson author
    Instant dateTime
    String shortMessage
    String fullMessage
}
