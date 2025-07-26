package org.example.Things.TaskThings;

import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.things.model.ThingId;
import org.example.DittoEventAction.DittoEventActionHandler;
import org.example.Things.EventActionHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TasksEventsActions implements EventActionHandler {
    private final Map<String, Boolean> refuelStarted = new ConcurrentHashMap<>();

    private final Map<String, Boolean> tirePressureTaskStarted = new ConcurrentHashMap<>();

    private final Map<String, Boolean> loadingTaskStarted = new ConcurrentHashMap<>();



    DittoEventActionHandler dittoEventActionHandler = new DittoEventActionHandler();

    @Override
    public void startLogging(String thingID) {
        dittoEventActionHandler.createEventLoggingForAttribute(thingID, "refuelBegin");
        dittoEventActionHandler.createEventLoggingForAttribute(thingID, "refuelUndergoing");
        dittoEventActionHandler.createEventLoggingForAttribute(thingID, "refuelFinished");

        dittoEventActionHandler.createEventLoggingForAttribute(thingID, "tirePressureAdjustingBegins");
        dittoEventActionHandler.createEventLoggingForAttribute(thingID, "tirePressureAdjustingUndergoing");
        dittoEventActionHandler.createEventLoggingForAttribute(thingID, "tirePressureAdjustingFinished");

        dittoEventActionHandler.createEventLoggingForAttribute(thingID, "loadBegin");
        dittoEventActionHandler.createEventLoggingForAttribute(thingID, "loadingUndergoing");
        dittoEventActionHandler.createEventLoggingForAttribute(thingID, "loadingFinished");

    }

    public void handleRefuelTaskEvents(DittoClient dittoClient, Tasks tasks) throws InterruptedException {

        String thingID = tasks.getThingId();
        JsonObject startObject = JsonObject.newBuilder().set("message", "Refuel Task started for " + thingID).build();

        JsonObject processObject = JsonObject.newBuilder().set("message", "Refuel Task in Process for " + thingID).build();
        JsonObject endObject = JsonObject.newBuilder().set("message", "Refuel Task finished for " + thingID).build();

        if (tasks.getStatus().equals(TaskStatus.UNDERGOING)) {

            sendEvent(dittoClient, tasks.getThingId(), processObject, "refuelUndergoing");
        }
        if(tasks.getStatus().equals(TaskStatus.FINISHED)){
            sendEvent(dittoClient, tasks.getThingId(), endObject, "refuelFinished");
            //refuelStarted.put(thingID, false);
        }
    }
    public void handleTirePressureLowTaskEvents(DittoClient dittoClient, Tasks tasks) throws InterruptedException {


        String thingID = tasks.getThingId();
        JsonObject startObject = JsonObject.newBuilder().set("message", "Tire Pressure Task started for " + thingID).build();

        JsonObject processObject = JsonObject.newBuilder().set("message", "Tire Pressure Task in Process for " + thingID).build();
        JsonObject endObject = JsonObject.newBuilder().set("message", "Tire Pressure Task finished for " + thingID).build();

        if (tasks.getStatus().equals(TaskStatus.UNDERGOING)) {
            sendEvent(dittoClient, tasks.getThingId(), processObject, "tirePressureAdjustingUndergoing");
        }
        if(tasks.getStatus().equals(TaskStatus.FINISHED)){
            sendEvent(dittoClient, tasks.getThingId(), endObject, "tirePressureAdjustingFinished");
            //tirePressureTaskStarted.put(thingID, false);
        }
    }
    public void handleLoadingTruckTaskEvents(DittoClient dittoClient, Tasks tasks) throws InterruptedException {
        String thingID = tasks.getThingId();
        JsonObject startObject = JsonObject.newBuilder().set("message", "Loading Task started for " + thingID).build();

        JsonObject processObject = JsonObject.newBuilder().set("message", "Loading Task in Process for " + thingID).build();
        JsonObject endObject = JsonObject.newBuilder().set("message", "Loading Task finished for " + thingID).build();

        if (tasks.getStatus().equals(TaskStatus.UNDERGOING)) {
            sendEvent(dittoClient, tasks.getThingId(), processObject, "loadingUndergoing");
        }
        if(tasks.getStatus().equals(TaskStatus.FINISHED)){
            sendEvent(dittoClient, tasks.getThingId(), endObject, "loadingFinished");
           // loadingTaskStarted.put(thingID, false);
        }
    }
}

