package org.example.Factory.ConcreteFactories;

import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.things.model.Thing;
import org.example.Factory.DigitalTwinTaskFactory;
import org.example.Gateways.ConcreteGateways.TaskGateway;
import org.example.Things.TaskThings.TaskStatus;
import org.example.util.ThingHandler;
import org.example.Things.TaskThings.TaskType;
import org.example.Things.TaskThings.Task;
import org.example.Things.TruckThing.Truck;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class TaskFactory
{

    //private final TaskType taskType;



    DittoClient dittoClient;

    Task task;
    List<Task> taskList = new ArrayList<>();

    ThingHandler thingHandler = new ThingHandler();

    public TaskFactory(DittoClient dittoClient){
        this.dittoClient = dittoClient;
    }

    public void startTask(Task task){
        try {
            thingHandler.createTwinAndPolicy(dittoClient, task.getTaskType().getWot(), task.getTaskType().getPolicy(), task.getThingId()).toCompletableFuture();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public Task createTask(TaskType taskType, Map<String, Object> useCaseData){
        String thingId = "task:" + taskType + "_" + UUID.randomUUID().toString().substring(0,6);
        Task task = new Task(thingId, taskType);
        useCaseData.forEach(task::putData);
        return task;
    }
/*
    @Override
    public void createTwinsForDitto() throws ExecutionException, InterruptedException {
        initializeTask();
        thingHandler.createTwinAndPolicy(dittoClient, taskType.getWot(), taskType.getPolicy(), task.getThingId()).toCompletableFuture();
        }

    @Override
    public String getWOTURL() {
        return taskType.getWot();
    }

    @Override
    public String getPolicyURL() {
        return taskType.getPolicy();
    }

    @Override
    public void initializeTask() {
        task = new Task();
        switch (taskType){
            case REFUEL:
                initializeRefuelTask();
                break;
            case TIREPRESSUREADJUSTMENT:
                initializeTirePressureTask();
                break;
            case LOAD:
                initializeLoadingTask();
                break;
            case UNLOAD:
                initializeUnloadingTask();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + taskType);
        }


    }

    public void initializeRefuelTask() {
        task.setThingId("task:refuel_" + UUID.randomUUID().toString().substring(0,6));
        task.setStatus(TaskStatus.STARTING);
        task.setTaskType(TaskType.REFUEL);
        task.setCreationTime("Created at: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
        taskList.add(task);
    }
    public void initializeTirePressureTask(){
        task.setThingId("task:tirePressureLow_" + UUID.randomUUID().toString().substring(0,6));
        task.setStatus(TaskStatus.STARTING);
        task.setTaskType(TaskType.TIREPRESSUREADJUSTMENT);
        task.setCreationTime("Created at: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
        taskList.add(task);

    }
    public void initializeLoadingTask(){
        task.setThingId("task:loadingTruck_" + UUID.randomUUID().toString().substring(0,6));
        task.setStatus(TaskStatus.STARTING);
        task.setTaskType(TaskType.LOAD);
        task.setCreationTime("Created at: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
        taskList.add(task);
    }
    public void initializeUnloadingTask(){
        task.setThingId("task:unloadingTruck_" + UUID.randomUUID().toString().substring(0,6));
        task.setStatus(TaskStatus.STARTING);
        task.setTaskType(TaskType.UNLOAD);
        task.setCreationTime("Created at: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
        taskList.add(task);
    }

    @Override
    public Task getTask() {
        return task;
    }

    public List<Task> getAllTasks(){
        return taskList;
    }
    public void removeTaskFromList(Task task){
        taskList.remove(task);
    }

    public void startTask(InfluxDBClient influxDBClient){
        TaskGateway taskGateway = new TaskGateway(dittoClient, influxDBClient, getTask());
        taskGateway.startGateway();

    }

 */
}
