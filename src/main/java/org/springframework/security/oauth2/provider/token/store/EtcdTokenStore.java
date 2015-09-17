package org.springframework.security.oauth2.provider.token.store;

import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeysResponse;
import org.apache.commons.codec.binary.Base64;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.common.util.SerializationUtils;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.AuthenticationKeyGenerator;
import org.springframework.security.oauth2.provider.token.DefaultAuthenticationKeyGenerator;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Component;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Created by belozovs on 9/10/2015.
 * TokenStore with persistence in etcd
 * Based on standard JdbcTokenStore and only supports client_credentials grant type
 */
@Component
public class EtcdTokenStore implements TokenStore {

    private final String AT_PATH="accesstokens/";

    private AuthenticationKeyGenerator authenticationKeyGenerator = new DefaultAuthenticationKeyGenerator();


    public OAuth2Authentication readAuthentication(OAuth2AccessToken token) {
        return readAuthentication(token.getValue());
    }

    public OAuth2Authentication readAuthentication(String token) {

        OAuth2Authentication authentication = null;

        try(EtcdClient etcdClient = new EtcdClient()) {
            EtcdKeysResponse response = etcdClient.get(AT_PATH+extractTokenKey(token)).send().get();
            String[] elements = response.node.value.split(", ");
            authentication = deserializeAuthentication(Base64.decodeBase64(elements[5].split("=")[1]));
        } catch (IOException | TimeoutException  | EtcdException e) {
            e.printStackTrace();
        }

        return authentication;
    }

    //Store access token; if exists, delete and recreate - the same logic as in JdbcTokenStore
    public void storeAccessToken(OAuth2AccessToken oToken, OAuth2Authentication oAuthentication) {

        String refreshToken = null;
        if (oToken.getRefreshToken() != null) {
            refreshToken = oToken.getRefreshToken().getValue();
        }

        String token_id = extractTokenKey(oToken.getValue());
        String token = Base64.encodeBase64String(serializeAccessToken(oToken));
        String authentication_id = authenticationKeyGenerator.extractKey(oAuthentication);
        String user_name = oAuthentication.isClientOnly() ? null : oAuthentication.getName();
        String client_id = oAuthentication.getOAuth2Request().getClientId();
        String authentication = Base64.encodeBase64String(serializeAuthentication(oAuthentication));
        String refresh_token = extractTokenKey(refreshToken);
        String value = "token_id=" + token_id + ", token=" + token + ", authentication_id=" + authentication_id + ", user_name=" + user_name + ", client_id=" + client_id + ", authentication=" + authentication + ", refresh_token=" + refresh_token;


        //delete, if existing
        if (readAccessToken(oToken.getValue()) != null) {
            removeAccessToken(oToken.getValue());
        }
        //and recreate
        try(EtcdClient etcdClient = new EtcdClient()){
            //should be post, actually
            EtcdKeysResponse existenceResponce = etcdClient.put(AT_PATH + token_id, value).send().get();
            System.out.println(existenceResponce.node);

        } catch (IOException | TimeoutException  | EtcdException e) {
            e.printStackTrace();
        }

    }

    //returns token or null, if not found
    public OAuth2AccessToken readAccessToken(String tokenValue) {

        OAuth2AccessToken token = null;

        try(EtcdClient etcdClient = new EtcdClient()){

            EtcdKeysResponse existenceResponce = etcdClient.get(AT_PATH + extractTokenKey(tokenValue)).send().get();
            String[] elements = existenceResponce.node.value.split(",");
            token = deserializeAccessToken(Base64.decodeBase64(elements[1].split("=")[1]));
            System.out.println(existenceResponce.node);

        } catch (IOException | TimeoutException  | EtcdException e) {
            e.printStackTrace();
        }

        return token;
    }

    //recreate access token, if already exists for provided authentication
    public OAuth2AccessToken getAccessToken(OAuth2Authentication authentication) {

        OAuth2AccessToken accessToken = null;


        String key = authenticationKeyGenerator.extractKey(authentication);
        try (EtcdClient etcdClient = new EtcdClient()){
            EtcdResponsePromise<EtcdKeysResponse> promise = etcdClient.getDir(AT_PATH).recursive().send();
            EtcdKeysResponse response = promise.get();
            List<EtcdKeysResponse.EtcdNode> allClients = response.node.nodes;
            for(EtcdKeysResponse.EtcdNode client : allClients){
                if(client.value.contains(key)){
                    String[] elements = client.value.split(",");
                    accessToken = deserializeAccessToken(Base64.decodeBase64(elements[1].split("=")[1]));

                }
            }
            if(accessToken != null && !key.equals(authenticationKeyGenerator.extractKey(readAuthentication(accessToken.getValue())))){
                removeAccessToken(accessToken.getValue());
                storeAccessToken(accessToken, authentication);
            }

        } catch (IOException | TimeoutException  | EtcdException e) {
            e.printStackTrace();
        }

        return accessToken;
    }


    public void removeAccessToken(OAuth2AccessToken token) {
        removeAccessToken(token.getValue());
    }

    public void removeAccessToken(String tokenValue) {

        try(EtcdClient etcdClient = new EtcdClient()){
            etcdClient.delete(AT_PATH + extractTokenKey(tokenValue)).send().get();
        } catch (IOException | TimeoutException  | EtcdException e) {
            e.printStackTrace();
        }
    }

    protected String extractTokenKey(String value) {
        if (value == null) {
            return null;
        }
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available.  Fatal (should be in the JDK).");
        }

        try {
            byte[] bytes = digest.digest(value.getBytes("UTF-8"));
            return String.format("%032x", new BigInteger(1, bytes));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 encoding not available.  Fatal (should be in the JDK).");
        }
    }

    protected byte[] serializeAccessToken(OAuth2AccessToken token) {
        return SerializationUtils.serialize(token);
    }

    protected byte[] serializeAuthentication(OAuth2Authentication authentication) {
        return SerializationUtils.serialize(authentication);
    }

    protected OAuth2AccessToken deserializeAccessToken(byte[] token) {
        return SerializationUtils.deserialize(token);
    }

    protected OAuth2Authentication deserializeAuthentication(byte[] authentication) {
        return SerializationUtils.deserialize(authentication);
    }

    public void storeRefreshToken(OAuth2RefreshToken refreshToken, OAuth2Authentication authentication) {
        throw new NotImplementedException();
    }

    public OAuth2RefreshToken readRefreshToken(String tokenValue) {
        throw new NotImplementedException();
    }

    public OAuth2Authentication readAuthenticationForRefreshToken(OAuth2RefreshToken token) {
        throw new NotImplementedException();
    }

    public void removeRefreshToken(OAuth2RefreshToken token) {
        throw new NotImplementedException();
    }

    public void removeAccessTokenUsingRefreshToken(OAuth2RefreshToken refreshToken) {
        throw new NotImplementedException();
    }

    public Collection<OAuth2AccessToken> findTokensByClientIdAndUserName(String clientId, String userName) {
        throw new NotImplementedException();
    }

    public Collection<OAuth2AccessToken> findTokensByClientId(String clientId) {
        throw new NotImplementedException();
    }
}
