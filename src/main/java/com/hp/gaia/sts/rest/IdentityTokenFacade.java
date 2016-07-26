package com.hp.gaia.sts.rest;

import com.fasterxml.jackson.databind.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by belozovs on 6/28/2016.
 * <p>
 * Transition from gaia.it (identity token received via dex login) to gaia.at (api token) required for administrative operations (webhook configuration API)
 */

@RestController
public class IdentityTokenFacade {

    private static final String identityTokenName = "gaia.it";
    static final ObjectMapper om = new ObjectMapper();

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    UserLoginController ulk;

    @Autowired
    private ClientDetailsService clientDetailsService;

    @PostConstruct
    void init() {
        System.out.println("Facade starting");
    }

    @RequestMapping(value = "/facade/getmyapitoken", method = RequestMethod.GET)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> getMyToken(HttpServletRequest request, HttpServletResponse response) throws IOException {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Cookie identityCookie = null;

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(identityTokenName)) {
                    identityCookie = cookie;
                    break;
                }
            }
        }

        if (identityCookie == null) {
            return createBadResponse(HttpStatus.UNAUTHORIZED, "No identity token found");
        }

        boolean verified = ulk.verifyIdToken(identityCookie);
        if (!verified) {
            return createBadResponse(HttpStatus.FORBIDDEN, "Identity verification fails");
        }
        JsonNode decoded = null;
        try {
            decoded = ulk.decodeIdToken(identityCookie);
        } catch (Exception e) {
            return createBadResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        Long tenantId = fetchTenantFromEmail(decoded.get("email").asText());
        ClientDetails cd = findMyClient(tenantId);
        if (cd == null) {
            return createBadResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Client is missing");
        }

        String uri = "http://localhost:8080/sts/oauth/token?grant_type=client_credentials&client_id={cid}&client_secret={cs}";
        Map<String, String> params = new HashMap<>();
        params.put("cid", cd.getClientId());
        params.put("cs", cd.getClientSecret());
        String token = restTemplate.postForObject(uri, null, String.class, params);
        if (token == null) {
            return createBadResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to obtain token");
        }

        return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(token);
    }

    ResponseEntity<String> createBadResponse(HttpStatus status, String message) {
        Map<String, String> map = new HashMap<>();
        map.put("message", message);
        try {
            return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(om.writeValueAsString(map));
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body("");
        }
    }

    Long fetchTenantFromEmail(String email) {

        return Long.parseLong(email.substring(0, email.indexOf('@')));
    }

    ClientDetails findMyClient(Long tenantId) {
        List<ClientDetails> clientDetails = ((ClientRegistrationService) clientDetailsService).listClientDetails();

        for (ClientDetails cd : clientDetails) {
            if (cd.getAdditionalInformation() != null && cd.getAdditionalInformation().get("tenantId") != null) {

                if(cd.getAdditionalInformation().get("tenantId") instanceof java.lang.Integer){
                    if(tenantId.equals(((Integer) cd.getAdditionalInformation().get("tenantId")).longValue())){
                        return cd;
                    }
                } else {
                    if(tenantId.equals(cd.getAdditionalInformation().get("tenantId"))){
                        return cd;
                    }
                }
            }
        }
        return null;


/*        Optional<ClientDetails> found = clientDetails.stream()
                .filter(cd -> cd.getAdditionalInformation()!=null && tenantId.equals(cd.getAdditionalInformation().get("tenantId")))
                .findFirst();
        if (found.isPresent()) {
            return found.get();
        } else {
            return null;
        }*/

    }

}
