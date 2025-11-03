package org.example.Gateways.Temporary.TaskGateway;

import com.eclipsesource.json.Json;
import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.example.Mapper.TruckMapper;
import org.example.Gateways.AbstractGateway;
import org.example.Things.EventActionHandler;
import org.example.Things.TaskThings.*;
import org.example.Things.TruckThing.TruckEventsActions;
import org.example.Things.TruckThing.TruckStatus;
import org.example.Things.WarehouseThing.Warehouse;
import org.example.util.ThingHandler;
import org.example.Things.TruckThing.Truck;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.ToDoubleFunction;

public class TaskGateway extends AbstractGateway<Task> {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    TasksEvents tasksEvents = new TasksEvents();
    TaskActions taskActions = new TaskActions();
    ThingHandler thingHandler = new ThingHandler();

    Task task;

    public TaskGateway(DittoClient dittoClient, DittoClient listenerClient, InfluxDBClient influxDBClient, Task task) {
        super(dittoClient, listenerClient, influxDBClient);
        this.task = task;
        assignThingToTask();
        registerForThingMessagesFromThing();
    }

    @Override
    public void startGateway() {
        updateAttributes(task);
    }
    @Override
    public void logToInfluxDB(Task thing) {

    }

    @Override
    public void updateAttributes(Task task) {
        updateAttributeValue("status", task.getStatus().toString(), task.getThingId());
        updateAttributeValue("creationDate", task.getCreationTime(), task.getThingId());
        updateAttributeValue("type", task.getTaskType().toString(), task.getThingId());
    }


    public <T> T selectBestThing(List<T> things, ToDoubleFunction<T> score) {
        return things.stream().min(Comparator.comparingDouble(score)).orElse(null);
    }

    public void assignThingToTask() {
        String selectedTruck = findBestIdleTruck();

        System.out.println("DAS WURDE GEFUNDEN "+ selectedTruck);
        if (selectedTruck == null) {

            noSuitableThingFound();
            return;
        }

        assignTruckToTask(selectedTruck);
        sendEventForTask(task);

    }

    public void sendEventForTask(Task task) {
        tasksEvents.sendStartEvent(dittoClient, task);
        switch (task.getTaskType()) {
            case LOAD -> {
                taskActions.sendLoadAction(dittoClient, task);
                logger.info("LOAD EVENT SENT FOR {}", task.getThingId());
            }
            case UNLOAD -> {
                taskActions.sendUnloadAction(dittoClient, task);
                logger.info("UNLOAD EVENT SENT FOR {}", task.getThingId());
            }
            default -> logger.warn("Unknown Task Type: {}", task.getTaskType());
        }
    }

    public void noSuitableThingFound() {
        logger.warn("NO BEST THING FOUND FOR {}", task.getThingId());
        task.setStatus(TaskStatus.PAUSED);
        tasksEvents.sendPausedEvent(dittoClient, task);
    }

    public String findBestIdleTruck() {

        if(task.getTargetTruck() == null) {
            Truck bestTruck = null;
            try {
                bestTruck = thingHandler.searchThings(dittoClient, new TruckMapper()::fromThing, "truck")
                        .stream().sorted(Comparator.comparingDouble(truck ->
                                truck.getAverageUtilizationForTask(truck.getUtilization(), truck.calculateLocationUtilization((Warehouse) task.getData("toWarehouse")))))
                        .filter(truck -> truck.getStatus() == TruckStatus.IDLE)
                        .findFirst()
                        .orElse(null);
            } catch (Exception e) {
                logger.error("error while looking for truck: {}", e.getMessage(), e);
            }

            return bestTruck != null ? bestTruck.getThingId() : null;
        }
        return task.getTargetTruck();
    }

    public void assignTruckToTask(String truck) {
        task.setTargetTruck(truck);
        updateAttributeValue("targetThing", task.getTargetTruck(), task.getThingId());

    }


//listener for messages from the truck. Sending to the process.
    public void registerForThingMessagesFromThing() {
        listenerClient.live().registerForMessage("thing_" + task.getThingId(), "*", message -> {
            Optional<?> optionalObject = message.getPayload();
            switch (message.getSubject()) {
                case TruckEventsActions.TRUCK_SUCCESSFUL:
                    if (optionalObject.isPresent()) {
                        String rawPayload = optionalObject.get().toString();
                        var parsePayload = Json.parse(rawPayload).asObject();

                        String thingId = parsePayload.get("thingId").asString();


                        if (task.getTargetTruck().equals(thingId)) {
                            tasksEvents.sendFinishedEvent(dittoClient, task);
                            logger.info("Task {} for Truck {} finished successful", task.getThingId(), task.getTargetTruck());
                        }
                    }
                    break;
                case TruckEventsActions.TRUCK_FAILED:
                    tasksEvents.sendFailEvent(dittoClient, task);
                    logger.warn("Task {} of Truck {} failed", task.getThingId(), task.getTargetTruck());
                    break;
                case TruckEventsActions.TRUCK_TIRE_PRESSURE_LOW:
                    tasksEvents.sendEscalationEvent(dittoClient, task);
                    logger.warn("Task {} of Truck {} escalated", task.getThingId(), task.getTargetTruck());
                    break;

            }
        });
    }

}

