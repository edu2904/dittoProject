package org.example.Things.TaskThings;

import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.json.JsonObject;
import org.example.Things.EventActionHandler;
import org.example.Things.TruckThing.Truck;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TaskActions implements EventActionHandler {
    public static final String TASK_LOAD_START = "taskLoadStart";
    public static final String TASK_REPLACEMENT_LOAD_START = "taskReplacementLoadStart";
    public static final String TASK_UNLOAD_START = "taskUnloadStart";

    public void sendLoadAction(DittoClient dittoClient, Task task){

        Map<String, Double> cargoAllocation = task.getThingAllocation();
        List<String> targetThings = task.getTargetTrucks();
        System.out.println("ÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖ");
        System.out.println(cargoAllocation);
        System.out.println("ÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖ");

        for (String thingId : targetThings) {
            double quantity = 0.0;
            if(cargoAllocation != null){
                quantity = cargoAllocation.getOrDefault(thingId, 0.0);
            }
            JsonObject startObject = JsonObject.newBuilder()
                    .set("message", "Load task started for " + thingId)
                    .set("thingId", thingId)
                    .set("from", task.getData("from").toString())
                    .set("to", task.getData("to").toString())
                    .set("quantity", quantity)
                    .build();
            sendAction(dittoClient, task.getThingId(), startObject, TASK_LOAD_START);


        }
    }
    public void sendReplacementLoadAction(DittoClient dittoClient,Task task, Map<String, Double> replacement){
        for(Map.Entry<String, Double> truck : Collections.unmodifiableSet(replacement.entrySet())){
            String thingId = truck.getKey();
            double quantity = truck.getValue();
            JsonObject startObject = JsonObject.newBuilder()
                    .set("message", "Unload task started for " + thingId)
                    .set("thingId", thingId)
                    .set("from", task.getData("from").toString())
                    .set("to", task.getData("to").toString())
                    .set("quantity", quantity)
                    .build();
            sendAction(dittoClient, task.getThingId(), startObject, TASK_LOAD_START);
        }
    }

    public void sendUnloadAction(DittoClient dittoClient, Task task) {

        Map<String, Double> cargoAllocation = task.getThingAllocation();
        System.out.println("ÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖ");
        System.out.println(cargoAllocation);
        System.out.println("ÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖÖ");

        List<String> targetTrucks = task.getTargetTrucks();

        for (String thingId : targetTrucks) {
            double quantity = 0.0;
            if(cargoAllocation != null){
                quantity = cargoAllocation.getOrDefault(thingId, 0.0);
            }
            JsonObject startObject = JsonObject.newBuilder()
                    .set("message", "Unload task started for " + thingId)
                    .set("thingId", thingId)
                    .set("from", task.getData("from").toString())
                    .set("to", task.getData("to").toString())
                    .set("quantity", quantity)
                    .build();
            sendAction(dittoClient, task.getThingId(), startObject, TASK_UNLOAD_START);
        }
    }


}
