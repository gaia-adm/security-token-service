package com.hp.gaia.sts.util;

import com.hp.gaia.sts.dao.TenantDao;
import com.hp.gaia.sts.dto.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.UnapprovedClientAuthenticationException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by belozovs on 6/14/2015.
 *
 */
public class CustomTokenEnhancer implements TokenEnhancer {

    @Autowired
    ClientDetailsService clientDetailsService;
    @Autowired
    TenantDao tenantDao;

    @Override
    public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {

        //not found client exception is handled internally in JdbcClientDetailsService
        ClientDetails clientDetails = clientDetailsService.loadClientByClientId(authentication.getPrincipal().toString());
        Object tenantId = clientDetails.getAdditionalInformation().get("tenantId");
        if(tenantId == null){
            System.out.println("Client cannot be used, no tenantId set; client should be re-created: " + authentication.getPrincipal());
            throw new UnapprovedClientAuthenticationException("Client configuration is wrong, the token cannot be created");
        }

        Tenant tenant = tenantDao.getTenantById((Integer) tenantId);

        Map<String, Object> map = new HashMap<>();
        map.put("tenantId", tenant.getTenantId());
        map.put("tenantName", tenant.getTenantName());
        map.put("tenantDbName", tenant.getTenantDbName());
        map.put("createdAt", new Date());

        ((DefaultOAuth2AccessToken) accessToken).setAdditionalInformation(map);

        return accessToken;
    }
}
