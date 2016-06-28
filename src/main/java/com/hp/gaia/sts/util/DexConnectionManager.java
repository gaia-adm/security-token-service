package com.hp.gaia.sts.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by belozovs on 2/7/2016.
 * Collects basic dex configuration from environment
 * Also collects previously saved dex-client configuration in order to prevent clients multiplying on restart
 */
public class DexConnectionManager implements IDPConnectManager {

    private final static Logger logger = LoggerFactory.getLogger(DexConnectionManager.class);

    private final static String internalProtocol = "http";
    private final static String externalProtocol = "https";
    private final static String internalPort = "5556";
    private final static String externalPort = "444";
    private final static String externalHttpPort = "88";

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
            if (StringUtils.isEmpty(domain)) {
                logger.error("DOMAIN environment variable not set; using gaia-local.skydns.local - bad for all though working for vagrant");
                domain = "gaia-local.skydns.local";
            }
            logger.info("DOMAIN is " + domain);

            String internalDexServer = System.getenv("INTERNAL_DEX_SERVER");    //e.g., dexworker.skydns.local
            if (StringUtils.isEmpty(internalDexServer)) {
                logger.error("INTERNAL_DEX_SERVER environment variable not set; using dexworker.skydns.local; it may work but you must verify it is not by mistake");
                internalDexServer = "dexworker.skydns.local";
            }

            logger.info("INTERNAL_DEX_SERVER is " + internalDexServer);

            String externalDexUrl = externalProtocol + "://" + domain + ":" + externalPort;
            dexConnectionDetails.put("externalDexUrl", externalDexUrl);
            dexConnectionDetails.put("discoveryUrl", internalProtocol + "://" + internalDexServer + ":" + internalPort + "/.well-known/openid-configuration");

            dexConnectionDetails.put("domain", domain);
            dexConnectionDetails.put("internalDexServer", internalDexServer);
            dexConnectionDetails.put("internalPort", internalPort);
            dexConnectionDetails.put("externalPort", externalPort);
            dexConnectionDetails.put("externalHttpPort", externalHttpPort);
            dexConnectionDetails.put("internalProtocol", internalProtocol);
            dexConnectionDetails.put("externalProtocol", externalProtocol);
            //Since internal communication between services goes via http, claims.get("iss") validation in UserLoginController.decodeIdToken method fails
            //InternalIssuerUrl is a workaround for this problem until we switch to the SSL based internal communication
            String internalIssuerUrl = internalProtocol+"://"+domain+":"+externalHttpPort;
            dexConnectionDetails.put("internalIssuerUrl", internalIssuerUrl);

        }

        return dexConnectionDetails;
    }

    public Map<String, String> getClientDetails() {
        if (dexClientDetails.isEmpty()) {
            dexClientDetails.put("dexClientId", System.getenv("DEX_APP_CLIENT_ID"));
            dexClientDetails.put("dexClientSecret", System.getenv("DEX_APP_CLIENT_SECRET"));
            dexClientDetails.put("dexAppRedirectUrl", System.getenv("DEX_APP_REDIRECTURL_0"));
        }

        return dexClientDetails;
    }
}
