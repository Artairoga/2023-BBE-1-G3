package com.hotel.continental.ws.core.rest;

import com.hotel.continental.api.core.service.IAccessCardService;
import com.ontimize.jee.server.rest.ORestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accesscard")
public class AccessCardRestController extends ORestController<IAccessCardService> {
    @Autowired
    private IAccessCardService accessCardSrv;

    @Override
    public IAccessCardService getService() {
        return this.accessCardSrv;
    }
}
