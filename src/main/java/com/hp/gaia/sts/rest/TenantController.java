package com.hp.gaia.sts.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.gaia.sts.dao.TenantDao;
import com.hp.gaia.sts.dto.Tenant;
import com.sun.java.browser.dom.DOMAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Created by belozovs on 6/25/2015.
 */
@Controller
public class TenantController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    TenantDao tenantDao;

    @RequestMapping(value = "/tenant", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<String> createTenant(@RequestBody String jsonTenant) {

        try {
            Tenant tenant = objectMapper.readValue(jsonTenant, Tenant.class);
            tenantDao.save(tenant);
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (DataAccessException dae) {
            dae.printStackTrace();
            return new ResponseEntity<>("Failed to create tenant, see logs for more details", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IOException ioe) {
            return new ResponseEntity<>("Bad input received", HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/tenant", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> getTenantByName(@RequestParam("user") String adminUserName) {

        try {
            Tenant tenant = tenantDao.getTenantByAdminName(adminUserName);
            return new ResponseEntity<>(objectMapper.writeValueAsString(tenant), HttpStatus.OK);
        } catch (DataAccessException dae){
            dae.printStackTrace();
            return new ResponseEntity<>("Tenant not found with the name provided", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (JsonProcessingException jpe) {
            jpe.printStackTrace();
            return new ResponseEntity<>("Failed to return tenant data", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/tenant/{tenantId}", method = RequestMethod.DELETE)
    @ResponseBody
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTenantByName(@PathVariable("tenantId") Integer tenantId){

        try{
            tenantDao.deleteById(tenantId);
        } catch (EmptyResultDataAccessException erdae) {
            //do nothing
        }

    }

}
