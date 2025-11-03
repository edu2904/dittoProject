package org.example.process;

import org.example.Gateways.Temporary.TaskManager;
import org.example.Things.TaskThings.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ExecutionException;

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
    public synchronized Double startNewTask(){
        if(route == null){
            logger.warn("No route found");
            return null;
        }

        Task task = taskQueue.peek();

        if(task == null){
            long endTime = System.currentTimeMillis();
            double minutes = (endTime-startTime) / 60000.0;
            logger.info("The route {} was finished with executor {} in {} min", route.getRouteId(), route.getExecutor(), minutes);
            return minutes;
        }



        try {

            //There exist an assigned Truck for the route.
            if(route.getExecutor() != null){
               task.setTargetTruck(route.getExecutor());
               taskQueue.remove();
               taskManager.startTask(task);
               return null;
            }

            //Task doesn't have truck
            if(task.getTargetTruck() == null){
                taskManager.startTask(task);
                return null;
            }

            route.setExecutor(task.getTargetTruck());
            taskQueue.remove();
            startNewTask();

        } catch (ExecutionException e) {
            logger.error("Error while executing task {}" , task, e);
            throw new RuntimeException(e);
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            logger.error("Task {} Interrupted", task, e);
            throw new RuntimeException("Task interrupted: " + task, e);
        }
        return null;
    }

    public void delayTask(){
        logger.info("TASK FAILED, try again in 2 minutes");
        try {
            Thread.sleep(1200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        startNewTask();
    }

    public void removeExecutor(){
        if(route.getExecutor() != null){
            route.setExecutor(null);
        }
    }
    public void deleteRoute(){
        for(Task task : getTaskQueue()){
            taskManager.deleteTask(task.getThingId());
        }
    }

    public Queue<Task> getTaskQueue() {
        return taskQueue;
    }
}
