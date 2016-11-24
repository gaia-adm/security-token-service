package com.hp.gaia.sts.rest;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.gaia.sts.util.AcmConnectionManager;
import com.hp.gaia.sts.util.AcmConnectionProxy;
import com.hp.gaia.sts.util.IDPConnectManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by belozovs on 1/20/2016.
 * Supports Oauth2 authorization code flow against Account Management Service
 */
@RestController
public class UserLoginController {

    private final static Logger logger = LoggerFactory.getLogger(UserLoginController.class);

    @Autowired
    AcmConnectionProxy acmConnectionProxy;

    @Autowired
    RestTemplate restTemplate;

    ObjectMapper mapper = new ObjectMapper();

    private final static IDPConnectManager idcm = AcmConnectionManager.getInstance();
    private final static Map<String, String> idcmConnectionDetails = idcm.getConnectionDetails();
    private final static String idcmDomain = idcmConnectionDetails.get("domain");
    private final static String idcmPort = idcmConnectionDetails.get("externalPort");
    private final static String idcmProtocol = idcmConnectionDetails.get("externalProtocol");

    private static String tokenEndpointUrl;
    private static String authEndpointUrl;
    private static String jwksUrl;
    private static List<String> algorithms = new ArrayList<>();

    @PostConstruct
    void init() {

        if (!validateConfiguration()) {
            //hope that fleet will restart it and meanwhile the configuration becomes OK
            logger.error("Insufficient configuration provided, exiting ....");
            System.exit(-1);
        }

    }

    boolean validateConfiguration() {

        boolean result = true;

        Set<String> acmConnectionBadDetails = idcmConnectionDetails.entrySet().stream().filter(e -> StringUtils.isEmpty(e.getValue())).map(Map.Entry::getKey).collect(Collectors.toSet());
        Set<String> acmClientBadDetails = idcmConnectionDetails.entrySet().stream().filter(e -> StringUtils.isEmpty(e.getValue())).map(Map.Entry::getKey).collect(Collectors.toSet());

        if (!acmConnectionBadDetails.isEmpty()) {
            result = false;
            acmConnectionBadDetails.forEach(e -> logger.error("Empty or null value provided for " + e));
        }

        if (!acmClientBadDetails.isEmpty()) {
            result = false;
            acmClientBadDetails.forEach(e -> logger.error("Empty or null value provided for " + e));
        }

        return result;

    }


    @RequestMapping("/login")
    @ResponseBody
    void login(HttpServletResponse httpServletResponse) {
        httpServletResponse.setHeader("Location", idcmProtocol + "://acmc." + idcmDomain + "/acmc");
        httpServletResponse.setStatus(302);
    }


    @RequestMapping("/verify")
    @ResponseBody
    ResponseEntity<String> verify(HttpServletRequest request, HttpServletResponse response) {

        boolean isFound = false;

        Cookie cookieToDecode = null;

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("gaia.token")) {
                    isFound = true;
                    String goodCookie = verifyIdTokenByAcm(cookie);
                    if (goodCookie != null) {
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        return new ResponseEntity<>(goodCookie, headers, HttpStatus.OK);
                    } else {
                        break;
                    }
                }
            }
        }
        String message = (isFound) ? "Token verification has failed" : "No token provided";
        logger.error(message);
        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

    }

    @RequestMapping(value = "/logout")
    @ResponseBody
    void logout(HttpServletRequest request, HttpServletResponse response) {

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals("gaia.token")) {
                    response.addCookie(createIdentityTokenCookie("gaia.token", null, 0));
                    response.addCookie(createIdentityTokenCookie("user", null, 0));
                }
            }
        }

        response.setHeader("Location", request.getContextPath() + "/login.jsp");
        response.setStatus(302);
    }


    /**
     * @param cookie gaia.token cookie provided in request
     * @return the identity (user id and name, accounts, roles, etc.) OR null, if no valid identity found
     */
    String verifyIdTokenByAcm(Cookie cookie) {

        String stringIdToken = cookie.getValue();
        logger.info("StringIdToken is " + stringIdToken);

        //http://acms.gaia-local.skydns.local:88/acms/api/users/self with gaia.token cookie (sent by client)

        String userDetails = acmConnectionProxy.userVerification(stringIdToken, null);
        if (userDetails == null) {
            logger.error("Failed to verify identity token, returns null");
            return null;
        } else {
            return userDetails;
        }
    }

    private Cookie createIdentityTokenCookie(String name, String value, Integer expiration) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setDomain("." + idcmDomain);
        cookie.setSecure(false);
        if (expiration != null) {
            cookie.setMaxAge(expiration);
        }
        return cookie;
    }

}
