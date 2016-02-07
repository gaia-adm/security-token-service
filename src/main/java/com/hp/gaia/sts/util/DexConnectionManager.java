package com.hp.gaia.sts.util;

import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by belozovs on 2/7/2016.
 */
public class DexConnectionManager {

    private static final DexConnectionManager instance = new DexConnectionManager();
    private Map<String, String> dexConnectionDetails = new HashMap<>();

    protected DexConnectionManager() {
    }

    public static DexConnectionManager getInstance() {
        return instance;
    }

    public Map<String, String> getConnectionDetails() {

        if (dexConnectionDetails.isEmpty()) {
            String externalDexUrl = System.getenv("externalDexUrl");
            if (StringUtils.isEmpty(externalDexUrl)) {
                throw new RuntimeException("externalDexUrl environment variable is not set");
            }

            String internalDexUrl = System.getenv("internalDexUrl");
            if(StringUtils.isEmpty(internalDexUrl)){
                internalDexUrl =  "http://dexworker.skydns.local:5556";
            }

            dexConnectionDetails.put("externalDexUrl", externalDexUrl);
            dexConnectionDetails.put("internalDexUrl", internalDexUrl);
            dexConnectionDetails.put("discoveryUrl", dexConnectionDetails.get("internalDexUrl") + "/.well-known/openid-configuration");
        }

        return dexConnectionDetails;
    }

}
