package com.hp.gaia.sts.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * Created by boris on 10/25/16.
 *
 */
public class AcmConnectionProxyTest {

    private RestTemplate restTemplate;
    private ObjectMapper mapper = new ObjectMapper();

    private final String token = "eyJhbGciOiQLXo81NiIsInR5cCI6IkpXVCJ9.eyJpZAF5LHAwLCJpYXQiJ8Sxxc0MDE2MzcsImV4cCI6MTUwODkzNzYzN30.x00HioMx4ROEbP4Msa3A4qysRIhw9E5yFGzenEK3I2Y";

    private final String tenantData = "{\"id\":100,\"name\":\"Account-1\",\"description\":\"My first account\",\"icon\":null,\"enabled\":true,\"invitations\":[{\"id\":1,\"uuid\":\"7487afb2-0956-4bc1-a724-398658117ba0\",\"account_id\":100,\"email\":\"gaiaadmuser@gmail.com\",\"invited_role_ids\":[3],\"date_invited\":\"2016-10-09T08:37:42.207Z\",\"date_accepted\":\"2016-10-09T08:39:16.794Z\"}],\"users\":[{\"firstName\":\"Gaia\",\"lastName\":\"User\",\"id\":100,\"role_ids\":[3,1],\"role_names\":[\"Member\",\"Account Admin\"],\"emails\":[\"gaiaadmuser@gmail.com\",\"gaiaadmuser@gmail.com\"],\"_pivot_user_id\":100,\"_pivot_account_id\":100}]}";
    private final String tenantId = "100";

    private final String userWithNoAccount = "{\"id\":10,\"firstName\":\"Gaia\",\"lastName\":\"Team\",\"isSuperuser\":true,\"isAdmin\":true,\"emails\":[\"gaiaadmservice@gmail.com\"],\"accounts\":[]}";
    private final String userWithAccount = "{\"id\":100,\"firstName\":\"Gaia\",\"lastName\":\"User\",\"isSuperuser\":false,\"isAdmin\":false,\"emails\":[\"gaiaadmuser@gmail.com\"],\"accounts\":[{\"account_id\":100,\"name\":\"Account-1\",\"role_ids\":[3,1],\"role_names\":[\"Member\",\"Account Admin\"],\"_pivot_user_id\":100,\"_pivot_account_id\":100}]}";
    private final String userId = "100";

    private AcmConnectionProxy acmConnectionProxy;

    @Before
    public void setUp() throws Exception {

        restTemplate = createNiceMock(RestTemplate.class);
        acmConnectionProxy = new AcmConnectionProxy();
        ReflectionTestUtils.setField(acmConnectionProxy, "restTemplate", restTemplate, RestTemplate.class);
        ReflectionTestUtils.setField(acmConnectionProxy, "om", mapper, ObjectMapper.class);

    }

    @Test
    //good - any user
    public void userVerification() throws Exception {

        ResponseEntity<String> resp = new ResponseEntity<>(userWithAccount, HttpStatus.OK);

        expect(restTemplate.exchange(anyString(), anyObject(), anyObject(), eq(String.class))).andReturn(resp);
        replay(restTemplate);

        String result = acmConnectionProxy.userVerification(token, userId);
        verify(restTemplate);

        assertEquals(userWithAccount, result);
    }

    @Test
    //still valid - site admin
    public void userVerificationNoAccount() throws Exception {

        ResponseEntity<String> resp = new ResponseEntity<>(userWithNoAccount, HttpStatus.OK);

        expect(restTemplate.exchange(anyString(), anyObject(), anyObject(), eq(String.class))).andReturn(resp);
        replay(restTemplate);

        String result = acmConnectionProxy.userVerification(token, userId);
        verify(restTemplate);

        assertEquals(userWithNoAccount,result);
    }

    @Test
    //self evaluation - no user id provided
    public void userVerificationSelf() throws Exception {

        ResponseEntity<String> resp = new ResponseEntity<>(userWithAccount, HttpStatus.OK);

        expect(restTemplate.exchange(anyString(), anyObject(), anyObject(), eq(String.class))).andReturn(resp);
        replay(restTemplate);

        String result = acmConnectionProxy.userVerification(token, null);
        verify(restTemplate);

        assertEquals(userWithAccount, result);
    }

