package com.hp.gaia.sts.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by belozovs on 6/15/2015.
 */
@Controller
public class CustomTokenController {

    @Autowired
    private DefaultTokenServices defaultTokenServices;

    public void setDefaultTokenServices(DefaultTokenServices defaultTokenServices) {
        this.defaultTokenServices = defaultTokenServices;
    }

    public DefaultTokenServices getDefaultTokenServices() {
        return defaultTokenServices;
    }

    @RequestMapping(value = "oauth/token/revoke", method = RequestMethod.DELETE)
    @ResponseBody
    public void create(@RequestParam("token") String value) throws InvalidClientException {
        defaultTokenServices.revokeToken(value);
    }
}
