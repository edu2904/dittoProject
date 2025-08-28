package org.example.Things.TaskThings;

import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.json.JsonObject;
import org.example.Things.EventActionHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TasksEvents implements EventActionHandler {
    private final Map<String, Boolean> refuelStarted = new ConcurrentHashMap<>();

    private final Map<String, Boolean> tirePressureTaskStarted = new ConcurrentHashMap<>();

    private final Map<String, Boolean> loadingTaskStarted = new ConcurrentHashMap<>();

    public static final String TASK_FINISHED = "taskFinished";
    public static final String TASK_BEGIN = "taskBegins";
    public static final String TASK_FAILED = "taskFailed";



  //  DittoEventActionHandler dittoEventActionHandler = new DittoEventActionHandler();

  //  @Override
 //   public void startLogging(String thingID) {
 //       dittoEventActionHandler.createEventLoggingForAttribute(thingID, "taskBegins");
 //       dittoEventActionHandler.createEventLoggingForAttribute(thingID, "taskUndergoing");
 //       dittoEventActionHandler.createEventLoggingForAttribute(thingID, "taskFinished");
 //   }

    public void sendStartEvent(DittoClient dittoClient, Task task){
        String thingID = task.getThingId();
        JsonObject startObject = JsonObject.newBuilder()
                .set("message", "Task started for " + thingID)
                .set("setId", task.getSetId()).build();
        sendEvent(dittoClient, thingID, startObject, TASK_BEGIN);
    }
    public void sendFailEvent(DittoClient dittoClient, Task task){
        String thingID = task.getThingId();
        JsonObject startObject = JsonObject
                .newBuilder()
                .set("message", "Task failed for " + thingID)
                .set("setId", task.getSetId()).build();
        sendEvent(dittoClient, thingID, startObject, TASK_FAILED);
    }
    public void sendFinishedEvent(DittoClient dittoClient, Task task){
        String thingID = task.getThingId();
        JsonObject endObject = JsonObject
                .newBuilder()
                .set("message", "Task finished")
                .set("thingId", task.getThingId())
                .set("setId", task.getSetId())
                .build();
        sendEvent(dittoClient, thingID, endObject, TASK_FINISHED);
    }

    public void handleRefuelTaskEvents(DittoClient dittoClient, Task task) throws InterruptedException {

        String thingID = task.getThingId();
        JsonObject startObject = JsonObject.newBuilder().set("message", "Refuel Task started for " + thingID).build();

        JsonObject processObject = JsonObject.newBuilder().set("message", "Refuel Task in Process for " + thingID).build();
        JsonObject endObject = JsonObject.newBuilder().set("message", "Refuel Task finished for " + thingID).build();

        if (task.getStatus().equals(TaskStatus.UNDERGOING)) {

            sendEvent(dittoClient, task.getThingId(), processObject, "taskUndergoing");
        }
        if(task.getStatus().equals(TaskStatus.FINISHED)){
            sendEvent(dittoClient, task.getThingId(), endObject, "taskFinished");
            //refuelStarted.put(thingID, false);
        }
    }
    public void handleTirePressureLowTaskEvents(DittoClient dittoClient, Task task) throws InterruptedException {


        String thingID = task.getThingId();
        JsonObject startObject = JsonObject.newBuilder().set("message", "Tire Pressure Task started for " + thingID).build();

        JsonObject processObject = JsonObject.newBuilder().set("message", "Tire Pressure Task in Process for " + thingID).build();
        JsonObject endObject = JsonObject.newBuilder().set("message", "Tire Pressure Task finished for " + thingID).build();

        if (task.getStatus().equals(TaskStatus.UNDERGOING)) {
            sendEvent(dittoClient, task.getThingId(), processObject, "taskUndergoing");
        }
        if(task.getStatus().equals(TaskStatus.FINISHED)){
            sendEvent(dittoClient, task.getThingId(), endObject, "taskFinished");
            //tirePressureTaskStarted.put(thingID, false);
        }
    }
    public void handleLoadingTruckTaskEvents(DittoClient dittoClient, Task task) throws InterruptedException {
        String thingID = task.getThingId();
        JsonObject startObject = JsonObject.newBuilder().set("message", "Loading Task started for " + thingID).build();

        JsonObject processObject = JsonObject.newBuilder().set("message", "Loading Task in Process for " + thingID).build();
        JsonObject endObject = JsonObject.newBuilder().set("message", "Loading Task finished for " + thingID).build();

        if (task.getStatus().equals(TaskStatus.UNDERGOING)) {
            sendEvent(dittoClient, task.getThingId(), processObject, "taskUndergoing");
        }
        if(task.getStatus().equals(TaskStatus.FINISHED)){
            sendEvent(dittoClient, task.getThingId(), endObject, "taskFinished");
           // loadingTaskStarted.put(thingID, false);
        }
    }
}