    @Test
    //bad token - does not fit the user id
    public void userVerificationSelfBadToken() throws Exception {

        String unauthorizedResponse = "{\"name\":\"AuthenticationError\",\"message\":\"Unauthorized\"}";
        ResponseEntity<String> resp = new ResponseEntity<>(unauthorizedResponse, HttpStatus.UNAUTHORIZED);

        expect(restTemplate.exchange(anyString(), anyObject(), anyObject(), eq(String.class))).andReturn(resp);
        replay(restTemplate);

        String result = acmConnectionProxy.userVerification(token, null);
        verify(restTemplate);

        assertNull(result);
    }

    @Test
    //no user id somehow in the AcM response
    public void userVerificationSelfBadAcmResponseNoId() throws Exception {

        String badAcmResponseNoId = "{\"firstName\":\"Gaia\",\"lastName\":\"User\",\"isSuperuser\":false,\"isAdmin\":false,\"emails\":[\"gaiaadmuser@gmail.com\"],\"accounts\":[{\"account_id\":100,\"name\":\"Account-1\",\"role_ids\":[3,1],\"role_names\":[\"Member\",\"Account Admin\"],\"_pivot_user_id\":100,\"_pivot_account_id\":100}]}";
        ResponseEntity<String> resp = new ResponseEntity<>(badAcmResponseNoId, HttpStatus.OK);

        expect(restTemplate.exchange(anyString(), anyObject(), anyObject(), eq(String.class))).andReturn(resp);
        replay(restTemplate);

        String result = acmConnectionProxy.userVerification(token, null);
        verify(restTemplate);

        assertNull(result);
    }

    @Test
    //empty response from AcM somehow - really nothing
    public void userVerificationEmptyAcmResponse() throws Exception {

        ResponseEntity<String> resp = new ResponseEntity<>("", HttpStatus.OK);

        expect(restTemplate.exchange(anyString(), anyObject(), anyObject(), eq(String.class))).andReturn(resp);
        replay(restTemplate);

        String result = acmConnectionProxy.userVerification(token, null);
        verify(restTemplate);

        assertNull(result);
    }

    @Test
    //empty response from AcM somehow - empty JSON
    public void userVerificationEmptyJsonAcmResponse() throws Exception {

        ResponseEntity<String> resp = new ResponseEntity<>(NullNode.instance.asText(), HttpStatus.OK);

        expect(restTemplate.exchange(anyString(), anyObject(), anyObject(), eq(String.class))).andReturn(resp);
        replay(restTemplate);

        String result = acmConnectionProxy.userVerification(token, null);
        verify(restTemplate);

        assertNull(result);
    }

    @Test
    //no token provided
    public void userVerificationNoToken() throws Exception {

        String result = acmConnectionProxy.userVerification(null, userId);

        assertNull(result);
    }

    @Test
    public void tenantExistenceVerification() throws Exception {

        ResponseEntity<String> resp = new ResponseEntity<>(tenantData, HttpStatus.OK);

        expect(restTemplate.exchange(anyString(), anyObject(), anyObject(), eq(String.class))).andReturn(resp);
        replay(restTemplate);
        boolean result = acmConnectionProxy.tenantExistenceVerification(token, tenantId);
        verify(restTemplate);

        assertEquals("Should be true", true, result);
    }

    @Test
    public void tenantExistenceVerificationNoToken() throws Exception {

        boolean result = acmConnectionProxy.tenantExistenceVerification(null, tenantId);

        assertFalse("Fast fail to fail without identity token provided", result);
    }

    @Test
    public void tenantExistenceVerificationNoTenantId() throws Exception {

        boolean result = acmConnectionProxy.tenantExistenceVerification(token, null);

        assertFalse("Fast fail to fail without tenantId provided", result);
    }

    @Test
    public void tenantExistenceVerificationBadAcmResponse() throws Exception {

        ResponseEntity<String> resp = new ResponseEntity<>("foo", HttpStatus.SERVICE_UNAVAILABLE);

        expect(restTemplate.exchange(anyString(), anyObject(), anyObject(), eq(String.class))).andReturn(resp);
        replay(restTemplate);
        boolean result = acmConnectionProxy.tenantExistenceVerification(token, tenantId);
        verify(restTemplate);

        assertEquals("Should be false", false, result);
    }
}