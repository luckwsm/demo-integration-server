package com.lyric;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by amadden on 2/1/16.
 */
public class AssignmentService {
    Logger logger = LoggerFactory.getLogger(AssignmentService.class.getName());
    Map<String, JsonArray> assignmentMap = new HashMap<>();

    public void assign(String vendorClientAccountId, JsonObject assignment){
        assignment.put("assignmentDate", new Date().toString());
        logger.info(String.format("Assignment Body: %s", assignment));

        JsonArray vendorClientAccountAssignments = assignmentMap.get(vendorClientAccountId);
        vendorClientAccountAssignments = vendorClientAccountAssignments == null ? new JsonArray() : vendorClientAccountAssignments;
        vendorClientAccountAssignments.add(assignment);
        assignmentMap.put(vendorClientAccountId, vendorClientAccountAssignments);
        // this is where you'd put code to lock down an account, change the assignment from the artist
        // to lyric
    }

    public JsonArray list(String vendorClientAccountId){
        JsonArray assignments = assignmentMap.get(vendorClientAccountId);
        return assignments != null ? assignments : new JsonArray();
    }

}
