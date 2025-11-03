package org.example.Things;


import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.things.model.ThingId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;


public interface EventActionHandler {
    Logger logger = LoggerFactory.getLogger(EventActionHandler.class.getName());
    String TASK_SUCCESS = "taskSuccessful";
    String TASK_FAILED = "taskFailed";


    default void sendEvent(DittoClient dittoClient, String thingID, JsonObject jsonData, String eventSubject) {
        dittoClient.live()
                .forId(ThingId.of(thingID))
                .message()
                .from()
                .subject(eventSubject)
                .timeout(Duration.ofSeconds(10))
                .payload(jsonData)
                .contentType("application/json")
                .send(String.class, (response, throwable) -> logger.info("RESPONSE for send message from subject {}: {}", response.getSubject(), response.getPayload().orElse(null)));
    }
    default void sendAction(DittoClient dittoClient, String thingID, JsonObject jsonData, String actionSubject){
        dittoClient
                .live()
                .forId(ThingId.of(thingID))
                .message()
                .to()
                .subject(actionSubject)
                .timeout(Duration.ofSeconds(10))
                .payload(jsonData)
                .contentType("application/json")
                .send(String.class, (response, throwable) ->
                        logger.info("Received response for {} message: {} with headers {}",
                                actionSubject,
                                response.getPayload().orElse(null),
                                response.getHeaders()));
    }

}
