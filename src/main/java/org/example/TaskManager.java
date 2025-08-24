package org.example;

import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.example.Gateways.ConcreteGateways.TaskGateway;
import org.example.Mapper.TaskMapper;
import org.example.Things.TaskThings.Task;
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
    private final DittoClient listenerClient;
    private InfluxDBClient influxDBClient;
    ThingHandler thingHandler = new ThingHandler();
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public TaskManager(DittoClient dittoClient, DittoClient listenerClient, InfluxDBClient influxDBClient){
        this.dittoClient = dittoClient;
        this.listenerClient = listenerClient;
        this.influxDBClient = influxDBClient;
    }

    public void startTask(Task task) throws ExecutionException, InterruptedException {
        //Task task = createNewTask(taskType);
        startTaskGateway(task);

    }
/*
    public Task createNewTask(TaskType taskType){
        TaskFactory taskFactory = new TaskFactory(dittoClient, taskType);
        try {
            taskFactory.createTwinsForDitto();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return taskFactory.getTask();
    }

 */


    public void startTaskGateway(Task task){
        TaskGateway taskGateway = new TaskGateway(dittoClient, listenerClient, influxDBClient, task);

        scheduler.scheduleAtFixedRate(taskGateway::startGateway,0 , Config.STANDARD_TICK_RATE, TimeUnit.SECONDS);
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
                                .sort(s -> s.asc("thingId"))).fields("thingId"))
                .toList()
                .forEach(foundThing -> {
                    Task task = TaskMapper.fromThing(foundThing);
                    System.out.println("Found thing: " + foundThing);
                    currentTasks.add(task);
                });
        return currentTasks;
    }
}
