package com.hp.gaia.sts.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.*;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * Created by belozovs on 6/15/2015.
 *
 */
@Controller
public class ClientDetailsController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    ClientDetailsService clientDetailsService;

    @RequestMapping(value = "/oauth/client/{id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> getClientById(@PathVariable("id") String clientId) throws JsonProcessingException {

        try {
            ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);
            return new ResponseEntity<>(objectMapper.writeValueAsString(clientDetails), HttpStatus.OK);
        } catch (NoSuchClientException nsce) {
            nsce.printStackTrace();
            return new ResponseEntity<>("Client " + clientId + " not found", HttpStatus.NOT_FOUND);
        }

    }

    @RequestMapping(value = "/oauth/client", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String getAllClients() throws JsonProcessingException {

        List<ClientDetails> clientDetails = ((ClientRegistrationService) clientDetailsService).listClientDetails();

        return objectMapper.writeValueAsString(clientDetails);
    }

    @RequestMapping(value = "/oauth/client", method = RequestMethod.POST, consumes = "application/json")
    @ResponseBody
    public ResponseEntity<String> addClient(@RequestBody String clientDetailsString) throws IOException {

        ClientDetails clientDetails = objectMapper.readValue(clientDetailsString, BaseClientDetails.class);
        try {
            ((ClientRegistrationService) clientDetailsService).addClientDetails(clientDetails);
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (ClientAlreadyExistsException caee) {
            caee.printStackTrace();
            return new ResponseEntity<>(caee.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @RequestMapping(value = "/oauth/client/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClientById(@PathVariable("id") String clientId) throws JsonProcessingException {

        try {
            ((ClientRegistrationService) clientDetailsService).removeClientDetails(clientId);
        } catch (NoSuchClientException nsce) {//do nothing
        }

    }
}
