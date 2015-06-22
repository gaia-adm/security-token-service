package com.hp.gaia.sts;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by belozovs on 6/22/2015.
 */

public class MyTest {

    @Test
    public void alwaysPassed() throws Exception {
        assertTrue("always true", true);
    }

    @Test
    public void faileIfNoFoo() throws Exception {
        String foo = System.getenv("foo");
        if (foo == null || "".equals(foo)) {
            assertFalse("foo not found", true);
        } else {
            assertTrue("foo found: " + foo, true);
        }

    }

}
