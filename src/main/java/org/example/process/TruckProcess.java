package org.example.process;

import com.eclipsesource.json.Json;
import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.base.model.common.HttpStatus;
import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.client.changes.ChangeAction;
import org.example.Client.DittoClientBuilder;
import org.example.Gateways.Permanent.GatewayManager;
import org.example.Gateways.Temporary.TaskManager;
import org.example.Things.TaskThings.Task;
import org.example.Things.TaskThings.TasksEvents;
import org.example.util.ThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class TruckProcess {
    private final DittoClientBuilder dittoClientBuilder = new DittoClientBuilder();
    private final Logger logger = LoggerFactory.getLogger(TruckProcess.class);
    private final GatewayManager gatewayManager;
    private final ThingHandler thingHandler = new ThingHandler();
    private final TaskManager taskManager;
    private final RouteRegister routeRegister = new RouteRegister();
    private final List<Double> overallTime = new ArrayList<>();
    public TruckProcess(DittoClient thingClient, DittoClient listenerClient, InfluxDBClient influxDBClient, GatewayManager gatewayManager) throws ExecutionException, InterruptedException {
        this.gatewayManager = gatewayManager;
        taskManager = new TaskManager(thingClient, listenerClient, influxDBClient);
        deleteAllTasksForNewIteration();
        subscribeForChanges(listenerClient);
        receiveMessages(listenerClient);

    }
    public void subscribeForChanges(DittoClient dittoClient) {
        dittoClient.twin().registerForThingChanges(UUID.randomUUID().toString(), thingChange -> {
            CompletableFuture.runAsync(() -> {
                if (thingChange.getAction() != ChangeAction.MERGED) {
                    logger.info(thingChange.getAction().toString() + " " + thingChange.getThing().toString());
                }
                if (Objects.equals(thingChange.getAction(), ChangeAction.CREATED)) {
                    logger.info("{}, {}", thingChange.getAction(), thingChange.getThing());
                }
            });
        });


    }

    //Receives messages from tasks. Determines next Action when message is received.
    public void receiveMessages(DittoClient dittoClient) {
            dittoClient.live().registerForMessage("test2", "*", message -> {
                Optional<?> optionalObject = message.getPayload();
                RouteExecutor routeExecutor;
                if (optionalObject.isPresent()) {
                    switch (message.getSubject()) {
                        case TasksEvents.TASK_FINISHED:
                            String rawPayload = optionalObject.get().toString();
                            var parsePayload = Json.parse(rawPayload).asObject();
                            String finishedSetId = parsePayload.get("setId").asString();
                            String thingId = parsePayload.get("thingId").asString();
                            taskManager.deleteTask(thingId);
                            routeExecutor = routeRegister.getRegister(finishedSetId);

                            if(routeExecutor == null){
                                logger.warn("No RouteExecutor found for setId {}", finishedSetId);
                                return;
                            }
                            CompletableFuture.runAsync(() -> {
                                try {
                                    Thread.sleep(5000);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }

                                if(routeExecutor.getTaskQueue().isEmpty()){
                                    synchronized (overallTime){
                                        overallTime.add(routeExecutor.getTaskTimes().stream().mapToDouble(Double::doubleValue).sum());
                                        logger.info("Current average route time: {}", getAverageTime());
                                    }
                                }
                                routeExecutor.startNewTask();

                            });
                            break;
                        case TasksEvents.TASK_TIMER:
                            String rawTimedPayload = optionalObject.get().toString();
                            var parseTimedPayload = Json.parse(rawTimedPayload).asObject();
                            String timedSetId = parseTimedPayload.get("setId").asString();
                            double time = parseTimedPayload.get("time").asDouble();
                            routeExecutor = routeRegister.getRegister(timedSetId);
                            routeExecutor.getTaskTimes().add(time);
                            break;

                        case TasksEvents.TASK_PAUSED:
                            String rawPausedPayload = optionalObject.get().toString();
                            var parsePausedPayload = Json.parse(rawPausedPayload).asObject();
                            String pausedSetId = parsePausedPayload.get("setId").asString();
                            routeExecutor = routeRegister.getRegister(pausedSetId);
                            routeExecutor.delayTask();
                            break;

                    case TasksEvents.TASK_ESCALATED:
                        String rawEscalatedPayload = optionalObject.get().toString();
                        var parseEscalatedPayload = Json.parse(rawEscalatedPayload).asObject();
                        String escalatedSetId = parseEscalatedPayload.get("setId").asString();
                        routeExecutor = routeRegister.getRegister(escalatedSetId);
                        routeExecutor.removeExecutor();
                        routeExecutor.startNewTask();
                        break;

                    case TasksEvents.TASK_FAILED:
                        String rawFailedPayload = optionalObject.get().toString();
                        var parseFinishedPayload = Json.parse(rawFailedPayload).asObject();
                        String failedSetId = parseFinishedPayload.get("setId").asString();
                        routeExecutor = routeRegister.getRegister(failedSetId);
                        routeExecutor.deleteRoute();
                        break;
                }
                }
                message.reply().httpStatus(HttpStatus.OK).payload("response sent for " + message.getPayload()).send();
            });
    }
    public void startProcess(){
        String routeId = "route-" + UUID.randomUUID().toString().substring(0, 6);
        Queue<Task> taskQueue = new LinkedList<>();
        RoutePlanner routePlanner = new RoutePlanner(taskManager, gatewayManager);
        RoutePlanner.Route route = routePlanner.createRoute();
        route.setRouteId(routeId);


        for(RoutePlanner.Segment segment : route.getSegments()) {
                Map<String, Object> data = new HashMap<>();
                data.put("from", segment.getFrom());
                data.put("to", segment.getTo().getThingId());
                data.put("toWarehouse", segment.getTo());
                data.put("quantity", segment.getQuantity());
                data.put("setId", segment.getSetId());

                Task task = taskManager.createTask(segment.getTaskType(), data);
                task.setSetId(routeId);
                taskQueue.add(task);
        }
        RouteExecutor routeExecutor = new RouteExecutor(taskManager, taskQueue, route);
        routeRegister.registerExecutor(routeId, routeExecutor);
        routeExecutor.startNewTask();
    }

    public void deleteAllTasksForNewIteration(){
        List<Task> taskList = taskManager.getallTasks();
        for(Task task : taskList){
            taskManager.deleteTask(task.getThingId());
            logger.info("Task {} was deleted", task.getThingId());
        }
    }
    public void receiveActions(DittoClient dittoClient){
        dittoClient.live().registerForClaimMessage("test3", String.class, claimMessage -> {
            logger.info("Received claim Message from client1: {}", claimMessage.getSubject());
            claimMessage.reply().httpStatus(HttpStatus.ACCEPTED).payload("claim").send();
        });
    }

    public List<Double> getOveralLTime() {
        return Collections.unmodifiableList(overallTime);
    }
    public double getAverageTime() {
        if(overallTime.isEmpty()) return 0.0;
        System.out.println("****************************************");
        System.out.println(overallTime.size());
        System.out.println("****************************************");
        return overallTime.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

}
