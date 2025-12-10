package org.example.Things.TaskThings;

import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.json.JsonArray;
import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.json.JsonValue;
import org.example.Things.EventActionHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
// all actions the task is able to send. The static values have to be the ones the defined in the corresponding WoT file

public class TasksEvents implements EventActionHandler {
    private final Map<String, Boolean> refuelStarted = new ConcurrentHashMap<>();

    private final Map<String, Boolean> tirePressureTaskStarted = new ConcurrentHashMap<>();

    private final Map<String, Boolean> loadingTaskStarted = new ConcurrentHashMap<>();

    public static final String TASK_FINISHED = "taskFinished";
    public static final String TASK_BEGIN = "taskBegins";
    public static final String TASK_FAILED = "taskFailed";
    public static final String TASK_ESCALATED = "taskEscalated";
    public static final String TASK_PAUSED = "taskPaused";
    public static final String TASK_CONTINUED = "taskContinued";
    public static final String TASK_TIMER = "taskTimer";
    public static final String TASK_ABORTED = "taskAborted";


    public void sendStartEvent(DittoClient dittoClient, Task task){
        JsonArray assignedThings = JsonArray.newBuilder()
                .addAll(task
                        .getTargetTrucks()
                        .stream()
                        .map(JsonValue::of)
                        .toList())
                .build();


        JsonObject startObject = JsonObject.newBuilder()
                .set("message", "Task started for " + task.getThingId())
                .set("thingId", task.getThingId())
                .set("targetThings", assignedThings)
                .set("setId", task.getSetId()).build();
        sendEvent(dittoClient, task.getThingId(), startObject, TASK_BEGIN);
    }
    public void sendFailEvent(DittoClient dittoClient, Task task){
        String thingID = task.getThingId();
        JsonObject failObject = JsonObject
                .newBuilder()
                .set("message", "Task failed for " + task.getThingId())
                .set("thingId", task.getThingId())
                .set("eventMessage", task.getEventInformation())
                .set("time", task.getTime())
                .set("setId", task.getSetId()).build();
        sendEvent(dittoClient, task.getThingId(), failObject, TASK_FAILED);
    }
    public void sendFinishedEvent(DittoClient dittoClient, Task task){
        JsonArray assignedThings = JsonArray.newBuilder()
                .addAll(task
                        .getTargetTrucks()
                        .stream()
                        .map(JsonValue::of)
                        .toList())
                .build();

        JsonObject finishedObject = JsonObject
                .newBuilder()
                .set("message", "Task finished")
                .set("thingId", task.getThingId())
                .set("targetThings", assignedThings)
                .set("setId", task.getSetId())
                .build();
        sendEvent(dittoClient, task.getThingId(), finishedObject, TASK_FINISHED);
    }
    public void sendEscalationEvent(DittoClient dittoClient, Task task, String failedThing){
        JsonObject escalationObject = JsonObject
                .newBuilder()
                .set("message", "Task escalated for " + task.getThingId())
                .set("thingId", task.getThingId())
                .set("failedThing", failedThing)
                .set("eventMessage", task.getEventInformation())
                .set("setId", task.getSetId())
                .build();
        sendEvent(dittoClient, task.getThingId(), escalationObject, TASK_ESCALATED);
    }
    public void sendPausedEvent(DittoClient dittoClient, Task task){
        JsonObject pauseObject = JsonObject
                .newBuilder()
                .set("message", "Task paused for " + task.getThingId())
                .set("thingId", task.getThingId())
                .set("setId", task.getSetId())
                .build();
        sendEvent(dittoClient, task.getThingId(), pauseObject, TASK_PAUSED);
    }
    public void sendTimeEvent(DittoClient dittoClient, Task task){
        JsonObject timeObject = JsonObject
                .newBuilder()
                .set("message", "Task time " + task.getThingId())
                .set("thingId", task.getThingId())
                .set("setId", task.getSetId())
                .set("time", task.getTime())
                .build();
        sendEvent(dittoClient, task.getThingId(), timeObject, TASK_TIMER);
    }

    public void taskAbortedEvent(DittoClient dittoClient, Task task){
        for(String truckThingId : task.getTargetTrucks()) {
            JsonObject timeObject = JsonObject
                    .newBuilder()
                    .set("message", "Task aborted " + task.getThingId())
                    .set("thingId", truckThingId)
                    .set("setId", task.getSetId())
                    .build();
            sendEvent(dittoClient, task.getThingId(), timeObject, TASK_ABORTED);
        }
    }
}

