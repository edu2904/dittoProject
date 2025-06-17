package org.example.Things.TruckThing;

import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.things.model.ThingId;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class TruckEventsActions {
    public void weightEvent(DittoClient dittoClient, String thingID, double weightAmount) {

        JsonObject weightmessage = JsonObject.newBuilder()
                .set("amount", weightAmount)
                .build();
        JsonObject jsonWeight = JsonObject.newBuilder()
                .set("title", "Weight Status Payload")
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
    public void fuelAmountEvents(DittoClient dittoClient, String thingID, double fuelAmount)  {
            JsonObject lowfuelmessage = JsonObject.newBuilder()
                    .set("amount", fuelAmount)
                    .build();
            JsonObject jsonData = JsonObject.newBuilder()
                    .set("title", "Caution LOW FUEL!")
                    .set("type", "object")
                    .set("properties", lowfuelmessage).build();

            if (fuelAmount < 50) {
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

}

