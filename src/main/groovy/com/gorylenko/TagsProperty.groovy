package com.gorylenko

import java.text.SimpleDateFormat
import java.util.Collection
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Tag

class TagsProperty extends Closure<String>{

    TagsProperty() {
        super(null)
    }

    String doCall(Grgit repo) {
        Commit head = repo.head()
        return repo.tag.list().findAll { it.commit == head }.collect {it.name}.join(',')
    }
}
