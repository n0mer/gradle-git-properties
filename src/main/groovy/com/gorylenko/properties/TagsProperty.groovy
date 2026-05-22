package com.gorylenko.properties

import com.gorylenko.jgit.GitFacade

class TagsProperty extends AbstractGitProperty {

    String doCall(GitFacade facade) {
        if (isEmpty(facade)) return ''
        def headId = facade.head().id
        return facade.tag.list()
                .findAll { it.commit.id == headId }
                .collect { it.name }
                .join(',')
    }
}
