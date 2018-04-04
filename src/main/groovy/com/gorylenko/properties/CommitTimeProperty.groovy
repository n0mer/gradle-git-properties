package com.gorylenko.properties

import java.text.SimpleDateFormat
import java.time.Instant
import org.ajoberstar.grgit.Grgit

class CommitTimeProperty extends AbstractGitProperty {
    private String dateFormat
    private String timezone

    CommitTimeProperty(String dateFormat, String timezone) {
        this.dateFormat = dateFormat
        this.timezone = timezone
    }

    String doCall(Grgit repo) {
        return isEmpty(repo) ? '' : formatDate(repo.head().dateTime.toInstant(), dateFormat, timezone)
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
