/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.ingenico.ogone.direct.webhook.controllers;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.ingenico.direct.domain.WebhooksEvent;
import com.ingenico.ogone.direct.facade.IngenicoWebhookFacade;


/**
 * Sample Controller
 */
@Controller
@RequestMapping(value = "/webhook")
public class IngenicoWebhookController {
    private final static Logger LOGGER = LoggerFactory.getLogger(IngenicoWebhookController.class);

    @Resource(name = "ingenicoWebhookFacade")
    private IngenicoWebhookFacade ingenicoWebhookFacade;

    @RequestMapping(method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public String onPostReceive(@RequestHeader("X-GCS-KeyId") String keyId,
                                @RequestHeader("X-GCS-Signature") String signature,
                                HttpServletRequest request) throws IOException {

        String bodyString = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
        final WebhooksEvent webhooksEvent = ingenicoWebhookFacade.retrieveWebhooksEvent(bodyString, keyId, signature);
        LOGGER.debug("[INGENICO] webhookevent received : {}", bodyString);

        ingenicoWebhookFacade.validateWebhooksEvent(webhooksEvent);
        ingenicoWebhookFacade.saveWebhooksEvent(webhooksEvent);

        return "ok";
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String onGetReceive(@RequestHeader("X-GCS-Webhooks-Endpoint-Verification") String verification,
                               HttpServletRequest request) {
        return verification;
    }

}
