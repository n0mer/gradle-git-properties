package com.gorylenko.jgit

import groovy.transform.Immutable

/**
 * Represents a person (author/committer) in Git.
 */
@Immutable
class GitPerson {
    String name
    String email
}
