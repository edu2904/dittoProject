package org.example.Factory.ConcreteFactories;

import org.eclipse.ditto.client.DittoClient;
import org.example.util.ThingHandler;
import org.example.Things.TaskThings.TaskType;
import org.example.Things.TaskThings.Task;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class TaskFactory {
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

    public List<Task> getTaskList() {
        return taskList;
    }

}
