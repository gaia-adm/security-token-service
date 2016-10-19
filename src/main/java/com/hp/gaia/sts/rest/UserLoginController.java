package com.hp.gaia.sts.rest;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hp.gaia.sts.util.AcmConnectionManager;
import com.hp.gaia.sts.util.DexConnectionManager;
import com.hp.gaia.sts.util.IDPConnectManager;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by belozovs on 1/20/2016.
 * Supports Oauth2 authorization code flow against Dex
 */
@RestController
public class UserLoginController {

    private final static Logger logger = LoggerFactory.getLogger(UserLoginController.class);

    @Autowired
    RestTemplate restTemplate;

    ObjectMapper mapper = new ObjectMapper();

    private final static IDPConnectManager idcm = AcmConnectionManager.getInstance();
    private final static Map<String, String> idcmConnectionDetails = idcm.getConnectionDetails();
    private final static String idcmDomain = idcmConnectionDetails.get("domain");
    private final static String idcmPort = idcmConnectionDetails.get("externalPort");
    private final static String idcmProtocol = idcmConnectionDetails.get("externalProtocol");

    private final static IDPConnectManager idpcm = DexConnectionManager.getInstance();
    private final static Map<String, String> idpConnectionDetails = idpcm.getConnectionDetails();
    private final static Map<String, String> idpClientDetails = idpcm.getClientDetails();

    private final static String externalDexUrl = idpConnectionDetails.get("externalDexUrl"); //"https://gaia.skydns.local:444";
    private final static String discoveryUrl = idpConnectionDetails.get("discoveryUrl"); //internalDexUrl + "/.well-known/openid-configuration";
    private final static String domain = idpConnectionDetails.get("domain");
    private final static String internalIssuerUrl = idpConnectionDetails.get("internalIssuerUrl"); //"http://gaia.skydns.local:88";

    private final static String clientId = idpClientDetails.get("dexClientId");
    private final static String clientSecret = idpClientDetails.get("dexClientSecret");
    private final static String callbackUrl = idpClientDetails.get("dexAppRedirectUrl");

    private static String tokenEndpointUrl;
    private static String authEndpointUrl;
    private static String jwksUrl;
    private static List<String> algorithms = new ArrayList<>();

    @PostConstruct
    void init() {

        if (!Boolean.valueOf(System.getenv("noDex"))) {


            if (!validateConfiguration()) {
                //hope that fleet will restart it and meanwhile the configuration becomes OK
                logger.error("Insufficient configuration provided, exiting ....");
                System.exit(-1);
            }

            ResponseEntity<String> openIdConfig = restTemplate.getForEntity(discoveryUrl, String.class);
            JsonNode jsonOpenIdConfig = null;
            try {
                jsonOpenIdConfig = mapper.readTree(openIdConfig.getBody());
            } catch (IOException e) {
                //Todo - boris: should be moved to special handler to retry 3-4 times, in case of failure
                logger.error("Failed to get OIDC well-known configuration, cannot continue", e);
            }

            if (jsonOpenIdConfig != null) {

                jwksUrl = jsonOpenIdConfig.get("jwks_uri") != null ? switchToInternalUrl(jsonOpenIdConfig.get("jwks_uri").asText()) : null;
                tokenEndpointUrl = jsonOpenIdConfig.get("token_endpoint") != null ? switchToInternalUrl(jsonOpenIdConfig.get("token_endpoint").asText()) : null;
                authEndpointUrl = jsonOpenIdConfig.get("authorization_endpoint") != null ? jsonOpenIdConfig.get("authorization_endpoint").asText().replace(idpConnectionDetails.get("internalProtocol"), idpConnectionDetails.get("externalProtocol")).replace(idpConnectionDetails.get("externalHttpPort"), idpConnectionDetails.get("externalPort")) : null;
                logger.debug("authEndpointUrl: " + authEndpointUrl);
                logger.debug("tokenEndpointUrl: " + tokenEndpointUrl);
                logger.debug("jwksUrl: " + jwksUrl);

                if (jsonOpenIdConfig.get("id_token_signing_alg_values_supported") != null) {
                    if (jsonOpenIdConfig.get("id_token_signing_alg_values_supported").isArray()) {
                        for (int i = 0; i < jsonOpenIdConfig.get("id_token_signing_alg_values_supported").size(); i++) {
                            algorithms.add(jsonOpenIdConfig.get("id_token_signing_alg_values_supported").get(i).asText());
                        }
                        logger.debug("token_endpoint_auth_methods_supported (algorithms) found: " + algorithms.size());
                    } else {
                        algorithms.add(jsonOpenIdConfig.get("id_token_signing_alg_values_supported").asText());
                        logger.debug("Single token_endpoint_auth_methods_supported (algorithms) found and it is a String - suspicious, expected to be Array");
                    }
                }

                if (jsonOpenIdConfig.get("id_token_signing_alg_values_supported") == null || StringUtils.isEmpty(jwksUrl) || StringUtils.isEmpty(tokenEndpointUrl) || StringUtils.isEmpty(authEndpointUrl)) {
                    //Todo - boris: should be moved to special handler to retry 3-4 times, in case of failure
                    logger.error("One of well-known oidc-configuration parameter is null or empty (see preceding messages for more details)");
                }
            }
            logger.info("Configured to work with Dex as " + clientId);
        }
    }

