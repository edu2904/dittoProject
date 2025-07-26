package org.example.Things.TruckThing;

import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.things.model.ThingId;
import org.example.DittoEventAction.DittoEventActionHandler;
import org.example.Things.EventActionHandler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class TruckEventsActions implements EventActionHandler {

    DittoEventActionHandler dittoEventActionHandler = new DittoEventActionHandler();

    @Override
    public void startLogging(String thingID) {
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


            sendEvent(dittoClient, thingID, jsonWeight, "showStatus");

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
                sendEvent(dittoClient, thingID, jsonData, "lowfuel");
            }
        }
    public void progressResetAction(DittoClient dittoClient, String thingID, Truck truck, double currentProgress) {


        if(currentProgress == 100) {
            JsonObject object = JsonObject.newBuilder().set("message", "Progress was resettet").build();

            sendAction(dittoClient, thingID, object, "resetProgress");
            truck.setProgress(0);

        }

    }

}

