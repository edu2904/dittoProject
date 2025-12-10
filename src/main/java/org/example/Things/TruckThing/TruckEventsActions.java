package org.example.Things.TruckThing;

import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.things.model.ThingId;
import org.example.Things.EventActionHandler;
import org.example.Things.Location;

import java.util.Map;
import java.util.concurrent.*;


// all events the truck is able to send. The static values have to be the ones the defined in the corresponding WoT file
public class TruckEventsActions implements EventActionHandler {


    private final Map<String, Boolean> eventState = new ConcurrentHashMap<>();
    private final Map<String, Long> waitingSince = new ConcurrentHashMap<>();

    public DittoClient dittoClient;
     public static final String TRUCK_ARRIVED = "truckArrived";
    public static final String TRUCK_WAITING_TOO_LONG = "tooLongIdle";

    public static final String TRUCK_SUCCESSFUL = "truckSuccessful";
    public static final String TRUCK_FAILED = "truckFailed";
    public static final String TRUCK_TIRE_PRESSURE_LOW = "tirePressureTooLow";
    public static final String FUEL_TOO_LOW = "fuelTooLow";

    public TruckEventsActions(){

    }

    public void sendTaskFailEvent(DittoClient dittoClient, Truck truck){
        JsonObject failedMessage = JsonObject
                .newBuilder()
                .set("message", truck.getThingId() + " failed to fulfill task")
                .set("thingId", truck.getThingId())
                .build();
        sendEvent(dittoClient, truck.getThingId(), failedMessage, TRUCK_FAILED);
    }
    public void sendSuccessEvent(DittoClient dittoClient, Truck truck){
        System.out.println("SUCCESS SENT FOR " + truck.getThingId());
        JsonObject successMessage = JsonObject
                .newBuilder()
                .set("message", truck.getThingId() + " successfully fulfilled task")
                .set("thingId", truck.getThingId())
                .build();
        System.out.println("MESSAGE SENT FROM " + truck.getThingId());
        sendEvent(dittoClient, truck.getThingId(), successMessage, TRUCK_SUCCESSFUL);
    }
    public void sendTirePressureTooLowEvent(DittoClient dittoClient, Truck truck){
        JsonObject tirePressureMessage = JsonObject
                .newBuilder()
                .set("message", truck.getThingId() + " reached critical tire pressure value")
                .set("thingId", truck.getThingId())
                .build();
        System.out.println("TASK SENT FROM " + truck.getThingId());
        sendEvent(dittoClient, truck.getThingId(), tirePressureMessage, TRUCK_TIRE_PRESSURE_LOW);
    }
    public void sendFuelTooLow(DittoClient dittoClient, Truck truck)  {
        JsonObject fuelMessage = JsonObject
                .newBuilder()
                .set("message", truck.getThingId() + " reached critical fuel value")
                .set("thingId", truck.getThingId())
                .build();
        System.out.println("TASK SENT FROM " + truck.getThingId());
        sendEvent(dittoClient, truck.getThingId(), fuelMessage, FUEL_TOO_LOW);
    }

    public void arrivalEvent(String thingId, Location destination, Location location, String locationName){
        if(destination != null) {
            boolean wasActive = eventState.getOrDefault("arrival_" + thingId, false);
            boolean isActive = location.getLat() == destination.getLat() && location.getLon() == destination.getLon();

            if (!wasActive && isActive) {
                JsonObject arrivalMessage = JsonObject.newBuilder().set("message", thingId + " arrived at next destination: " + locationName).build();
                sendEvent(dittoClient, thingId, arrivalMessage, TRUCK_ARRIVED);
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
                sendEvent(dittoClient, thingId, withoutTaskMessage, TRUCK_WAITING_TOO_LONG);
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

