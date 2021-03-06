package org.springframework.security.oauth2.provider.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.gaia.sts.util.EtcdClientCreator;
import com.hp.gaia.sts.util.EtcdPaths;
import com.hp.gaia.sts.util.TokenStorageException;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeysResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * Created by belozovs on 9/10/2015.
 * ClientDetailsService implementation using etcd as a persistence layer
 * Based on standard JdbcClientDetailsService and only supports client_credentials grant type
 */
@Component
public class EtcdClientDetailsService implements ClientDetailsService, ClientRegistrationService {

    private final static Logger logger = LoggerFactory.getLogger(EtcdClientDetailsService.class);

    private ObjectMapper mapper = new ObjectMapper();

    private PasswordEncoder passwordEncoder = NoOpPasswordEncoder.getInstance();

    private EtcdClient etcdClient = EtcdClientCreator.getInstance().getEtcdClient();

    @PostConstruct
    void init(){
        ensureEtcdFolder(EtcdPaths.CD_PATH);
        ensureEtcdFolder(EtcdPaths.AT_PATH);
    }

    private void ensureEtcdFolder(String folderName) {
        try {
            EtcdResponsePromise<EtcdKeysResponse> promise = etcdClient.putDir(folderName).send();
            EtcdKeysResponse response = promise.get();
            if(response.node.dir){
                logger.info(folderName + " dir is successfully created");
            } else {
                logger.error(folderName + " is not a dir? Please check! Continue...");
            }
        } catch (IOException | TimeoutException e) {
            logger.error("Cannot create " + folderName + " dir - connectivity problem. You can create it manually, if needed or just restart this service. Continue...");
        } catch (EtcdException e) {
            logger.error(folderName + " dir is probably already existing. Continue...");
        }
    }


    /**
     * @param passwordEncoder the password encoder to set
     */
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public ClientDetails loadClientByClientId(String clientId) throws ClientRegistrationException {

        ClientDetails clientDetails;
        try {
            EtcdResponsePromise<EtcdKeysResponse> promise = etcdClient.get(EtcdPaths.CD_PATH + clientId).send();
            EtcdKeysResponse response = promise.get();
            clientDetails = createClientDetails(response.node.value);
            return clientDetails;
        } catch (IOException e) {
            e.printStackTrace();
            throw new TokenStorageException("IOException happened");
        } catch (TimeoutException e) {
            e.printStackTrace();
            throw new TokenStorageException("Timeout occurred when tried to get client details for " + clientId);
        } catch (EtcdException e) {
            e.printStackTrace();
            throw new NoSuchClientException("No client details found for client id " + clientId);
        }
    }

    public void addClientDetails(ClientDetails clientDetails) throws ClientAlreadyExistsException {

        try {
            String cd = Arrays.toString(getFields(clientDetails));
            System.out.println(System.currentTimeMillis() + ": 1");
            EtcdResponsePromise<EtcdKeysResponse> response = etcdClient.put(EtcdPaths.CD_PATH + clientDetails.getClientId(), cd).prevExist(false).send();
            System.out.println(System.currentTimeMillis() + ": 2");
            response.get();
            System.out.println(System.currentTimeMillis() + ": 3");
        } catch (IOException e) {
            e.printStackTrace();
            throw new TokenStorageException("Failed to create client: " + e.getMessage());
        } catch (TimeoutException e) {
            e.printStackTrace();
            throw new TokenStorageException("Timeout occurred when tried to create client");
        } catch (EtcdException e) {
            e.printStackTrace();
            throw new ClientAlreadyExistsException("Client " + clientDetails.getClientId() + " already exists");
        }

    }

    public void updateClientDetails(ClientDetails clientDetails) throws NoSuchClientException {
        throw new NotImplementedException();
    }

    public void updateClientSecret(String clientId, String secret) throws NoSuchClientException {
        throw new NotImplementedException();
    }

    public void removeClientDetails(String clientId) throws NoSuchClientException {

        try {
            EtcdResponsePromise<EtcdKeysResponse> promise = etcdClient.getDir(EtcdPaths.AT_PATH).send();
            EtcdKeysResponse response = promise.get();
            for (EtcdKeysResponse.EtcdNode node : response.node.nodes) {
                if (compareClient(node.value, clientId)) {
                    etcdClient.delete(node.key).send().get();
                    //we only have single token per client, no more need to iterate
                    break;
                }
            }
        } catch (IOException | TimeoutException | EtcdException e) {
            e.printStackTrace();
            System.out.println("Tokens cleanup for client " + clientId + " failed. It may be OK, if no tokens created yet");
        }

        try {
            etcdClient.delete(EtcdPaths.CD_PATH + clientId).send().get();
        } catch (IOException | TimeoutException | EtcdException e) {
            e.printStackTrace();
            throw new NoSuchClientException("No client found with id = " + clientId);
        }
    }

