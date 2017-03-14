package com.lyric.controllers;

import com.lyric.AssignmentService;
import com.lyric.SecurityService;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import org.jose4j.jwe.JsonWebEncryption;
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
        HttpServerRequest request = routingContext.request();
        String vendorClientAccountId = request.getParam("id");
        final HttpServerResponse response = routingContext.response();

        boolean useJose = Boolean.parseBoolean(System.getenv("DEFAULT_ASSIGNMENT_JOSE_FLAG"));
        final String body = routingContext.getBodyAsString();

        if(useJose){
            handleEncryptedRequest(vendorClientAccountId, response, body);
            return;
        }

        JsonObject assignment = new JsonObject(body);
        assignmentService.assign(vendorClientAccountId, assignment);

        String responseStatus = assignment.getDouble("advanceAmount") <= 10000 ? "assignment_confirmed" : "assignment_rejected";

        JsonObject responseObject = new JsonObject().put("memberToken", assignment.getString("memberToken"))
                .put("vendorClientAccountId", assignment.getString("vendorClientAccountId")).put("advanceStatus", responseStatus);

        response.putHeader("content-type", "application/json").end(responseObject.toString());

    }

    private void handleEncryptedRequest(String vendorClientAccountId, HttpServerResponse response, String body) {
        try {

            final String vendorId = System.getenv("DEFAULT_VENDOR_ID");
            JsonWebEncryption jwe = securityService.decryptPayload(body, vendorId);

            if(!securityService.isValidSignature(jwe, vendorId)){
                logger.error("Signature is not Verified");
                response.setStatusCode(500);
                response.end();
                return;
            }

            JsonObject assignment = new JsonObject(jwe.getPlaintextString());
            assignmentService.assign(vendorClientAccountId, assignment);

            JsonObject responseObject = new JsonObject().put("memberToken", assignment.getString("memberToken"))
                    .put("vendorClientAccountId", assignment.getString("vendorClientAccountId"));

            JsonWebSignature signature = securityService.createSignature(responseObject.toString().getBytes(), vendorId);
            String compactSerialization = securityService.encryptPayload(signature, responseObject.toString().getBytes(), "application/json", null, vendorId);


            response.putHeader("content-type", "application/jose").end(compactSerialization);
        } catch (JoseException e) {
            logger.error(e);
            response.setStatusCode(500);
            response.setStatusMessage(e.getMessage());
            response.end();
        }
    }

    // This is here strictly for demonstration purposes
    public void get(RoutingContext routingContext){
        HttpServerRequest request = routingContext.request();
        String vendorClientAccountId = request.getParam("id");
        final HttpServerResponse response = routingContext.response();

        final JsonArray assignments = assignmentService.list(vendorClientAccountId);

        response.end(assignments.encodePrettily());
    }

}
