package org.example.Gateways.Temporary;

import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.example.Factory.ConcreteFactories.TaskFactory;
import org.example.Gateways.AbstractGateway;
import org.example.Gateways.Temporary.TaskGateway.TaskGateway;
import org.example.Mapper.TaskMapper;
import org.example.Things.TaskThings.Task;
import org.example.Things.TaskThings.TaskType;
import org.example.util.Config;
import org.example.util.ThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;


// orchestrates the tasks

public class TaskManager {
    private final DittoClient dittoClient;
    private final DittoClient listenerClient;
    private final InfluxDBClient influxDBClient;
    private final ThingHandler thingHandler = new ThingHandler();
    private final TaskFactory taskFactory;
    private final Map<String, ScheduledExecutorService> taskSchedulers = new ConcurrentHashMap<>();
    protected final Logger logger = LoggerFactory.getLogger(TaskManager.class);

    public TaskManager(DittoClient dittoClient, DittoClient listenerClient, InfluxDBClient influxDBClient){
        this.dittoClient = dittoClient;
        this.listenerClient = listenerClient;
        this.influxDBClient = influxDBClient;
        taskFactory = new TaskFactory(dittoClient);
    }


    // starts the tasks and its taskGateway. Afterwards, it is visible in Eclipse Ditto and is updated
    public void startTask(Task task) throws ExecutionException, InterruptedException {
        task.startTimeTracking();
        taskFactory.startTask(task);
        startTaskGateway(task);

    }

    // created the task object and is stored withing the process. After the process decides that the tasks needs to be executed, the startTask(Task task) method will activate it.
    public Task createTask(TaskType taskType, Map<String, Object> useCaseData){
        return taskFactory.createTask(taskType, useCaseData);
    }


    // initiates the TaskGateway
    public void startTaskGateway(Task task){
        String thingId = task.getThingId();


        // shuts down potential old schedulers to avoid race conditions
        ScheduledExecutorService oldScheduler = taskSchedulers.remove(thingId);
        if (oldScheduler != null && !oldScheduler.isShutdown()) {
            oldScheduler.shutdownNow();
        }



        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        TaskGateway taskGateway = new TaskGateway(dittoClient, listenerClient, influxDBClient, task);


        // updates the task with new values every three seconds
        scheduler.scheduleAtFixedRate(() -> {
                    try {
                        taskGateway.startGateway();
                    } catch (Throwable T) {
                        System.out.println(T);
                    }
                }, 0, Config.STANDARD_TICK_RATE, TimeUnit.SECONDS);
         taskSchedulers.put(task.getThingId(), scheduler);
    }


    // deletes a task
    public void deleteTask(String thingId){
        logger.info("Delete task: {}", thingId);

        ScheduledExecutorService currentScheduler = taskSchedulers.remove(thingId);

        System.out.println(taskSchedulers.size());
        if(currentScheduler != null){
            logger.info("shutdown scheduler for {}", thingId);

            currentScheduler.shutdownNow();
        }
        thingHandler.deleteThing(dittoClient, thingId);
    }

    // retrieve a singel task as a JSON-file
    public Task getTask(String thingID){
        Optional<Task> task = dittoClient
                .twin()
                .search()
                .stream(queryBuilder -> queryBuilder.filter("like(thingId," + thingID + ")")
                        .options(o -> o.size(1)))
                .map(TaskMapper::fromThing).findFirst();
        return task.orElse(null);
    }


    // retrieve all task as a JSON-file
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
