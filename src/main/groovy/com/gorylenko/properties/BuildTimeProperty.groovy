package com.gorylenko.properties

import java.text.SimpleDateFormat
import java.time.Instant

import org.ajoberstar.grgit.Grgit

class BuildTimeProperty extends Closure<String> {
    private String dateFormat
    private String timezone

    BuildTimeProperty(String dateFormat, String timezone) {
        super(null)
        this.dateFormat = dateFormat
        this.timezone = timezone
    }

    String doCall(Grgit repo) {
        return formatDate(Instant.now(), dateFormat, timezone)
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
            date = instant.epochSecond.toString()
        }
        return date
    }
}