    boolean validateConfiguration() {

        boolean result = true;

        Set<String> connectionBadDetails = idpConnectionDetails.entrySet().stream().filter(e -> StringUtils.isEmpty(e.getValue())).map(Map.Entry::getKey).collect(Collectors.toSet());
        Set<String> clientBadDetails = idpClientDetails.entrySet().stream().filter(e -> StringUtils.isEmpty(e.getValue())).map(Map.Entry::getKey).collect(Collectors.toSet());

        if (!connectionBadDetails.isEmpty()) {
            result = false;
            connectionBadDetails.stream().forEach(e -> logger.error("Empty or null value provided for " + e));
        }

        if (!clientBadDetails.isEmpty()) {
            result = false;
            clientBadDetails.stream().forEach(e -> logger.error("Empty or null value provided for " + e));
        }

        return result;

    }


    @RequestMapping("/login")
    @ResponseBody
    void login(HttpServletResponse httpServletResponse) {
//        httpServletResponse.setHeader("Location", authEndpointUrl + "?client_id=" + clientId + "&redirect_uri=" + callbackUrl + "&response_type=code&scope=openid+email+profile&state=");
        httpServletResponse.setHeader("Location", idcmProtocol+"://acmc."+idcmDomain+"/acmc");
        httpServletResponse.setStatus(302);
    }

    @RequestMapping(value = "/callback")
    @ResponseBody
    void loginCallback(@RequestParam(value = "code", required = false) String code, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {

        if (code == null) {
            //password reset flow or just a hack - redirecting to logout
            logout(httpServletRequest, httpServletResponse);
        } else {

            byte[] clientBytes = (clientId + ":" + clientSecret).getBytes();
            String authValue = Base64.getEncoder().encodeToString(clientBytes);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.add("Authorization", "Basic " + authValue);

            logger.debug("code=" + code);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("client_id", clientId);
            map.add("client_secret", clientSecret);
            map.add("grant_type", "authorization_code");
            map.add("code", code);
            map.add("scope", "openid+email+profile");
            map.add("redirect_uri", callbackUrl);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            ResponseEntity<String> dexResponse = restTemplate.postForEntity(tokenEndpointUrl, request, String.class);

            JsonNode jsonDexResponse = null;
            try {
                jsonDexResponse = mapper.readTree(dexResponse.getBody());
            } catch (IOException e) {
                e.printStackTrace();
                httpServletResponse.setStatus(401);
            }

            httpServletResponse.setHeader("Location", httpServletRequest.getContextPath() + "/welcome.jsp");
            if (jsonDexResponse != null && jsonDexResponse.get("id_token") != null) {
                Cookie cookie = createIdentityTokenCookie("gaia.token",jsonDexResponse.get("id_token").asText(), null);
                httpServletResponse.addCookie(cookie);
            }

            httpServletResponse.setStatus(302);
        }

    }

    @RequestMapping("/verify")
    @ResponseBody
    ResponseEntity<String> verify(HttpServletRequest request, HttpServletResponse response) {

        boolean isVerified = false;

        Cookie cookieToDecode = null;

        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("gaia.it")) {
                isVerified = verifyIdToken(cookie);

                cookieToDecode = cookie;
                break;
            }


        }

