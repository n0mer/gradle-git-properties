package com.gorylenko.properties

import com.gorylenko.jgit.GitFacade

class CommitIdAbbrevProperty extends AbstractGitProperty {

    private final int abbrevLength

    CommitIdAbbrevProperty() {
        this(7)
    }

    CommitIdAbbrevProperty(int abbrevLength) {
        this.abbrevLength = abbrevLength
    }

    String doCall(GitFacade facade) {
        return isEmpty(facade) ? '' : facade.getAbbreviatedId(abbrevLength)
    }
}
