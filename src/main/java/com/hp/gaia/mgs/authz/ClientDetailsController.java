package com.hp.gaia.mgs.authz;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

/**
 * Created by belozovs on 6/15/2015.
 */
@Controller
public class ClientDetailsController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    ClientDetailsService clientDetailsService;

    @RequestMapping(value = "/oauth/client/{id}", method = RequestMethod.GET)
    @ResponseBody
    public String getClientById(@PathVariable("id") String clientId) throws JsonProcessingException {

        ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);

        return objectMapper.writeValueAsString(clientDetails);
    }

    @RequestMapping(value = "/oauth/client", method = RequestMethod.GET)
    @ResponseBody
    public String getAllClients() throws JsonProcessingException {

        List<ClientDetails> clientDetails = ((JdbcClientDetailsService)clientDetailsService).listClientDetails();

        return objectMapper.writeValueAsString(clientDetails);
    }

    @RequestMapping(value = "/oauth/client", method = RequestMethod.POST)
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public void addClient(@RequestBody String clientDetailsString) throws IOException {

        ClientDetails clientDetails = objectMapper.readValue(clientDetailsString, BaseClientDetails.class);
        ((JdbcClientDetailsService)clientDetailsService).addClientDetails(clientDetails);
    }

    @RequestMapping(value = "/oauth/client/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClientById(@PathVariable("id") String clientId) throws JsonProcessingException {

        ((JdbcClientDetailsService)clientDetailsService).removeClientDetails(clientId);

    }
}
