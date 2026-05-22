package com.gorylenko.properties

import static org.junit.Assert.*

import org.junit.Test
import com.gorylenko.jgit.GitFacade

class BuildHostPropertyTest {

    // BuildHostProperty doesn't use git, just returns hostname
    @Test
    public void testDoCall() {
        def result = new BuildHostProperty().doCall((GitFacade) null)
        assertNotNull(result)
        // Should return hostname (non-empty string on most systems)
        // May be empty string if hostname resolution fails
    }

}
