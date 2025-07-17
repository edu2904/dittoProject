package org.example.Things;

import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.things.model.ThingId;

public interface EventActionHandler {
    void startLogging(String thingID);
    default void sendEvent(DittoClient dittoClient, String thingID, JsonObject jsonData, String eventSubject) {
        dittoClient.live()
                .forId(ThingId.of(thingID))
                .message()
                .from()
                .subject(eventSubject)
                .payload(jsonData)
                .contentType("application/json")
                .send();
    }
    default void sendAction(DittoClient dittoClient, String thingID, JsonObject jsonData, String actionSubject){
        dittoClient
                .live()
                .forId(ThingId.of(thingID))
                .message()
                .to()
                .subject(actionSubject)
                .payload(jsonData)
                .contentType("application/json")
                .send();
    }
}
