package org.example.Gateways.ConcreteGateways;

import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.example.util.Config;
import org.example.Gateways.AbstractGateway;
import org.example.util.ThingHandler;
import org.example.Things.TaskThings.TaskStatus;
import org.example.Things.TaskThings.TaskType;
import org.example.Things.TaskThings.Tasks;
import org.example.Things.TaskThings.TasksEventsActions;
import org.example.Things.TruckThing.Truck;

import java.util.concurrent.*;

public class TaskGateway extends AbstractGateway<Tasks> {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    TasksEventsActions tasksEventsActions = new TasksEventsActions();

    Truck truck;

    ThingHandler thingHandler = new ThingHandler();

    Tasks tasks;

    public TaskGateway(DittoClient dittoClient, InfluxDBClient influxDBClient, Tasks tasks, Truck truck) {
        super(dittoClient, influxDBClient);
        this.truck = truck;
        this.tasks = tasks;
       // tasksEventsActions.startLogging(tasks.getThingId());
    }

    @Override
    public void startGateway() {
        startUpdating(tasks);
    }

    @Override
    public void startUpdating(Tasks tasks)  {
        if(tasks.getTaskType() == TaskType.REFUEL){
            handleRefueling(dittoClient, truck, tasks);
        }
        if(tasks.getTaskType() == TaskType.TIREPRESSUREADJUSTMENT){
            handleTirePressure(dittoClient, truck, tasks);
        }
        if(tasks.getTaskType() == TaskType.LOAD){
            handleLoading(dittoClient, truck, tasks);
        }
    }

    @Override
    public void logToInfluxDB(Tasks thing, String measurementType) {

    }


   // @Override
   // public void subscribeForEventsAndActions() {
    //    tasksEventsActions.startLogging(tasks.getThingId());
   // }


    @Override
    public String getWOTURL() {
        return tasks.getTaskType().getWot();
    }


    @Override
    public void updateAttributes(Tasks tasks) {
        if(tasks.getTaskType() == TaskType.REFUEL) {
            updateAttributeValue("status", tasks.getStatus().toString(), tasks.getThingId());
            updateAttributeValue("targetThing", tasks.getTargetTruck(), tasks.getThingId());
            updateAttributeValue("creationDate", tasks.getCreationTime(), tasks.getThingId());
        }
        if(tasks.getTaskType() == TaskType.TIREPRESSUREADJUSTMENT){
            updateAttributeValue("status", tasks.getStatus().toString(), tasks.getThingId());
            updateAttributeValue("targetThing", tasks.getTargetTruck(), tasks.getThingId());
            updateAttributeValue("creationDate", tasks.getCreationTime(), tasks.getThingId());
        }
        if(tasks.getTaskType() == TaskType.LOAD){
            updateAttributeValue("status", tasks.getStatus().toString(), tasks.getThingId());
            updateAttributeValue("targetThing", tasks.getTargetTruck(), tasks.getThingId());
            updateAttributeValue("creationDate", tasks.getCreationTime(), tasks.getThingId());
        }
    }

    public void handleRefueling(DittoClient dittoClient, Truck truck, Tasks tasks){
        final ScheduledFuture<?>[] future = new ScheduledFuture<?>[1];

        //tasksEventsActions.handleRefuelTaskEvents(dittoClient, tasks);
        tasks.setStatus(TaskStatus.UNDERGOING);


        Runnable updateTask = () -> {

            updateAttributes(tasks);
            double truckCurrentFuelAmount;
            try {
                truckCurrentFuelAmount = (double) getFeatureValueFromDitto("FuelTank", truck.getThingId());


            if(truckCurrentFuelAmount == Config.FUEL_MAX_VALUE_STANDARD_TRUCK){
                tasks.setStatus(TaskStatus.FINISHED);
                updateAttributeValue("status", tasks.getStatus().toString(), tasks.getThingId());

                try {
                    Thread.sleep(1000);
                    thingHandler.deleteThing(dittoClient, tasks.getThingId());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                future[0].cancel(false);
                truck.setTaskActive(false);
            }

                tasksEventsActions.handleRefuelTaskEvents(dittoClient, tasks);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

        };
        ScheduledFuture<?> future1 = scheduler.scheduleAtFixedRate(updateTask, 0, Config.STANDARD_TICK_RATE, TimeUnit.SECONDS);
        future[0] = future1;
    }


    public void handleTirePressure(DittoClient dittoClient, Truck truck, Tasks tasks) {
        final ScheduledFuture<?>[] future = new ScheduledFuture<?>[1];


        tasks.setStatus(TaskStatus.UNDERGOING);


        Runnable updateTask = () -> {

            updateAttributes(tasks);
            double truckCurrentTirePressureAmount;
            try {
                truckCurrentTirePressureAmount = (double) getFeatureValueFromDitto("TirePressure", truck.getThingId());


                if(truckCurrentTirePressureAmount == Config.TIRE_PRESSURE_MAX_VALUE_STANDARD_TRUCK){
                    tasks.setStatus(TaskStatus.FINISHED);
                    updateAttributeValue("status", tasks.getStatus().toString(), tasks.getThingId());

                    try {
                        Thread.sleep(1000);
                        thingHandler.deleteThing(dittoClient, tasks.getThingId());
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    future[0].cancel(false);
                    truck.setTaskActive(false);
                }

                tasksEventsActions.handleTirePressureLowTaskEvents(dittoClient, tasks);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

        };
        ScheduledFuture<?> future1 = scheduler.scheduleAtFixedRate(updateTask, 0, Config.STANDARD_TICK_RATE, TimeUnit.SECONDS);
        future[0] = future1;
    }

    public void handleLoading(DittoClient dittoClient, Truck truck, Tasks tasks){
        final ScheduledFuture<?>[] future = new ScheduledFuture<?>[1];


        tasks.setStatus(TaskStatus.UNDERGOING);


        Runnable updateTask = () -> {

            updateAttributes(tasks);
            double truckCapacity;
            double truckCurrentInventory;
            try {
                truckCurrentInventory = (double) getFeatureValueFromDitto("Inventory", truck.getThingId());
                truckCapacity = (double) getAttributeValueFromDitto("capacity", truck.getThingId());


                if(truckCurrentInventory == truckCapacity){
                    tasks.setStatus(TaskStatus.FINISHED);
                    updateAttributeValue("status", tasks.getStatus().toString(), tasks.getThingId());

                    try {
                        Thread.sleep(1000);
                        thingHandler.deleteThing(dittoClient, tasks.getThingId());
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    future[0].cancel(false);
                    truck.setTaskActive(false);
                }

                tasksEventsActions.handleLoadingTruckTaskEvents(dittoClient, tasks);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

        };
        ScheduledFuture<?> future1 = scheduler.scheduleAtFixedRate(updateTask, 0, Config.STANDARD_TICK_RATE, TimeUnit.SECONDS);
        future[0] = future1;
    }


}


