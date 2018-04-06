package com.gorylenko.properties

import static org.junit.Assert.*

import java.text.SimpleDateFormat
import java.time.Instant
import org.junit.Test

class BuildTimePropertyTest {

    @Test
    public void testDoCall() {
        Instant i1 = Instant.now()
        String time1 = new BuildTimeProperty(null, null).doCall(null)
        Instant i2 = Instant.now()
        assertNotNull(i1.epochSecond <= Long.parseLong(time1) && Long.parseLong(time1) <= i2.epochSecond)
    }

    @Test
    public void testDoCallWithFormat() {
        Instant i1 = Instant.now()
        String time1 = new BuildTimeProperty("yyyy-MM-dd'T'HH:mmZ", "PST").doCall(null)
        Instant i2 = Instant.now()

        def sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ")
        sdf.setTimeZone(TimeZone.getTimeZone("PST"))
        Instant buildTime = sdf.parse(time1).toInstant()

        assertNotNull(i1.epochSecond <= buildTime.epochSecond && buildTime.epochSecond <= i2.epochSecond)
    }

}
