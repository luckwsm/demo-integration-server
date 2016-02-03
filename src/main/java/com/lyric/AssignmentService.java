package com.lyric;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Created by amadden on 2/1/16.
 */
public class AssignmentService {
    Logger logger = LoggerFactory.getLogger(AssignmentService.class.getName());

    public void assign(JsonObject assignment){
        logger.info(String.format("Assignment Body: %s", assignment));
        // this is where you'd put code to lock down an account, change the assignment from the artist
        // to lyric
    }

}
