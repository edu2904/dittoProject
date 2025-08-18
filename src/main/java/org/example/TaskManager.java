package org.example;

import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.example.Factory.ConcreteFactories.TaskFactory;
import org.example.Gateways.ConcreteGateways.TaskGateway;
import org.example.Gateways.GatewayManager;
import org.example.Things.TaskThings.TaskType;
import org.example.Things.TaskThings.Task;
import org.example.Things.TruckThing.Truck;
import org.example.util.Config;
import org.example.util.ThingHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TaskManager {
    private final DittoClient dittoClient;
    private InfluxDBClient influxDBClient;
    ThingHandler thingHandler = new ThingHandler();

    public TaskManager(DittoClient dittoClient, InfluxDBClient influxDBClient){
        this.dittoClient = dittoClient;
        this.influxDBClient = influxDBClient;
    }
    public TaskManager(DittoClient dittoClient){
        this.dittoClient = dittoClient;
    }
    public void startTask(TaskType taskType) throws ExecutionException, InterruptedException {
        Task task = createNewTask(taskType);
        startTaskGateway(task);

    }

    public Task createNewTask(TaskType taskType){
        TaskFactory taskFactory = new TaskFactory(dittoClient, taskType);
        try {
            taskFactory.createTwinsForDitto();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return taskFactory.getTask();
    }
    public void startTaskGateway(Task task){
        TaskGateway taskGateway = new TaskGateway(dittoClient, influxDBClient, task);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.schedule(() -> {
        taskGateway.startUpdating(task);
        }, Config.STANDARD_TICK_RATE, TimeUnit.SECONDS);
    }
    public void deleteTask(Task task){
        thingHandler.deleteThing(dittoClient, task.getThingId());
    }
    public Task getTask(String thingID){
        Optional<Task> task = dittoClient
                .twin()
                .search()
                .stream(queryBuilder -> queryBuilder.filter("like(thingId," + thingID + ")")
                        .options(o -> o.size(1)))
                .map(TaskMapper::fromThing).findFirst();
        return task.orElse(null);
    }

    public List<Task> getallTasks(){
        List<Task> currentTasks = new ArrayList<>();
        dittoClient.twin().search()
                .stream(queryBuilder -> queryBuilder.filter("like(thingId,'task:*')")
                        .options(o -> o.size(20)
                                .sort(s -> s.asc("thingId"))))
                .map(TaskMapper::fromThing).toList()
                .forEach(foundThing -> {
                    System.out.println("Found thing: " + foundThing);
                    currentTasks.add(foundThing);
                });
        return currentTasks;
    }
}
