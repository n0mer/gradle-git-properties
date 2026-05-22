package com.gorylenko.properties

import static org.junit.Assert.*

import org.junit.Test
import com.gorylenko.jgit.GitFacade

class BuildVersionPropertyTest {

    // GitFacade test - BuildVersionProperty doesn't use git, just returns version
    @Test
    public void testDoCall() {
        assertEquals("1.0", new BuildVersionProperty("1.0").doCall((GitFacade) null))
    }

    @Test
    public void testDoCallWithDifferentVersion() {
        assertEquals("2.0", new BuildVersionProperty("2.0").doCall((GitFacade) null))
    }

}
