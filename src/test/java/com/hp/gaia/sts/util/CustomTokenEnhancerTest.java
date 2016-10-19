package com.hp.gaia.sts.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.gaia.sts.dao.TenantDao;
import com.hp.gaia.sts.dto.Tenant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.UnapprovedClientAuthenticationException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

/**
 * Created by belozovs on 6/28/2015.
 */
public class CustomTokenEnhancerTest {

    private final static String TENANT_ID_PROP_NAME = "tenantId";
    private final static Long TENANT_ID = 100L;
    private final static String TENANT_ADMIN_USER_NAME = "admin@hp.com";

    private final static String ACCOUNT_DATA = "{\"id\":100,\"name\":\"Account-1\",\"description\":\"My first account\",\"icon\":null,\"enabled\":true,\"users\":[{\"firstName\":\"Gaia\",\"lastName\":\"User\",\"id\":100,\"role_ids\":[3,1],\"role_names\":[\"Member\",\"Account Admin\"],\"emails\":[\"gaiaadmuser@gmail.com\"]}]}";

    private Map<String, Object> additionalInfo = new HashMap<>();
    private OAuth2AccessToken accessToken = new DefaultOAuth2AccessToken("myvalue");

    private ClientDetails clientDetails;
    private ClientDetailsService clientDetailsService;
    private TenantDao tenantDao;
    private OAuth2Authentication authentication;

    private RestTemplate restTemplate;
    private ObjectMapper mapper = new ObjectMapper();
    private final static IDPConnectManager idcm = AcmConnectionManager.getInstance();

    private CustomTokenEnhancer enhancer;


    @Before
    public void setUp() throws Exception {
        additionalInfo.put(TENANT_ID_PROP_NAME, TENANT_ID);

        authentication = createNiceMock(OAuth2Authentication.class);
        clientDetails = createNiceMock(BaseClientDetails.class);
        clientDetailsService = createNiceMock(ClientDetailsService.class);
        restTemplate = createNiceMock(RestTemplate.class);

        enhancer = new CustomTokenEnhancer();
        ReflectionTestUtils.setField(enhancer, "clientDetailsService", clientDetailsService, ClientDetailsService.class);
        ReflectionTestUtils.setField(enhancer, "restTemplate", restTemplate, RestTemplate.class);
        ReflectionTestUtils.setField(enhancer, "mapper", mapper, ObjectMapper.class);

    }

    @Test
    public void testEnhance() throws Exception {

        ResponseEntity<String> resp = new ResponseEntity<>(ACCOUNT_DATA, HttpStatus.OK);
        String acmUrl = idcm.getConnectionDetails().get("internalProtocol") + "://" + idcm.getConnectionDetails().get("internalIdmServer") + ":" + idcm.getConnectionDetails().get("internalPort") + "/acms/api/accounts/" + TENANT_ID;

        Tenant tenant = new Tenant(TENANT_ADMIN_USER_NAME);
        tenant.setTenantId(TENANT_ID);
        expect(authentication.getPrincipal()).andReturn("justDummyString").once();
        expect(clientDetailsService.loadClientByClientId(anyString())).andReturn(clientDetails).once();
        expect(clientDetails.getAdditionalInformation()).andReturn(additionalInfo).once();
        expect(restTemplate.getForEntity(acmUrl, String.class)).andReturn(resp);
        replay(authentication, clientDetailsService, clientDetails, restTemplate);
        enhancer.enhance(accessToken, authentication);
        verify(authentication, clientDetailsService, clientDetails);

        assertEquals(TENANT_ID, accessToken.getAdditionalInformation().get(TENANT_ID_PROP_NAME));
    }

    @Test(expected = UnapprovedClientAuthenticationException.class)
    public void testEnhanceNoTenant() throws Exception {
        additionalInfo.put(TENANT_ID_PROP_NAME, null);
        expect(authentication.getPrincipal()).andReturn("justDummyString").once();
        expect(clientDetailsService.loadClientByClientId(anyString())).andReturn(clientDetails).once();
        expect(clientDetails.getAdditionalInformation()).andReturn(additionalInfo).once();
        replay(authentication, clientDetailsService, clientDetails);
        enhancer.enhance(accessToken, authentication);
        verify(authentication, clientDetailsService, clientDetails);

    }

}