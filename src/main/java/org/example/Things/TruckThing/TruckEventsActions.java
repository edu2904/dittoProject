package org.example.Things.TruckThing;

import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.things.model.ThingId;
import org.example.DittoEventAction.DittoEventActionHandler;
import org.example.Things.EventActionHandler;
import org.example.Things.Location;

import java.util.Map;
import java.util.concurrent.*;

public class TruckEventsActions implements EventActionHandler {


    private final Map<String, Boolean> eventState = new ConcurrentHashMap<>();
    private final Map<String, Long> waitingSince = new ConcurrentHashMap<>();

    public DittoClient dittoClient;
     public static final String TRUCKARRIVED = "truckArrived";
    public static final String TRUCKWAITNGTOOLONG = "tooLongIdle";
    public static final String TASKSUCCESS = "taskSuccessful";
    public static final String TASKFAILED = "taskFailed";

    public TruckEventsActions(DittoClient dittoClient){
        this.dittoClient = dittoClient;
    }

    public void sendTaskFailEvent(Truck truck){
        JsonObject failedMessage = JsonObject
                .newBuilder()
                .set("message", truck.getThingId() + " failed to fulfill task")
                .build();
        sendTaskFailed(dittoClient, truck.getThingId(), failedMessage);
    }
    public void sendSuccessEvent(Truck truck){
        System.out.println("SUCCESS SENT");
        JsonObject successMessage = JsonObject
                .newBuilder()
                .set("message", truck.getThingId() + " successfully fulfilled task")
                .build();
        sendTaskSuccess(dittoClient, truck.getThingId(), successMessage);
    }

    public void arrivalEvent(String thingId, Location destination, Location location, String locationName){
        if(destination != null) {
            boolean wasActive = eventState.getOrDefault("arrival_" + thingId, false);
            boolean isActive = location.getLat() == destination.getLat() && location.getLon() == destination.getLon();

            if (!wasActive && isActive) {
                JsonObject arrivalMessage = JsonObject.newBuilder().set("message", thingId + " arrived at next destination: " + locationName).build();
                sendEvent(dittoClient, thingId, arrivalMessage, TRUCKARRIVED);
                eventState.put("arrival_" + thingId, true);
            } else if (wasActive && !isActive) {
                eventState.put("arrival_" + thingId, false);
            }
        }
    }
    public void checkForTruckWithoutTask(String thingId, TruckStatus truckStatus){
        boolean wasActive = eventState.getOrDefault("withoutTask_" + thingId, false);
        boolean isActive = truckStatus == TruckStatus.IDLE;

        if(!wasActive && isActive){
            waitingSince.putIfAbsent(thingId, System.currentTimeMillis());
            long waited = System.currentTimeMillis() - waitingSince.get(thingId);
            if(waited >= TimeUnit.SECONDS.toMillis(10)){
                JsonObject withoutTaskMessage = JsonObject.newBuilder().set("message", thingId + " waiting for too long. Requesting task search").build();
                sendEvent(dittoClient, thingId, withoutTaskMessage, TRUCKWAITNGTOOLONG);
                eventState.put("withoutTask_" + thingId, true);
            }
        }else if (wasActive && !isActive) {
            eventState.put("withoutTask_" + thingId, false);
            waitingSince.remove(thingId);
        }
    }


    public void weightEvent(String thingID, double weightAmount) {

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
    public void fuelAmountEvents(String thingID, double fuelAmount)  {
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

    public void taskSearchAction(String thingID, double currentWeight){
        if(currentWeight > 9000){
            JsonObject weightmessage = JsonObject.newBuilder()
                    .set("amount", currentWeight)
                    .build();
            JsonObject jsonWeight = JsonObject.newBuilder()
                    .set("title", "Weight too high")
                    .set("type", "object")
                    .set("properties", weightmessage).build();


            sendAction(dittoClient, thingID, jsonWeight, "taskSearch");
        }
    }
    public void progressResetAction(String thingID, Truck truck, double currentProgress) {
        if(currentProgress == 100) {
            JsonObject object = JsonObject.newBuilder().set("message", "Progress was resettet").build();

            sendAction(dittoClient, thingID, object, "resetProgress");
            truck.setProgress(0);

        }

    }

}

