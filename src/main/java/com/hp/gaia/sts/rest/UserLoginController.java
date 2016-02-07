package com.hp.gaia.sts.rest;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hp.gaia.sts.util.DexConnectionManager;
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
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.*;

/**
 * Created by belozovs on 1/20/2016.
 */
@RestController
public class UserLoginController {

    private final static Logger log = LoggerFactory.getLogger(UserLoginController.class);

    @Autowired
    RestTemplate restTemplate;

    ObjectMapper mapper = new ObjectMapper();

    private final static Map<String, String> dexConnectionDetails = DexConnectionManager.getInstance().getConnectionDetails();

    private final static String internalDexUrl = dexConnectionDetails.get("internalDexUrl"); //"http://dexworker.skydns.local:5556";
    private final static String externalDexUrl = dexConnectionDetails.get("externalDexUrl"); //"http://gaia.skydns.local:88";
    private final static String discoveryUrl = dexConnectionDetails.get("discoveryUrl"); //internalDexUrl + "/.well-known/openid-configuration";
    private static String tokenEndpointUrl;
    private static String authEndpointUrl;
    private static String jwksUrl;
    private static List<String> algorithms = new ArrayList<>();

    private final static String clientId = "gMJE1WgA1cK2zMCcD7WraL6tDbamP7THRwCTQg-Xn8w=@sts.skydns.local";
    private final static String clientSecret = "Q6KRttECpBvf1bZRbzHHYpjia74udqhUAa4XISnkTPukMsJ7D_MQnF58krMJZv6t8-eQta9U5kZtHWwAzaEs_zCaE_r4pv_h";
    private final static String callbackUrl = "http://gaia.skydns.local:88/sts/callback";

    @PostConstruct
    void init() {
        ResponseEntity<String> openIdConfig = restTemplate.getForEntity(discoveryUrl, String.class);
        JsonNode jsonOpenIdConfig = null;
        try {
            jsonOpenIdConfig = mapper.readTree(openIdConfig.getBody());
        } catch (IOException e) {
            e.printStackTrace();
        }

        jwksUrl = jsonOpenIdConfig.get("jwks_uri").asText().replace(externalDexUrl, internalDexUrl);
        tokenEndpointUrl = jsonOpenIdConfig.get("token_endpoint").asText().replaceAll(externalDexUrl, internalDexUrl);
//        tokenEndpointUrl = internalDexUrl +"/token";
        authEndpointUrl = jsonOpenIdConfig.get("authorization_endpoint").asText();
//        authEndpointUrl = externalDexUrl+"/auth";
        log.debug("authEndpointUrl: " + authEndpointUrl);
        log.debug("tokenEndpointUrl: " + tokenEndpointUrl);
        log.debug("jwksUrl: " + jwksUrl);

        if (jsonOpenIdConfig.get("id_token_signing_alg_values_supported").isArray()) {
            for (int i = 0; i < jsonOpenIdConfig.get("id_token_signing_alg_values_supported").size(); i++) {
                algorithms.add(jsonOpenIdConfig.get("id_token_signing_alg_values_supported").get(i).asText());
            }
            log.debug("token_endpoint_auth_methods_supported (algorithms) found: " + algorithms.size());
        } else {
            algorithms.add(jsonOpenIdConfig.get("id_token_signing_alg_values_supported").asText());
            log.debug("Single token_endpoint_auth_methods_supported (algorithms) found and it is a String - suspicious, expected to be Array");
        }

    }


    @RequestMapping("/login")
    @ResponseBody
    void login(HttpServletResponse httpServletResponse) {
        httpServletResponse.setHeader("Location", authEndpointUrl + "?client_id=" + clientId + "&redirect_uri=" + callbackUrl + "&response_type=code&scope=openid+email+profile&state=");
        httpServletResponse.setStatus(302);
    }

