package org.example.Factory.ConcreteFactories;

import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.example.Factory.DigitalTwinTaskFactory;
import org.example.Gateways.ConcreteGateways.TaskGateway;
import org.example.Things.TaskThings.TaskStatus;
import org.example.util.ThingHandler;
import org.example.Things.TaskThings.TaskType;
import org.example.Things.TaskThings.Task;
import org.example.Things.TruckThing.Truck;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class TaskFactory implements DigitalTwinTaskFactory
{

    private final TaskType taskType;



    DittoClient dittoClient;

    Task task;
    List<Task> taskList = new ArrayList<>();

    ThingHandler thingHandler = new ThingHandler();

    public TaskFactory(DittoClient dittoClient, TaskType taskType){
        this.taskType = taskType;
        this.dittoClient = dittoClient;
    }

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
}
