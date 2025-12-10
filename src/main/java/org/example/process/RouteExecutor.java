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
    private final List<Double> taskTimes = new ArrayList<>();


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

        Task task = taskQueue.peek();

        if(task == null){
            long endTime = System.currentTimeMillis();
            double minutes = (endTime-startTime) / 60000.0;
            logger.info("The route {} was finished with executor {} in {} min", route.getRouteId(), route.getExecutor(), minutes);
            return;
        }



        try {

            //There exist an assigned Truck for the route.
            if(route.getExecutor() != null && !route.getExecutor().isEmpty()){
               task.setTargetTrucks(new ArrayList<>(route.getExecutor()));

            }

            taskManager.startTask(task);


        } catch (ExecutionException e) {
            logger.error("Error while executing task {}" , task, e);
            throw new RuntimeException(e);
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            logger.error("Task {} Interrupted", task, e);
            throw new RuntimeException("Task interrupted: " + task, e);
        }
    }

    public void delayTask(){
        logger.info("TASK FAILED, try again in 2 minutes");
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        startNewTask();
    }

    public void removeExecutor(String executor){
        if(route.getExecutor() != null){
            route.removeExecutor(executor);
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

    public List<Double> getTaskTimes() {
        return taskTimes;
    }

    public RoutePlanner.Route getRoute() {
        return this.route;
    }
}
