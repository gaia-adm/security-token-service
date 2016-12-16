package com.hp.gaia.sts.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * Created by boris on 10/25/16.
 * Proxy to AcM server
 * Communicates with AcM server using RestTemplate and allows to simplify code in other parts of the service
 */
@Service
public class AcmConnectionProxy {

    @Autowired
    private RestTemplate restTemplate;

    private final static IDPConnectManager idcm = AcmConnectionManager.getInstance();

    private final static Map<String, String> idcmConnectionDetails = idcm.getConnectionDetails();
    private final static String acmBaseUrl = idcmConnectionDetails.get("internalProtocol") + "://" + idcmConnectionDetails.get("internalIdmServer") + ":" + idcmConnectionDetails.get("internalPort");

    private final static Logger logger = LoggerFactory.getLogger(AcmConnectionProxy.class);
    private final static String identityTokenName = "gaia.token";

    private ObjectMapper om = new ObjectMapper();


    @PostConstruct
    void init(){
        logger.info("ACM server base URL: " + acmBaseUrl);
    }

    /**
     * Verify user details. Can be done based on identity token only (self verification) or also using the user ID provided;
     * in last case the token should have proper authorization (account admin)
     *
     * @param token  - identity token
     * @param userId - user ID to be verified or null for self verification
     * @return String representation of json, in order to not enforce using special json framework to all consumers OR null if verification fails
     */
    public String userVerification(String token, String userId) {

        if (token == null) {
            return null;
        }

        //https://acms.gaia-local.skydns.local:444/acms/api/users/self with gaia.token cookie (sent by client)
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add("Cookie", identityTokenName + "=" + token);
        HttpEntity entity = new HttpEntity(headers);

        if (userId == null) {
            userId = "self";
        }

        String acmVerificationUrl = acmBaseUrl + "/acms/api/users/" + userId;

        try {
            ResponseEntity<String> userData = restTemplate.exchange(acmVerificationUrl, HttpMethod.GET, entity, String.class);
            JsonNode jsonBody = om.readTree(userData.getBody());
            if (jsonBody != null && jsonBody.get("id") != null && !jsonBody.get("id").asText().isEmpty()) {
                return jsonBody.toString();

            } else {
                logger.error("Failed to get user id from the AcM data");
                return null;
            }
        } catch (IOException e) {
            logger.error("Failed to parse user details data");
            return null;
        } catch (RestClientException rce) {
            logger.error("Failed to verify " + identityTokenName + " against AcM", rce);
            return null;
        }
    }

    /**
     * Verify that tenant with ID provided exists in AccountManagerServer
     * NOTE: tenant in security token service equals account in AccountManagement terminology
     *
     * @param token    - identity token
     * @param tenantId - tenant (account id)
     * @return true, if tenant exists; false otherwise. Note that tenant details are not returned in order to minimize traffic
     */
    public boolean tenantExistenceVerification(String token, String tenantId) {

        if (token == null || tenantId == null) {
            return false;
        }

        //https://acms.gaia-local.skydns.local:444/acms/api/accounts/100 with gaia.token cookie (sent by client)
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add("Cookie", identityTokenName + "=" + token);
        HttpEntity entity = new HttpEntity(headers);

        String acmVerificationUrl = acmBaseUrl + "/acms/api/accounts/" + tenantId;

        try {
            ResponseEntity<String> userData = restTemplate.exchange(acmVerificationUrl, HttpMethod.GET, entity, String.class);
            JsonNode jsonBody = om.readTree(userData.getBody());
            if (jsonBody != null && jsonBody.get("id") != null && !jsonBody.get("id").asText().isEmpty()) {
                return true;

            } else {
                logger.error("Failed to get account id from the AcM data");
                return false;
            }
        } catch (IOException e) {
            logger.error("Failed to parse account details data");
            return false;
        } catch (RestClientException rce) {
            logger.error("Failed to verify account against AcM", rce);
            return false;
        }

    }

}