    @RequestMapping(value = "/callback")
    @ResponseBody
    void loginCallback(@RequestParam("code") String code, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {


        byte[] clientBytes = (clientId + ":" + clientSecret).getBytes();
        String authValue = Base64.getEncoder().encodeToString(clientBytes);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("Authorization", "Basic " + authValue);

        System.out.println("authValue=" + authValue);
        System.out.println("code=" + code);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("grant_type", "authorization_code");
        map.add("code", code);
        map.add("scope", "openid+email+profile");
        map.add("redirect_uri", callbackUrl);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        //RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> dexResponse = restTemplate.postForEntity(tokenEndpointUrl, request, String.class);

        JsonNode jsonDexResponse = null;
        try {
            jsonDexResponse = mapper.readTree(dexResponse.getBody());
        } catch (IOException e) {
            e.printStackTrace();
        }

        httpServletResponse.setHeader("Location", httpServletRequest.getContextPath() + "/welcome.jsp");
        Cookie cookie = createIdentityTokenCookie(jsonDexResponse.get("id_token").asText(), null);
/*
        Cookie cookie = new Cookie("it", jsonDexResponse.get("id_token").asText());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setDomain("skydns.local");
        cookie.setSecure(false);
*/
        httpServletResponse.addCookie(cookie);
        httpServletResponse.setStatus(302);


/**
 *      POST /token HTTP/1.1
 *      Authorization: Basic aW5aWF9sdlR6X0taaTI5d0JnOS1fY09wa09MbUVvZFUyT2J3MjNNSjdlND1AMTI3LjAuMC4xOjFvT0RMTUdsN1pGNW1TdmdFWFhUdDVhTlZLazY3VExQNTBoakFtSEtyN1RJckJVTkl6eTRkZUcwV1VPTkJCMFVVVUhlMXZZcUp0ZzZXS3N3bkNGZFlkc1M3UlRFc09FYQ==
 *      Content-Type: application/x-www-form-urlencoded
 *      client_id=inZX_lvTz_KZi29wBg9-_cOpkOLmEodU2Obw23MJ7e4%3D%40127.0.0.1&client_secret=1oODLMGl7ZF5mSvgEXXTt5aNVKk67TLP50hjAmHKr7TIrBUNIzy4deG0WUONBB0UUUHe1vYqJtg6WKswnCFdYdsS7RTEsOEa&code=7HBah9lMe3I%3D&grant_type=authorization_code&redirect_uri=http%3A%2F%2F127.0.0.1%3A5555%2Fcallback&scope=openid+email+profile
 */


    }

    @RequestMapping("/verify")
    @ResponseBody
    ResponseEntity<String> verify(HttpServletRequest request, HttpServletResponse response) {

        boolean isVerified = false;

        Cookie cookieToDecode = null;

        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("it")) {
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
        } else {
            System.out.println("Token verification has failed for " + cookieToDecode.getValue());
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/logout")
    @ResponseBody
    void logout(HttpServletRequest request, HttpServletResponse response){

        if(request.getCookies() != null){
            for(Cookie cookie : request.getCookies()){
                if(cookie.getName().equals("it")){
                    response.addCookie(createIdentityTokenCookie(null, 0));
                }
            }
        }

        response.setHeader("Location", request.getContextPath() + "/landing.jsp");
        response.setStatus(302);
    }


    private boolean verifyIdToken(Cookie cookie) {

        String stringIdToken = cookie.getValue();
        try {
            SignedJWT token = SignedJWT.parse(stringIdToken);

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

        } catch (ParseException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JOSEException e) {
            e.printStackTrace();
        }

        return false;
    }

    private JsonNode decodeIdToken(Cookie cookieToDecode) throws RuntimeException {

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
                throw new RuntimeException("bad issue: " + claims.get("iss").toString());
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
            if(claims.get("sub").toString().isEmpty()){
                throw new RuntimeException("no sub provided");
            }
            jsonClaims.put("sub", claims.get("sub").toString());

        } catch (ParseException e) {
            e.printStackTrace();
        }


        return jsonClaims;
    }

    private boolean isValidEmail(String emailAddress) {
        if (StringUtils.isEmpty(emailAddress)) {
            return false;
        }
        if (!emailAddress.contains("@")) {
            return false;
        }
        return true;
    }

    private Cookie createIdentityTokenCookie(String value, Integer expiration){
        Cookie cookie = new Cookie("it", value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setDomain("skydns.local");
        cookie.setSecure(false);
        if(expiration!=null) {
            cookie.setMaxAge(expiration);
        }
        return cookie;
    }

}
