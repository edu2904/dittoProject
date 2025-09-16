package org.example.Gateways.Temporary;

import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.example.Factory.ConcreteFactories.TaskFactory;
import org.example.Gateways.Temporary.TaskGateway.TaskGateway;
import org.example.Mapper.TaskMapper;
import org.example.Things.TaskThings.Task;
import org.example.Things.TaskThings.TaskType;
import org.example.util.Config;
import org.example.util.ThingHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

public class TaskManager {
    private final DittoClient dittoClient;
    private final DittoClient listenerClient;
    private final InfluxDBClient influxDBClient;
    private final ThingHandler thingHandler = new ThingHandler();
    private final TaskFactory taskFactory;
    private final Map<String, ScheduledExecutorService> taskSchedulers = new ConcurrentHashMap<>();
    public TaskManager(DittoClient dittoClient, DittoClient listenerClient, InfluxDBClient influxDBClient){
        this.dittoClient = dittoClient;
        this.listenerClient = listenerClient;
        this.influxDBClient = influxDBClient;
        taskFactory = new TaskFactory(dittoClient);
    }

    public void startTask(Task task) throws ExecutionException, InterruptedException {
        taskFactory.startTask(task);
        startTaskGateway(task);

    }

    public Task createTask(TaskType taskType, Map<String, Object> useCaseData){
        return taskFactory.createTask(taskType, useCaseData);
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
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        TaskGateway taskGateway = new TaskGateway(dittoClient, listenerClient, influxDBClient, task);

        scheduler.scheduleAtFixedRate(taskGateway::startGateway,0 , Config.STANDARD_TICK_RATE, TimeUnit.SECONDS);
        taskSchedulers.put(task.getThingId(), scheduler);
    }

    public void deleteTask(String thingId){
        thingHandler.deleteThing(dittoClient, thingId);

        ScheduledExecutorService currentScheduler = taskSchedulers.remove(thingId);
        if(currentScheduler != null){
            currentScheduler.shutdownNow();
        }
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
