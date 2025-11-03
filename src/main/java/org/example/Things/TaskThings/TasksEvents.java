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
    public static final String TASK_ESCALATED = "taskEscalated";
    public static final String TASK_PAUSED = "taskPaused";
    public static final String TASK_CONTINUED = "taskContinued";


    public void sendStartEvent(DittoClient dittoClient, Task task){
        JsonObject startObject = JsonObject.newBuilder()
                .set("message", "Task started for " + task.getThingId())
                .set("setId", task.getSetId()).build();
        sendEvent(dittoClient, task.getThingId(), startObject, TASK_BEGIN);
    }
    public void sendFailEvent(DittoClient dittoClient, Task task){
        String thingID = task.getThingId();
        JsonObject failObject = JsonObject
                .newBuilder()
                .set("message", "Task failed for " + task.getThingId())
                .set("thingId", task.getThingId())
                .set("setId", task.getSetId()).build();
        sendEvent(dittoClient, task.getThingId(), failObject, TASK_FAILED);
    }
    public void sendFinishedEvent(DittoClient dittoClient, Task task){
        JsonObject finishedObject = JsonObject
                .newBuilder()
                .set("message", "Task finished")
                .set("thingId", task.getThingId())
                .set("setId", task.getSetId())
                .build();
        sendEvent(dittoClient, task.getThingId(), finishedObject, TASK_FINISHED);
    }
    public void sendEscalationEvent(DittoClient dittoClient, Task task){
        JsonObject escalationObject = JsonObject
                .newBuilder()
                .set("message", "Task escalated for " + task.getThingId())
                .set("thingId", task.getThingId())
                .set("setId", task.getSetId())
                .build();
        sendEvent(dittoClient, task.getThingId(), escalationObject, TASK_FINISHED);
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
}

