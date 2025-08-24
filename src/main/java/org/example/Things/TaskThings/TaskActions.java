package org.example.Things.TaskThings;

import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.json.JsonObject;
import org.example.Things.EventActionHandler;

import java.util.HashMap;
import java.util.Map;


public class TaskActions implements EventActionHandler {
    public static final String TASK_LOAD_START = "taskLoadStart";
    public static final String TASK_UNLOAD_START = "taskUnloadStart";

    public void sendLoadEvent(DittoClient dittoClient, Task task){
        double quantity = (double) task.getData("quantity");
        JsonObject startObject = JsonObject.newBuilder()
                .set("message", "Load task started for " + task.getTargetTruck())
                .set("thingId", task.getTargetTruck())
                .set("from", task.getData("from").toString())
                .set("to", task.getData("to").toString())
                .set("quantity", quantity)
                .build();
        sendAction(dittoClient, task.getThingId(), startObject, TASK_LOAD_START);
    }

    public void sendUnloadEvent(DittoClient dittoClient, Task task){
        double quantity = (double) task.getData("quantity");
        JsonObject startObject = JsonObject.newBuilder()
                .set("message", "Unload task started for " + task.getTargetTruck())
                .set("thingId", task.getTargetTruck())
                .set("from", task.getData("from").toString())
                .set("to", task.getData("to").toString())
                .set("quantity", quantity)
                .build();
        sendAction(dittoClient, task.getThingId(), startObject, TASK_UNLOAD_START);
    }

}
