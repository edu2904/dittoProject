package org.example.process;

import org.example.Factory.ConcreteFactories.TaskFactory;
import org.example.TaskManager;
import org.example.Things.TaskThings.Task;

import java.util.Queue;
import java.util.concurrent.ExecutionException;

public class RouteExecutor {

    private final TaskManager taskManager;
    private final Queue<Task> taskQueue;
    private TaskFactory taskFactory;
    public RouteExecutor(TaskManager taskManager, Queue<Task> taskQueue, TaskFactory taskFactory){
        this.taskManager = taskManager;
        this.taskQueue = taskQueue;
        this.taskFactory = taskFactory;
    }
    public void startNewTask(){
        Task task = taskQueue.poll();
        try {
            assert task != null;
            taskFactory.startTask(task);
            taskManager.startTask(task);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Queue<Task> getTaskQueue() {
        return taskQueue;
    }
}
