package com.hp.gaia.sts.util;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Created by belozovs on 2/23/2016.
 */
public class DexConnectionManagerTest {


    @Test
    public void testGetInstance() throws Exception {
        IDPConnectManager idpcm = DexConnectionManager.getInstance();
        assertNotNull(idpcm);
        assertEquals(idpcm.getClass(), DexConnectionManager.class);
        assertTrue(idpcm instanceof IDPConnectManager);
    }

    @Test
    public void testGetConnectionDetails() throws Exception {
        IDPConnectManager idpcm = DexConnectionManager.getInstance();
        assertEquals("Domain", idpcm.getConnectionDetails().get("domain"), "gaia-local.skydns.local");
        assertEquals("internalDexUrl", idpcm.getConnectionDetails().get("internalDexUrl"), "http://dexworker.skydns.local:5556");
        assertEquals("externalDexUrl", idpcm.getConnectionDetails().get("externalDexUrl"), "https://gaia-local.skydns.local:88");
        assertEquals("discoveryUrl", idpcm.getConnectionDetails().get("discoveryUrl"), "http://dexworker.skydns.local:5556/.well-known/openid-configuration");
    }

    @Test
    public void testGetClientDetails() throws Exception {
        IDPConnectManager idpcm = DexConnectionManager.getInstance();
        assertNotNull(idpcm.getClientDetails());
    }

}