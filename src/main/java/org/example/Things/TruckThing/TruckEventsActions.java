package org.example.Things.TruckThing;

import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.things.model.ThingId;
import org.example.DittoEventAction.DittoEventActionHandler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class TruckEventsActions {

    DittoEventActionHandler dittoEventActionHandler = new DittoEventActionHandler();

    public void startTruckLogging(String thingID){
        dittoEventActionHandler.createEventLoggingForAttribute(thingID, "showStatus");
        dittoEventActionHandler.createEventLoggingForFeature(thingID, "lowfuel", "FuelTank");
        dittoEventActionHandler.createActionLoggingForAttribute(thingID, "resetProgress");

    }
    public void weightEvent(DittoClient dittoClient, String thingID, double weightAmount) {

        if(weightAmount > 9000) {
            JsonObject weightmessage = JsonObject.newBuilder()
                    .set("amount", weightAmount)
                    .build();
            JsonObject jsonWeight = JsonObject.newBuilder()
                    .set("title", "Weight too high")
                    .set("type", "object")
                    .set("properties", weightmessage).build();


            dittoClient.live()
                    .forId(ThingId.of(thingID))
                    .message()
                    .from()
                    .subject("showStatus")
                    .payload(jsonWeight)
                    .contentType("application/json")
                    .send();
        }

    }
    public void fuelAmountEvents(DittoClient dittoClient, String thingID, double fuelAmount)  {
            JsonObject lowfuelmessage = JsonObject.newBuilder()
                    .set("amount", fuelAmount)
                    .build();
            JsonObject jsonData = JsonObject.newBuilder()
                    .set("title", "Caution LOW FUEL!")
                    .set("type", "object")
                    .set("properties", lowfuelmessage).build();

            if (fuelAmount < 45) {
                dittoClient.live()
                        .forId(ThingId.of(thingID))
                        .forFeature("FuelTank")
                        .message()
                        .from()
                        .subject("lowfuel")
                        .payload(jsonData)
                        .contentType("application/json")
                        .send();}
        }
    public void progressResetAction(DittoClient dittoClient, String thingID, Truck truck, double currentProgress) {


        if(currentProgress == 100) {



            JsonObject object = JsonObject.newBuilder().set("message", "Progress was resettet").build();
            dittoClient
                    .live()
                    .forId(ThingId.of(thingID))
                    .message()
                    .to()
                    .subject("resetProgress")
                    .payload(object)
                    .contentType("application/json")
                    .send();

            truck.setProgress(0);

        }

    }
    public void sendEvent(DittoClient dittoClient, String thingID, JsonObject jsonData, String eventSubject) {
        dittoClient.live()
                .forId(ThingId.of(thingID))
                .message()
                .from()
                .subject(eventSubject)
                .payload(jsonData)
                .contentType("application/json")
                .send();
    }

}

