package org.example.process;

import org.example.Gateways.Temporary.TaskManager;
import org.example.Things.TaskThings.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public synchronized void startNewTask(){
        if(route == null){
            logger.warn("No route found");
            return;
        }

        Task task = taskQueue.peek();

        if(task == null){
            logger.warn("No task found in the queue");
            long endTime = System.currentTimeMillis();
            logger.info("The route {} was finished with executor {} in {} min", route.getRouteId(), route.getExecutor(), (endTime-startTime) / 60000.0);
            return;
        }



        try {
            if(route.getExecutor() != null){
               task.setTargetTruck(route.getExecutor());
               taskQueue.remove();
            }

            taskManager.startTask(task);

            if(task.getTargetTruck() == null){
                return;
            }

            if(route.getExecutor() == null){
                route.setExecutor(task.getTargetTruck());
                taskQueue.remove();
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

    public void delayTask(){
        logger.info("TASK FAILED, try again in 2 minutes");
        try {
            Thread.sleep(120000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        startNewTask();
    }

    public Queue<Task> getTaskQueue() {
        return taskQueue;
    }
}