    public List<ClientDetails> listClientDetails() {

        List<ClientDetails> allClientDetails = new ArrayList<>();

        try {
            EtcdResponsePromise<EtcdKeysResponse> promise = etcdClient.getDir(EtcdPaths.CD_PATH).recursive().send();
            EtcdKeysResponse response = promise.get();
            List<EtcdKeysResponse.EtcdNode> allClients = response.node.nodes;
            for (EtcdKeysResponse.EtcdNode client : allClients) {
                allClientDetails.add(createClientDetails(client.value));
            }
            return allClientDetails;
        } catch (IOException | TimeoutException | EtcdException e) {
            e.printStackTrace();
            throw new TokenStorageException("Change it to something more elegance");
        }
    }


    private Object[] getFields(ClientDetails clientDetails) {
        Object[] fieldsForUpdate = getFieldsForUpdate(clientDetails);
        Object[] fields = new Object[fieldsForUpdate.length + 1];
        System.arraycopy(fieldsForUpdate, 0, fields, 1, fieldsForUpdate.length);
        fields[0] = clientDetails.getClientSecret() != null ? passwordEncoder.encode(clientDetails.getClientSecret())
                : null;
        return fields;
    }

    private Object[] getFieldsForUpdate(ClientDetails clientDetails) {
        String json = null;
        try {
            json = mapper.writeValueAsString(clientDetails.getAdditionalInformation());
        } catch (Exception e) {
            System.out.println("Could not serialize additional information: " + clientDetails);
        }
        return new Object[]{
                clientDetails.getResourceIds() != null ? StringUtils.collectionToDelimitedString(clientDetails
                        .getResourceIds(), ";") : null,
                clientDetails.getScope() != null ? StringUtils.collectionToDelimitedString(clientDetails
                        .getScope(), ";") : null,
                clientDetails.getAuthorizedGrantTypes() != null ? StringUtils
                        .collectionToDelimitedString(clientDetails.getAuthorizedGrantTypes(), ";") : null,
                clientDetails.getRegisteredRedirectUri() != null ? StringUtils
                        .collectionToDelimitedString(clientDetails.getRegisteredRedirectUri(), ";") : null,
                clientDetails.getAuthorities() != null ? StringUtils.collectionToDelimitedString(clientDetails
                        .getAuthorities(), ";") : null, clientDetails.getAccessTokenValiditySeconds(),
                clientDetails.getRefreshTokenValiditySeconds(), json, getAutoApproveScopes(clientDetails),
                clientDetails.getClientId()};
    }

    private String getAutoApproveScopes(ClientDetails clientDetails) {
        if (clientDetails.isAutoApprove("true")) {
            return "true"; // all scopes autoapproved
        }
        Set<String> scopes = new HashSet<>();
        for (String scope : clientDetails.getScope()) {
            if (clientDetails.isAutoApprove(scope)) {
                scopes.add(scope);
            }
        }
        return StringUtils.collectionToCommaDelimitedString(scopes);
    }

    private ClientDetails createClientDetails(String clientDetailsString) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String[] elements = clientDetailsString.replace("[", "").replace("]", "").split(", ");
        BaseClientDetails details = new BaseClientDetails(elements[10], null/*elements[???]*/, elements[2].replace(";", ","), elements[3], elements[5], null/*elements[???]*/);
        details.setClientSecret(elements[0]);
        String json = elements[8];
        if (json != null) {
            try {
                Map<String, Object> additionalInformation = mapper.readValue(json, Map.class);
                details.setAdditionalInformation(additionalInformation);
            } catch (Exception e) {
                System.out.println("Could not decode JSON for additional information: " + details);
                e.printStackTrace();
            }
        }
        return details;
    }

    //Example of value:
    //token_id=8137b9787bd8c8454ee13bce42197621, token=rO0...TIxMTI=, authentication_id=3bf46c7dd13fa987862a1b40379a23cc, user_name=null, client_id=restapp, authentication=rO0...HA=, refresh_token=null
    private boolean compareClient(String value, String clientId) {
        int pos = value.indexOf("client_id");
        if (pos != -1) {
            String substr = value.substring(pos).split(",")[0];
            String tokenClient = substr.split("=")[1];
            if (tokenClient.equals(clientId)) {
                return true;
            }
        }
        return false;
    }
}