        if (isVerified) {
            JsonNode body = decodeIdToken(cookieToDecode);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            return new ResponseEntity<>(body.toString(), headers, HttpStatus.OK);
        } else {    //bad cookie or not presented at all
            String message = (cookieToDecode == null) ? "No token provided" : "Token verification has failed for " + cookieToDecode.getValue();
            logger.error(message);
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/logout")
    @ResponseBody
    void logout(HttpServletRequest request, HttpServletResponse response) {

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals("gaia.token")) {
                    response.addCookie(createIdentityTokenCookie("gaia.token", null, 0));
                    response.addCookie(createIdentityTokenCookie("user",null, 0));
                }
            }
        }

        response.setHeader("Location", request.getContextPath() + "/login.jsp");
        response.setStatus(302);
    }


    boolean verifyIdToken(Cookie cookie) {

        String stringIdToken = cookie.getValue();
        try {
            SignedJWT token = SignedJWT.parse(stringIdToken);
            //TODO - boris: brings the new keys every time. What happens if dex is not available for a moment?
            // Should previous set be cached and used until no keys fit to decode
            // and so the cache should be invalidate and rebuilt from scratch
            JWKSet publicKeys = JWKSet.load(new URL(jwksUrl));
            JWSVerifier[] verifiers = new JWSVerifier[publicKeys.getKeys().size()];
            for (int i = 0; i < publicKeys.getKeys().size(); i++) {
                verifiers[i] = new RSASSAVerifier(((RSAKey) publicKeys.getKeys().get(i)).toRSAPublicKey());
            }


            for (JWSVerifier verifier : verifiers) {
                //check that token signed with the original signature
                if (token.verify(verifier)) {
                    //check that provided signature (comes fromn cookie) is the same as original (comes from Dex)
                    //token.getSignature().
                    return true;
                }
            }

            return false;

        } catch (ParseException | IOException | JOSEException e) {
            e.printStackTrace();
        }

        return false;
    }



    /**
     * @param cookie
     * @return the identity (user id and name, accounts, roles, etc.) OR null, if no valid identity found
     */
    String verifyIdTokenByAcm(Cookie cookie) {

        String stringIdToken = cookie.getValue();
        logger.info("StringIdToken is " + stringIdToken);

        //http://acms.gaia-local.skydns.local:88/acms/api/users/self with gaia.token cookie (sent by client)
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Cookie","gaia.token="+stringIdToken);
        HttpEntity entity = new HttpEntity(headers);

        String acmVerificationUrl = idcm.getConnectionDetails().get("internalProtocol")+"://"+idcm.getConnectionDetails().get("internalIdmServer")+":"+idcm.getConnectionDetails().get("internalPort")+"/acms/api/users/self";
//        logger.debug("Verifying against " + acmVerificationUrl);

        ResponseEntity<String> userData;
        try {
            userData = restTemplate.exchange(acmVerificationUrl,HttpMethod.GET,entity, String.class);
        } catch (RestClientException rce) {
            logger.error("Failed to verify gaia.token against AcM", rce);
            return null;
        }

//        logger.trace("User data string: " + userData.getBody());

        JsonNode jsonBody = null;
        try {
            jsonBody = mapper.readTree(userData.getBody());
        } catch (IOException e) {
            logger.error("Failed to parse the identity verification response", e);
            return null;
        }

        if(jsonBody.get("id").toString() != null && !jsonBody.get("id").asText().isEmpty()){
            return jsonBody.toString();
        } else {
            return null;
        }
    }

    JsonNode decodeIdToken(Cookie cookieToDecode) throws RuntimeException {

        final JsonNodeFactory factory = JsonNodeFactory.instance;

        ObjectNode jsonClaims = factory.objectNode();
        try {
            String stringIdToken = cookieToDecode.getValue();
            SignedJWT token = SignedJWT.parse(stringIdToken);

            Map<String, Object> claims = token.getJWTClaimsSet().getClaims();

            //http://openid.net/specs/openid-connect-core-1_0.html#IDTokenValidation
            //probably getAlgorithm() will never return null - if so, it will fail earlier, on parse step
            if (!algorithms.contains(token.getHeader().getAlgorithm().getName())) {
                throw new RuntimeException("bad algorithm: " + token.getHeader().getAlgorithm());
            }
            jsonClaims.put("alg", token.getHeader().getAlgorithm().getName());
            if (!token.getHeader().getType().getType().equals("JWT")) {
                throw new RuntimeException("bad token type");
            }
            jsonClaims.put("typ", token.getHeader().getType().getType());
            if (!claims.get("iss").toString().equals(externalDexUrl)) {
                //claims.get("iss").toString() is http://gaia-local.skydns.local:88 due to the fact that internal communications are done via http
                //externalDexUrl is https://gaia-local.skydns.local:444
                //in order to prevent failure there is a workaround, but if it does not help, then issuer is really invalid
                if(!claims.get("iss").toString().equals(internalIssuerUrl)){
                    throw new RuntimeException("bad issue: " + claims.get("iss").toString());
                }
            }
            jsonClaims.put("iss", claims.get("iss").toString());
            if (!claims.get("aud").toString().replaceAll("\\[", "").replaceAll("\\]", "").equals(clientId)) {
                throw new RuntimeException("bad audience");
            }
            jsonClaims.put("aud", claims.get("aud").toString());
            if (DateUtils.toSecondsSinceEpoch((Date) claims.get("exp")) * 1000 < System.currentTimeMillis()) {
                throw new RuntimeException("expired at " + claims.get("exp"));
            }
            jsonClaims.put("exp", claims.get("exp").toString());
            if (DateUtils.toSecondsSinceEpoch((Date) claims.get("iat")) * 1000 > System.currentTimeMillis()) {
                throw new RuntimeException("issued at " + claims.get("iat").toString());
            }
            jsonClaims.put("iat", claims.get("iat").toString());
            if (!isValidEmail(claims.get("email").toString())) {
                throw new RuntimeException("invalid email: " + claims.get("email").toString());
            }
            jsonClaims.put("email", claims.get("email").toString());
            if (claims.get("sub").toString().isEmpty()) {
                throw new RuntimeException("no sub provided");
            }
            jsonClaims.put("sub", claims.get("sub").toString());

        } catch (ParseException e) {
            e.printStackTrace();
        }


        return jsonClaims;
    }

    private boolean isValidEmail(String emailAddress) {
        return !StringUtils.isEmpty(emailAddress) && emailAddress.contains("@");
    }

    private Cookie createIdentityTokenCookie(String name, String value, Integer expiration) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setDomain("."+idcmDomain);
        cookie.setSecure(false);
        if (expiration != null) {
            cookie.setMaxAge(expiration);
        }
        return cookie;
    }

    String switchToInternalUrl(String externalUrl) {
        String internalUrl =  externalUrl.replace(domain, idpConnectionDetails.get("internalDexServer")).replace(idpConnectionDetails.get("externalHttpPort"), idpConnectionDetails.get("internalPort")).replace(idpConnectionDetails.get("externalProtocol"), idpConnectionDetails.get("internalProtocol"));
        logger.info("Switch from " + externalUrl + " to " + internalUrl);
        return internalUrl;
    }

}
