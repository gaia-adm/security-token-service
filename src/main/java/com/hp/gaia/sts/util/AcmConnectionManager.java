package com.hp.gaia.sts.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by belozovs on 10/19/2016.
 * Collects basic ACM configuration from environment
  */
public class AcmConnectionManager implements IDPConnectManager{

    private final static Logger logger = LoggerFactory.getLogger(AcmConnectionManager.class);

    private final static String internalProtocol = "http";
    private final static String externalProtocol = "https";
    private final static String internalPort = "3000";
    private final static String externalPort = "444";
    private final static String externalHttpPort = "88";

    private static final AcmConnectionManager instance = new AcmConnectionManager();
    private Map<String, String> acmConnectionDetails = new HashMap<>();
    private Map<String, String> acmClientDetails = new HashMap<>();

    protected AcmConnectionManager() {
    }

    public static AcmConnectionManager getInstance() {
        return instance;
    }


    @Override
    public Map<String, String> getConnectionDetails() {

        if (acmConnectionDetails.isEmpty()) {

            //TODO - boris: configurable scheme and port
            String domain = System.getenv("DOMAIN");
            if (StringUtils.isEmpty(domain)) {
                logger.error("DOMAIN environment variable not set; using gaia-local.skydns.local - bad for all though working for vagrant");
                domain = "gaia-local.skydns.local";
            }
            logger.info("DOMAIN is " + domain);

            String internalIdmServer = System.getenv("INTERNAL_ACM_SERVER");    //e.g., dexworker.skydns.local
            if (StringUtils.isEmpty(internalIdmServer)) {
                logger.error("INTERNAL_ACM_SERVER environment variable not set; using acmserver.skydns.local; it may work but you must verify it is not by mistake");
                internalIdmServer = "acmserver.skydns.local";
            }

            String externalIdmUrl = externalProtocol + "://" + domain + ":" + externalPort;

            logger.info("INTERNAL_ACM_SERVER is " + internalIdmServer);
            logger.info("EXTERNAL_ACM_SERVER is " + externalIdmUrl);

            acmConnectionDetails.put("externalIdmUrl", externalIdmUrl);
            acmConnectionDetails.put("domain", domain);
            acmConnectionDetails.put("internalIdmServer", internalIdmServer);
            acmConnectionDetails.put("internalPort", internalPort);
            acmConnectionDetails.put("externalPort", externalPort);
            acmConnectionDetails.put("externalHttpPort", externalHttpPort);
            acmConnectionDetails.put("internalProtocol", internalProtocol);
            acmConnectionDetails.put("externalProtocol", externalProtocol);

        }

        return acmConnectionDetails;
    }

    //There is no need for IDP client details when working with ACM; not like in Dex flavor, the IDP is google and it is only accessed by ACM
    @Override
    public Map<String, String> getClientDetails() {
        return new HashMap<>();
    }

}
