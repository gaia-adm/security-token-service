package com.hp.gaia.sts.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.gaia.sts.dao.TenantDao;
import com.hp.gaia.sts.dto.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.io.IOException;

/**
 * Created by belozovs on 6/25/2015.
 *
 */
@Deprecated
@Controller
public class TenantController {

    private final static Logger logger = LoggerFactory.getLogger(TenantController.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TenantDao tenantDao;

    @RequestMapping(value = "/tenant", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<String> createTenant(@RequestBody String jsonTenant) {

        try {
            Tenant tenant = objectMapper.readValue(jsonTenant, Tenant.class);
            tenantDao.save(tenant);
            Tenant created = tenantDao.getTenantByAdminName(tenant.getAdminUserName());
            if (created != null) {
                logger.info("Tenant {} created successfully", created.getTenantId());
            }
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (DataAccessException dae) {
            dae.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.TEXT_PLAIN).body("Failed to create tenant, see logs for more details");
        } catch (IOException ioe) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.TEXT_PLAIN).body("Bad input received");
        }
    }

    @RequestMapping(value = "/tenant", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> getTenantByName(@RequestParam("user") String adminUserName) {

        try {
            Tenant tenant = tenantDao.getTenantByAdminName(adminUserName);
            return new ResponseEntity<>(objectMapper.writeValueAsString(tenant), HttpStatus.OK);
        } catch (DataAccessException dae) {
            dae.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.TEXT_PLAIN).body("Tenant not found with the name provided");
        } catch (JsonProcessingException jpe) {
            jpe.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.TEXT_PLAIN).body("Failed to return tenant data");
        }
    }

    @RequestMapping(value = "/tenant/{tenantId}", method = RequestMethod.DELETE)
    @ResponseBody
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTenantByName(@PathVariable("tenantId") Long tenantId) {

        try {
            tenantDao.deleteById(tenantId);
            logger.info("Tenant {} deleted successfully", tenantId);
        } catch (DataAccessException dae) {
            logger.error("Failed to delete tenant {}", tenantId);
        }
    }

}
