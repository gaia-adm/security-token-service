package com.hp.gaia.sts.util;

import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by belozovs on 2/7/2016.
 * Collects basic dex configuration from environment
 * Also collects previously saved dex-client configuration in order to prevent clients multiplying on restart
 */
public class DexConnectionManager {

    private static final DexConnectionManager instance = new DexConnectionManager();
    private Map<String, String> dexConnectionDetails = new HashMap<>();
    private Map<String, String> dexClientDetails = new HashMap<>();

    protected DexConnectionManager() {
    }

    public static DexConnectionManager getInstance() {
        return instance;
    }

    public Map<String, String> getConnectionDetails() {

        if (dexConnectionDetails.isEmpty()) {

            //TODO - boris: configurable scheme and port
            String domain = System.getenv("DOMAIN");
            String internalDexServer = domain.replace(domain.substring(0,domain.indexOf('.')), "dexworker");
            String internalDexUrl = "http://"+internalDexServer+":5556";
            String externalDexUrl = "http://"+domain+":88";
            dexConnectionDetails.put("internalDexUrl", internalDexUrl);
            dexConnectionDetails.put("externalDexUrl", externalDexUrl);
            dexConnectionDetails.put("discoveryUrl", dexConnectionDetails.get("internalDexUrl") + "/.well-known/openid-configuration");
            dexConnectionDetails.put("domain", domain);
        }

        return dexConnectionDetails;
    }

    public Map<String, String> getClientDetails() {
        if(dexClientDetails.isEmpty()){
            dexClientDetails.put("dexClientId", System.getenv("DEX_APP_CLIENT_ID"));
            dexClientDetails.put("dexClientSecret", System.getenv("DEX_APP_CLIENT_SECRET"));
            dexClientDetails.put("dexAppRedirectUrl", System.getenv("DEX_APP_REDIRECTURL_0"));
        }

        return dexClientDetails;
    }

}
