package com.hp.gaia.sts.util;

import com.hp.gaia.sts.dao.TenantDao;
import com.hp.gaia.sts.dao.TenantDaoImpl;
import com.hp.gaia.sts.dto.Tenant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.UnapprovedClientAuthenticationException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

/**
 * Created by belozovs on 6/28/2015.
 *
 */
public class CustomTokenEnhancerTest {

    private final static String TENANT_ID_PROP_NAME = "tenantId";
    private final static Long TENANT_ID = 12345L;
    private final static String TENANT_ADMIN_USER_NAME = "admin@hp.com";

    private Map<String, Object> additionalInfo = new HashMap<>();
    private OAuth2AccessToken accessToken = new DefaultOAuth2AccessToken("myvalue");

    private ClientDetails clientDetails;
    private ClientDetailsService clientDetailsService;
    private TenantDao tenantDao;
    private OAuth2Authentication authentication;

    private CustomTokenEnhancer enhancer;


    @Before
    public void setUp() throws Exception {
        additionalInfo.put(TENANT_ID_PROP_NAME, TENANT_ID);

        authentication = createNiceMock(OAuth2Authentication.class);
        clientDetails = createNiceMock(BaseClientDetails.class);
        clientDetailsService = createNiceMock(ClientDetailsService.class);
        tenantDao = createNiceMock(TenantDaoImpl.class);

        enhancer = new CustomTokenEnhancer();
        ReflectionTestUtils.setField(enhancer, "clientDetailsService", clientDetailsService, ClientDetailsService.class);
        ReflectionTestUtils.setField(enhancer, "tenantDao", tenantDao, TenantDao.class);
    }

    @Test
    public void testEnhance() throws Exception {
        Tenant tenant = new Tenant(TENANT_ADMIN_USER_NAME);
        tenant.setTenantId(TENANT_ID);
        expect(authentication.getPrincipal()).andReturn("justDummyString").once();
        expect(clientDetailsService.loadClientByClientId(anyString())).andReturn(clientDetails).once();
        expect(clientDetails.getAdditionalInformation()).andReturn(additionalInfo).once();
        expect(tenantDao.getTenantById(TENANT_ID)).andReturn(tenant).once();
        replay(authentication, clientDetailsService, clientDetails, tenantDao);
        enhancer.enhance(accessToken, authentication);
        verify(authentication, clientDetailsService, clientDetails, tenantDao);

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