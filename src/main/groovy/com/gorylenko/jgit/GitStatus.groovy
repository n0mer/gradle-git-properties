package com.gorylenko.jgit

import groovy.transform.Immutable

/**
 * Represents the status of a Git working tree.
 */
@Immutable
class GitStatus {
    /** True if the working tree has no uncommitted changes (staged or unstaged tracked files) */
    boolean clean
}
