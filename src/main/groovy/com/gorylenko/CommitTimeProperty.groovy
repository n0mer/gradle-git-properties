package com.gorylenko

import java.text.SimpleDateFormat
import org.ajoberstar.grgit.Grgit

class CommitTimeProperty extends Closure<String>{
    private String dateFormat
    private String timezone

    CommitTimeProperty(dateFormat, timezone) {
        super(null)
        this.dateFormat = dateFormat
        this.timezone = timezone
    }

    String doCall(Grgit repo) {
        return formatDate(repo.head().time, dateFormat, timezone)
    }

    private String formatDate(long timestamp, String dateFormat, String timezone) {
        String date
        if (dateFormat) {
            def sdf = new SimpleDateFormat(dateFormat)
            if (timezone) {
                sdf.setTimeZone(TimeZone.getTimeZone(timezone))
            }
            date = sdf.format(new Date(timestamp * 1000L))
        } else {
            date = timestamp.toString()
        }
    }

}
