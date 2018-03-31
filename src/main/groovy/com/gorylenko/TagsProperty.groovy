package com.gorylenko

import java.text.SimpleDateFormat
import java.util.Collection
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Tag

class TagsProperty extends AbstractGitProperty {

    String doCall(Grgit repo) {
        return isEmpty(repo) ? '' : repo.tag.list().findAll { it.commit == repo.head() }.collect {it.name}.join(',')
    }
}
