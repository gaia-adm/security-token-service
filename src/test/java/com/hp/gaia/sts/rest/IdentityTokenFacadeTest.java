package com.hp.gaia.sts.rest;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by belozovs on 6/28/2016.
 */
public class IdentityTokenFacadeTest {


    @Test
    public void fetchTenantFromEmail() throws Exception {
        IdentityTokenFacade itf = new IdentityTokenFacade();

        Long t = itf.fetchTenantFromEmail("12345@hpe.com");
        assertNotNull(t);
        assertEquals("t should be 12345", 12345L, t.longValue());
    }

}