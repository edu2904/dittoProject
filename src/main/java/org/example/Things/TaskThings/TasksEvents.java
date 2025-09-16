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
                .set("thingId", task.getThingId())
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
}

