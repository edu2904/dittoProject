package org.example.Factory.ConcreteFactories;

import org.eclipse.ditto.client.DittoClient;
import org.example.Factory.DigitalTwinFactory;
import org.example.Things.TaskThings.TaskStatus;
import org.example.util.ThingHandler;
import org.example.Things.TaskThings.TaskType;
import org.example.Things.TaskThings.Tasks;
import org.example.Things.TruckThing.Truck;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class TaskFactory implements DigitalTwinFactory<Tasks>
{

    private final TaskType taskType;

    public final Truck truck;

    DittoClient dittoClient;

    Tasks task;

    ThingHandler thingHandler = new ThingHandler();

    public TaskFactory(DittoClient dittoClient, TaskType taskType, Truck truck){
        this.dittoClient = dittoClient;
        this.taskType = taskType;
        this.truck = truck;
    }

    @Override
    public void createTwinsForDitto() throws ExecutionException, InterruptedException {
        initializeThings();
        thingHandler.createTwinAndPolicy(dittoClient, getWOTURL(), getPolicyURL(), task.getThingId()).toCompletableFuture();
        }

    @Override
    public String getWOTURL() {
        return taskType.getWot();
    }

    @Override
    public String getPolicyURL() {
        return "https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/lkwpolicy";
    }

    @Override
    public void initializeThings() throws InterruptedException {
        task = new Tasks();
        switch (taskType){
            case REFUEL:
                initializeRefuelTask(truck);
                break;
            case TIREPRESSUREADJUSTMENT:
                initializeTirePressureTask(truck);
                break;
            case LOAD:
                initializeLoadingTask(truck);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + taskType);
        }


    }

    public void initializeRefuelTask(Truck truck) {
        task.setThingId("task:refuel_" + truck.getThingId());
        task.setStatus(TaskStatus.STARTING);
        task.setTaskType(TaskType.REFUEL);
        task.setTargetTruck(truck.getThingId());
        task.setCreationTime("Created at: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
    }
    public void initializeTirePressureTask(Truck truck){
        task.setThingId("task:tirePressureLow_" + truck.getThingId());
        task.setStatus(TaskStatus.STARTING);
        task.setTaskType(TaskType.TIREPRESSUREADJUSTMENT);
        task.setTargetTruck(truck.getThingId());
        task.setCreationTime("Created at: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));

    }
    public void initializeLoadingTask(Truck truck){
        task.setThingId("task:loadingTruck_" + truck.getThingId());
        task.setStatus(TaskStatus.STARTING);
        task.setTaskType(TaskType.LOAD);
        task.setTargetTruck(truck.getThingId());
        task.setCreationTime("Created at: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
    }

    @Override
    public List<Tasks> getThings() {
        return Collections.singletonList(task);
    }


}
