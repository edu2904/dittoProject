package org.example.SustainableCodeTest.Gateways;

import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.example.Config;
import org.example.SustainableCodeTest.AbstractGateway;
import org.example.ThingHandler;
import org.example.Things.TaskThings.TaskStatus;
import org.example.Things.TaskThings.TaskType;
import org.example.Things.TaskThings.Tasks;
import org.example.Things.TaskThings.TasksEventsActions;
import org.example.Things.TruckThing.Truck;

import java.util.List;
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
        tasksEventsActions.startLogging(tasks.getThingId());
    }

    @Override
    public void startGateway() throws ExecutionException, InterruptedException {
        startUpdating(tasks);
    }

    @Override
    public void startUpdating(Tasks tasks) throws ExecutionException, InterruptedException {
        if(tasks.getTaskType() == TaskType.REFUEL){
            handleRefueling(dittoClient, truck, tasks);
        }
        if(tasks.getTaskType() == TaskType.TIREPRESSUREADJUSTMENT){
            handleTirePressure(dittoClient, truck, tasks);
        }
    }

    @Override
    public void logToInfluxDB(Tasks thing, String measurementType) {

    }

    @Override
    public void handleEvents(Tasks thing) {

    }

    @Override
    public void handelActions(Tasks thing) {

    }

    @Override
    public void subscribeForEventsAndActions() {
        tasksEventsActions.startLogging(tasks.getThingId());
    }


    @Override
    public String getWOTURL() {
        return tasks.getTaskType().getWot();
    }


    @Override
    public void updateAttributes(Tasks tasks) {
        if(tasks.getTaskType() == TaskType.REFUEL) {
            updateAttributeValue("status", tasks.getStatus().toString(), tasks.getThingId());
            updateAttributeValue("targetTruck", tasks.getTargetTruck(), tasks.getThingId());
            updateAttributeValue("creationDate", tasks.getCreationTime(), tasks.getThingId());
        }
        if(tasks.getTaskType() == TaskType.TIREPRESSUREADJUSTMENT){
            updateAttributeValue("status", tasks.getStatus().toString(), tasks.getThingId());
            updateAttributeValue("targetTruck", tasks.getTargetTruck(), tasks.getThingId());
            updateAttributeValue("creationDate", tasks.getCreationTime(), tasks.getThingId());
        }
    }

    public void handleRefueling(DittoClient dittoClient, Truck truck, Tasks tasks) throws ExecutionException, InterruptedException {
        final ScheduledFuture<?>[] future = new ScheduledFuture<?>[1];


        tasks.setStatus(TaskStatus.UNDERGOING);


        Runnable updateTask = () -> {

            updateAttributes(tasks);
            double truckCurrentFuelAmount = 0;
            try {
                truckCurrentFuelAmount = (double) getFeatureValueFromDitto("FuelTank", truck.getThingId());


            if(truckCurrentFuelAmount == Config.FUEL_MAX_VALUE){
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
        ScheduledFuture<?> future1 = scheduler.scheduleAtFixedRate(updateTask, 0, 3, TimeUnit.SECONDS);
        future[0] = future1;
    }


    public void handleTirePressure(DittoClient dittoClient, Truck truck, Tasks tasks) throws ExecutionException, InterruptedException {
        final ScheduledFuture<?>[] future = new ScheduledFuture<?>[1];


        tasks.setStatus(TaskStatus.UNDERGOING);


        Runnable updateTask = () -> {

            updateAttributes(tasks);
            double truckCurrentTirePressureAmount = 0;
            try {
                truckCurrentTirePressureAmount = (double) getFeatureValueFromDitto("TirePressure", truck.getThingId());


                if(truckCurrentTirePressureAmount == Config.TIRE_PRESSURE_MAX_VALUE){
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
        ScheduledFuture<?> future1 = scheduler.scheduleAtFixedRate(updateTask, 0, 3, TimeUnit.SECONDS);
        future[0] = future1;
    }


}


