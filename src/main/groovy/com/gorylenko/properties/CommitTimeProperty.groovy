package com.gorylenko.properties

import com.gorylenko.jgit.GitFacade
import java.text.SimpleDateFormat
import java.time.Instant

class CommitTimeProperty extends AbstractGitProperty {
    private String dateFormat
    private String timezone

    CommitTimeProperty(String dateFormat, String timezone) {
        this.dateFormat = dateFormat
        this.timezone = timezone
    }

    String doCall(GitFacade facade) {
        return isEmpty(facade) ? '' : formatDate(facade.head().dateTime, dateFormat, timezone)
    }

    private String formatDate(Instant instant, String dateFormat, String timezone) {
        String date
        if (dateFormat) {
            def sdf = new SimpleDateFormat(dateFormat)
            if (timezone) {
                sdf.setTimeZone(TimeZone.getTimeZone(timezone))
            }
            date = sdf.format(Date.from(instant))
        } else {
            date = instant.epochSecond
        }
    }

}
