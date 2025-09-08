package org.example.Gateways.ConcreteGateways.TaskGateway;

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
        registerForThingMessages();
    }

    @Override
    public void startGateway() {
        startUpdating(task);
    }

    @Override
    public void startUpdating(Task task) {
        updateAttributes(task);
    }

    @Override
    public void logToInfluxDB(Task thing, String measurementType) {

    }

    @Override
    public String getWOTURL() {
        return task.getTaskType().getWot();
    }

    @Override
    public void updateAttributes(Task task) {
        updateStandardTask(task);
    }

    public void updateStandardTask(Task task) {
        updateAttributeValue("status", task.getStatus().toString(), task.getThingId());
        updateAttributeValue("creationDate", task.getCreationTime(), task.getThingId());
        updateAttributeValue("type", task.getTaskType().toString(), task.getThingId());
    }


    public <T> T selectBestThing(List<T> things, ToDoubleFunction<T> score) {
        return things.stream().min(Comparator.comparingDouble(score)).orElse(null);
    }

    public void assignThingToTask() {
        String selectedTruck = findBestIdleTruck();

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
                taskActions.sendLoadEvent(dittoClient, task);
                logger.info("LOAD EVENT SENT FOR {}", task.getThingId());
            }
            case UNLOAD -> {
                taskActions.sendUnloadEvent(dittoClient, task);
                logger.info("UNLOAD EVENT SENT FOR {}", task.getThingId());
            }
            default -> logger.warn("Unknown Task Type: {}", task.getTaskType());
        }
    }

    public void noSuitableThingFound() {
        logger.warn("NO BEST THING FOUND FOR {}", task.getThingId());
        task.setStatus(TaskStatus.FAILED);
        tasksEvents.sendFailEvent(dittoClient, task);
    }

    public String findBestIdleTruck() {
        if(task.getTargetTruck() == null) {
            Truck bestTruck = new Truck();
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
            return Objects.requireNonNull(bestTruck).getThingId();
        }
        return task.getTargetTruck();
    }

    public void assignTruckToTask(String truck) {
        task.setTargetTruck(truck);
        updateAttributeValue("targetThing", task.getTargetTruck(), task.getThingId());

    }

    public void registerForThingMessages() {
        listenerClient.live().registerForMessage("thing_" + task.getThingId(), "*", message -> {
            Optional<?> optionalObject = message.getPayload();

            if (message.getSubject().equals(EventActionHandler.TASK_SUCCESS)) {
                if (optionalObject.isPresent()) {
                    String rawPayload = optionalObject.get().toString();
                    var parsePayload = Json.parse(rawPayload).asObject();
                    String thingId = parsePayload.get("thingId").asString();
                    if (task.getTargetTruck().equals(thingId)) {
                        tasksEvents.sendFinishedEvent(dittoClient, task);
                        logger.info("Task {} for Truck {} finished successful", task.getThingId(), task.getTargetTruck());
                    }
                }
            } else if (Objects.equals(message.getSubject(), TruckEventsActions.TASKFAILED)) {
                tasksEvents.sendFailEvent(dittoClient, task);
                logger.warn("Task {} of Truck {} failed", task.getThingId(), task.getTargetTruck());
            }
            ;
        });
    }


}

