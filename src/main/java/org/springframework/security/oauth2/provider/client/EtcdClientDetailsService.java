package org.springframework.security.oauth2.provider.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeysResponse;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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


    private ObjectMapper mapper = new ObjectMapper();
    private final String CD_PATH = "clientdetails/";

    private PasswordEncoder passwordEncoder = NoOpPasswordEncoder.getInstance();

    /**
     * @param passwordEncoder the password encoder to set
     */
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public ClientDetails loadClientByClientId(String clientId) throws ClientRegistrationException {

        ClientDetails clientDetails;
        try (EtcdClient etcdClient = new EtcdClient()) {
            EtcdResponsePromise<EtcdKeysResponse> promise = etcdClient.get(CD_PATH + clientId).send();
            EtcdKeysResponse response = promise.get();
            clientDetails = createClientDetails(response.node.value);
            return clientDetails;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("IOException happened");
        } catch (TimeoutException e) {
            e.printStackTrace();
            throw new RuntimeException("Timeout occurred when tried to get client details for " + clientId);
        } catch (EtcdException e) {
            e.printStackTrace();
            throw new NoSuchClientException("No client details found for client id " + clientId);
        }
    }

    public void addClientDetails(ClientDetails clientDetails) throws ClientAlreadyExistsException {

        try (EtcdClient etcdClient = new EtcdClient()) {
            String cd = Arrays.toString(getFields(clientDetails));
            System.out.println(System.currentTimeMillis() + ": 1");
            EtcdResponsePromise<EtcdKeysResponse> response = etcdClient.put(CD_PATH + clientDetails.getClientId(), cd).prevExist(false).send();
            System.out.println(System.currentTimeMillis() + ": 2");
            response.get();
            System.out.println(System.currentTimeMillis() + ": 3");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create client: " + e.getMessage());
        } catch (TimeoutException e) {
            e.printStackTrace();
            throw new RuntimeException("Timeout occurred when tried to create client");
        } catch (EtcdException e) {
            e.printStackTrace();
            throw new ClientAlreadyExistsException("Client " + clientDetails.getClientId() + " already exists");
        }

    }

    public void updateClientDetails(ClientDetails clientDetails) throws NoSuchClientException {

    }

    public void updateClientSecret(String clientId, String secret) throws NoSuchClientException {

    }

    public void removeClientDetails(String clientId) throws NoSuchClientException {
        try (EtcdClient etcdClient = new EtcdClient()){
            etcdClient.delete(CD_PATH+clientId).send().get();
        } catch (IOException | TimeoutException | EtcdException e) {
            e.printStackTrace();
        }
    }

    public List<ClientDetails> listClientDetails() {

        List<ClientDetails> allClientDetails = new ArrayList<>();

        try (EtcdClient etcdClient = new EtcdClient()) {
            EtcdResponsePromise<EtcdKeysResponse> promise = etcdClient.getDir(CD_PATH).recursive().send();
            EtcdKeysResponse response = promise.get();
            List<EtcdKeysResponse.EtcdNode> allClients = response.node.nodes;
            for(EtcdKeysResponse.EtcdNode client : allClients){
                allClientDetails.add(createClientDetails(client.value));
            }
            return allClientDetails;
        } catch (IOException | TimeoutException | EtcdException e) {
            e.printStackTrace();
            throw new RuntimeException("Change it to something more elegance");
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
}
