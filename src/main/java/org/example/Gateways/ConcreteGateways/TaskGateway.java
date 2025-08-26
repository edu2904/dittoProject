package org.example.Gateways.ConcreteGateways;

import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.example.Mapper.TruckMapper;
import org.example.Gateways.AbstractGateway;
import org.example.Things.EventActionHandler;
import org.example.Things.TaskThings.*;
import org.example.Things.TruckThing.TruckEventsActions;
import org.example.util.ThingHandler;
import org.example.Things.TruckThing.Truck;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.ToDoubleFunction;

public class TaskGateway extends AbstractGateway<Task> {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    TasksEvents tasksEvents = new TasksEvents();
    TaskActions taskActions = new TaskActions();


    ThingHandler thingHandler = new ThingHandler();

    Task task;

    public TaskGateway(DittoClient dittoClient, DittoClient listenerClient, InfluxDBClient influxDBClient, Task task) {
        super(dittoClient,listenerClient, influxDBClient);
        this.task = task;
        assignThingToTask();
        registerForThingMessages();
    }

    @Override
    public void startGateway() {
        startUpdating(task);
    }

    @Override
    public void startUpdating(Task task)  {
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

    public void updateStandardTask(Task task){
        updateAttributeValue("status", task.getStatus().toString(), task.getThingId());
        updateAttributeValue("creationDate", task.getCreationTime(), task.getThingId());
        updateAttributeValue("type", task.getTaskType().toString(), task.getThingId());
    }


    public <T> T selectBestThing(List<T> things, ToDoubleFunction<T> score){
        return things.stream().min(Comparator.comparingDouble(score)).orElse(null);
    }

    public void assignThingToTask(){
        List<Truck> trucks = thingHandler.searchThings(dittoClient, new TruckMapper()::fromThing, "truck");

        Truck truck = selectBestThing(trucks, Truck::getUtilization);
        if(truck != null){
            task.setTargetTruck(truck.getThingId());
            updateAttributeValue("targetThing", task.getTargetTruck(), task.getThingId());
            tasksEvents.sendStartEvent(dittoClient, task);
            if(task.getTaskType() == TaskType.LOAD){
                taskActions.sendLoadEvent(dittoClient, task);
                System.out.println("LOAD EVENT SENT");
            }else if(task.getTaskType() == TaskType.UNLOAD){
                taskActions.sendUnloadEvent(dittoClient, task);
                System.out.println("UNLOAD EVENT SENT");
            }
        }else {
            logger.warn("NO BEST THING FOUND");
            task.setStatus(TaskStatus.FAILED);
            tasksEvents.sendFailEvent(dittoClient, task);
        }
    }

    public void registerForThingMessages(){
        listenerClient.live().registerForMessage("thing_" + task.getThingId(), "*", repliableMessage -> {
            if(Objects.equals(repliableMessage.getSubject(), EventActionHandler.TASK_SUCCESS)){
                tasksEvents.sendFinishedEvent(dittoClient, task);
            }else if(Objects.equals(repliableMessage.getSubject(), TruckEventsActions.TASKFAILED)){
                tasksEvents.sendFailEvent(dittoClient, task);
            };
        });
    }


   /* public void handleRefueling(DittoClient dittoClient, Truck truck, Task task){
        final ScheduledFuture<?>[] future = new ScheduledFuture<?>[1];

        //tasksEventsActions.handleRefuelTaskEvents(dittoClient, tasks);
        task.setStatus(TaskStatus.UNDERGOING);


        Runnable updateTask = () -> {

            updateAttributes(task);
            double truckCurrentFuelAmount;
            try {
                truckCurrentFuelAmount = (double) getFeatureValueFromDitto("FuelTank", truck.getThingId());


            if(truckCurrentFuelAmount == Config.FUEL_MAX_VALUE_STANDARD_TRUCK){
                task.setStatus(TaskStatus.FINISHED);
                updateAttributeValue("status", task.getStatus().toString(), task.getThingId());

                try {
                    Thread.sleep(1000);
                    thingHandler.deleteThing(dittoClient, task.getThingId());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                future[0].cancel(false);
                truck.setTaskActive(false);
            }

                tasksEventsActions.handleRefuelTaskEvents(dittoClient, task);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

        };
        ScheduledFuture<?> future1 = scheduler.scheduleAtFixedRate(updateTask, 0, Config.STANDARD_TICK_RATE, TimeUnit.SECONDS);
        future[0] = future1;
    }


    public void handleTirePressure(DittoClient dittoClient, Truck truck, Task task) {
        final ScheduledFuture<?>[] future = new ScheduledFuture<?>[1];


        task.setStatus(TaskStatus.UNDERGOING);


        Runnable updateTask = () -> {

            updateAttributes(task);
            double truckCurrentTirePressureAmount;
            try {
                truckCurrentTirePressureAmount = (double) getFeatureValueFromDitto("TirePressure", truck.getThingId());


                if(truckCurrentTirePressureAmount == Config.TIRE_PRESSURE_MAX_VALUE_STANDARD_TRUCK){
                    task.setStatus(TaskStatus.FINISHED);
                    updateAttributeValue("status", task.getStatus().toString(), task.getThingId());

                    try {
                        Thread.sleep(1000);
                        thingHandler.deleteThing(dittoClient, task.getThingId());
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    future[0].cancel(false);
                    truck.setTaskActive(false);
                }

                tasksEventsActions.handleTirePressureLowTaskEvents(dittoClient, task);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

        };
        ScheduledFuture<?> future1 = scheduler.scheduleAtFixedRate(updateTask, 0, Config.STANDARD_TICK_RATE, TimeUnit.SECONDS);
        future[0] = future1;
    }

    public void handleLoading(DittoClient dittoClient, Truck truck, Task task){
        final ScheduledFuture<?>[] future = new ScheduledFuture<?>[1];


        task.setStatus(TaskStatus.UNDERGOING);


        Runnable updateTask = () -> {

            updateAttributes(task);
            double truckCapacity;
            double truckCurrentInventory;
            try {
                truckCurrentInventory = (double) getFeatureValueFromDitto("Inventory", truck.getThingId());
                truckCapacity = (double) getAttributeValueFromDitto("capacity", truck.getThingId());


                if(truckCurrentInventory == truckCapacity){
                    task.setStatus(TaskStatus.FINISHED);
                    updateAttributeValue("status", task.getStatus().toString(), task.getThingId());

                    try {
                        Thread.sleep(1000);
                        thingHandler.deleteThing(dittoClient, task.getThingId());
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    future[0].cancel(false);
                    truck.setTaskActive(false);
                }

                tasksEventsActions.handleLoadingTruckTaskEvents(dittoClient, task);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

        };
        ScheduledFuture<?> future1 = scheduler.scheduleAtFixedRate(updateTask, 0, Config.STANDARD_TICK_RATE, TimeUnit.SECONDS);
        future[0] = future1;
    }

    */


}


