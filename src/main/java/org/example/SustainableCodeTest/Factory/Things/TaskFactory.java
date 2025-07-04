package org.example.SustainableCodeTest.Factory.Things;

import org.eclipse.ditto.client.DittoClient;
import org.example.SustainableCodeTest.Factory.DigitalTwinFactory;
import org.example.ThingHandler;
import org.example.Things.TaskThings.TaskType;
import org.example.Things.TaskThings.Tasks;
import org.example.Things.TruckThing.Truck;

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
                task.initializeRefuelTask(truck);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + taskType);
        }


    }
    public TaskType getTaskType(Tasks tasks){
        return tasks.getTaskType();
    }

    public Tasks getTasks() {
        return task;
    }

}
