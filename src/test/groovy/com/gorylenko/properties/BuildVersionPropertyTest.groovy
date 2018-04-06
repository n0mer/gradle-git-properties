package com.gorylenko.properties

import static org.junit.Assert.*

import org.junit.Test

class BuildVersionPropertyTest {

    @Test
    public void testDoCall() {
        assertEquals("1.0", new BuildVersionProperty("1.0").doCall(null))
    }

}
