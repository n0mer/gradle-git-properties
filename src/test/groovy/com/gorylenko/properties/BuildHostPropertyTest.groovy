package com.gorylenko.properties

import static org.junit.Assert.*

import org.junit.Test

class BuildHostPropertyTest {

    @Test
    public void testDoCall() {
        assertNotNull(new BuildHostProperty().doCall(null))
    }

}
