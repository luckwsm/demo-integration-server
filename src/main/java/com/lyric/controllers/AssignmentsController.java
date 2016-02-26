package com.lyric.controllers;

import com.lyric.AssignmentService;
import com.lyric.SecurityService;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;

/**
 * Created by amadden on 1/29/16.
 */
public class AssignmentsController {
    private final Vertx vertx;
    private final SecurityService securityService;

    private final AssignmentService assignmentService;

    Logger logger = LoggerFactory.getLogger(AssignmentsController.class.getName());

    public AssignmentsController(Vertx vertx, SecurityService securityService) {
        this.vertx = vertx;
        this.securityService = securityService;

        assignmentService = new AssignmentService();
    }

    public void create(RoutingContext routingContext){

        final HttpServerResponse response = routingContext.response();
        try {
            String signedPayload = securityService.decryptPayload(routingContext.getBodyAsString());


            JsonWebSignature jws = securityService.getJws(signedPayload);

            if(!jws.verifySignature()){
                logger.error("Signature is not Verified");
                response.setStatusCode(500);
                response.end();
                return;
            }

            assignmentService.assign(new JsonObject(jws.getPayload()));

            JsonObject responseObject = new JsonObject().put("test", "test");

            JsonWebSignature signature = securityService.createSignature(responseObject.toString().getBytes());
            String compactSerialization = securityService.encryptPayload(signature, responseObject.toString().getBytes(), "application/json");


            response.end(compactSerialization);
        } catch (JoseException e) {
            logger.error(e);
            response.setStatusCode(500);
            response.setStatusMessage(e.getMessage());
            response.end();
        }
    }

}
