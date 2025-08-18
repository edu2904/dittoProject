package org.example.Gateways.ConcreteGateways;

import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.example.util.Config;
import org.example.Gateways.AbstractGateway;
import org.example.util.ThingHandler;
import org.example.Things.TaskThings.TaskStatus;
import org.example.Things.TaskThings.TaskType;
import org.example.Things.TaskThings.Task;
import org.example.Things.TaskThings.TasksEventsActions;
import org.example.Things.TruckThing.Truck;

import java.util.concurrent.*;

public class TaskGateway extends AbstractGateway<Task> {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    TasksEventsActions tasksEventsActions = new TasksEventsActions();


    ThingHandler thingHandler = new ThingHandler();

    Task task;

    public TaskGateway(DittoClient dittoClient, InfluxDBClient influxDBClient, Task task) {
        super(dittoClient, influxDBClient);
        this.task = task;
       // tasksEventsActions.startLogging(tasks.getThingId());
    }

    @Override
    public void startGateway() {
        startUpdating(task);
    }

    @Override
    public void startUpdating(Task task)  {
        if(task.getTaskType() == TaskType.REFUEL){
            updateAttributes(task);
           // handleRefueling(dittoClient, truck, task);
        }
        if(task.getTaskType() == TaskType.TIREPRESSUREADJUSTMENT){
            updateAttributes(task);
        //    handleTirePressure(dittoClient, truck, task);
        }
        if(task.getTaskType() == TaskType.LOAD){
            updateAttributes(task);
         //   handleLoading(dittoClient, truck, task);
        }
        if(task.getTaskType() == TaskType.UNLOAD){
            updateAttributes(task);
        }
    }

    @Override
    public void logToInfluxDB(Task thing, String measurementType) {

    }


   // @Override
   // public void subscribeForEventsAndActions() {
    //    tasksEventsActions.startLogging(tasks.getThingId());
   // }


    @Override
    public String getWOTURL() {
        return task.getTaskType().getWot();
    }


    @Override
    public void updateAttributes(Task task) {
        if(task.getTaskType() == TaskType.REFUEL) {
           updateStandardTask(task);
        }
        if(task.getTaskType() == TaskType.TIREPRESSUREADJUSTMENT){
            updateStandardTask(task);
        }
        if(task.getTaskType() == TaskType.LOAD){
            updateStandardTask(task);
        }
        if(task.getTaskType() == TaskType.UNLOAD){
            updateStandardTask(task);
        }
    }

    public void updateStandardTask(Task task){
        updateAttributeValue("status", task.getStatus().toString(), task.getThingId());
        //updateAttributeValue("targetThing", task.getTargetTruck(), task.getThingId());
        updateAttributeValue("creationDate", task.getCreationTime(), task.getThingId());
        updateAttributeValue("type", task.getTaskType().toString(), task.getThingId());
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


