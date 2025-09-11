package org.example.process;

import org.example.Factory.ConcreteFactories.TaskFactory;
import org.example.TaskManager;
import org.example.Things.TaskThings.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

public class RouteExecutor {

    private final TaskManager taskManager;
    private final Queue<Task> taskQueue;
    private final RoutePlanner.Route route;
    private final Logger logger = LoggerFactory.getLogger(RouteExecutor.class);
    private final long startTime;


    public RouteExecutor(TaskManager taskManager, Queue<Task> taskQueue, RoutePlanner.Route route){
        this.taskManager = Objects.requireNonNull(taskManager);
        this.taskQueue = Objects.requireNonNull(taskQueue);
        this.route = route;
        this.startTime = System.currentTimeMillis();


    }
    public synchronized void startNewTask(){
        if(route == null){
            logger.warn("No route found");
            return;
        }

        Task task = taskQueue.poll();

        if(task == null){
            logger.warn("No task found in the queue");
            long endTime = System.currentTimeMillis();
            System.out.println("**************************************");
            System.out.printf("The Route " + route.getRouteId() + " was finished with executor " + route.getExecutor() + " in: %.2f min%n", ((endTime - startTime) / 60000.0));
            System.out.println("**************************************");
            return;
        }
        try {
            if(route.getExecutor() != null){
               task.setTargetTruck(route.getExecutor());
            }

            taskManager.startTask(task);

            if(route.getExecutor() == null){
                route.setExecutor(task.getTargetTruck());
            }

        } catch (ExecutionException e) {
            logger.error("Error while executing task {}" , task, e);
            throw new RuntimeException(e);
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            logger.error("Task {} Interrupted", task, e);
            throw new RuntimeException("Task interrupted: " + task, e);
        }
    }

    public Queue<Task> getTaskQueue() {
        return taskQueue;
    }
}
