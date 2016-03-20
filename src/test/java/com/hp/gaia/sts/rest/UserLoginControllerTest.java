package com.hp.gaia.sts.rest;

import com.hp.gaia.sts.util.DexConnectionManager;
import com.hp.gaia.sts.util.IDPConnectManager;
import junit.framework.TestCase;

/**
 * Created by belozovs on 2/23/2016.
 *
 */
public class UserLoginControllerTest extends TestCase {


    public void testSwitchToInternalUrl() throws Exception {
        String internalProtocol = "http";
        String externalProtocol = "https";
        String domain = "gaia-local.skydns.local";
        String internalDexServer = "dexworker.skydns.local";
        String internalPort = "5556";
        String externalPort = "88";

        String token_endpoint = "http://gaia-local.skydns.local:88/token";
        String jwks_uri = "http://gaia-local.skydns.local:88/keys";

        IDPConnectManager idpcm = DexConnectionManager.getInstance();
        idpcm.getConnectionDetails().put("domain", domain);
        idpcm.getConnectionDetails().put("internalDexServer", internalDexServer);
        idpcm.getConnectionDetails().put("internalPort", internalPort);
        idpcm.getConnectionDetails().put("externalPort", externalPort);
        idpcm.getConnectionDetails().put("internalProtocol", internalProtocol);
        idpcm.getConnectionDetails().put("externalProtocol", externalProtocol);

        UserLoginController ulc = new UserLoginController();


        UserLoginController userLoginController = new UserLoginController();
        String jwksUrl = userLoginController.switchToInternalUrl(jwks_uri);
        String tokenUrl = userLoginController.switchToInternalUrl(token_endpoint);

        assertEquals("http://dexworker.skydns.local:5556/keys", jwksUrl);
        assertEquals("http://dexworker.skydns.local:5556/token", tokenUrl);
    }
}