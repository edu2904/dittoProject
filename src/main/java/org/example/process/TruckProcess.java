package org.example.process;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.base.model.common.HttpStatus;
import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.client.changes.ChangeAction;
import org.eclipse.ditto.client.live.messages.RepliableMessage;
import org.example.Client.DittoClientBuilder;
import org.example.Gateways.Permanent.GatewayManager;
import org.example.Gateways.Temporary.TaskGateway.TaskGateway;
import org.example.Gateways.Temporary.TaskManager;
import org.example.Things.TaskThings.Task;
import org.example.Things.TaskThings.TasksEvents;
import org.example.util.Config;
import org.example.util.ThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

public class TruckProcess {
    private final DittoClientBuilder dittoClientBuilder = new DittoClientBuilder();
    private final Logger logger = LoggerFactory.getLogger(TruckProcess.class);
    private final GatewayManager gatewayManager;
    private final ThingHandler thingHandler = new ThingHandler();
    private final TaskManager taskManager;
    private final RouteRegister routeRegister = new RouteRegister();
    private final List<Double> overallTime = new ArrayList<>();
    private final List<RoutePlanner.Route> allRoutes;
    private int roundCounter = 0;
    private int successfulTasks;
    private int successfulRoutes;
    private int overallTasks;
    private int overallRoutes;
    private int failedTasks;
    private int failedRoutes;
    private int escalatedTasks;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();


    public TruckProcess(DittoClient thingClient, DittoClient listenerClient, InfluxDBClient influxDBClient, GatewayManager gatewayManager) throws ExecutionException, InterruptedException {
        this.gatewayManager = gatewayManager;
        taskManager = new TaskManager(thingClient, listenerClient, influxDBClient);
        deleteAllTasksForNewIteration();
        subscribeForChanges(listenerClient);
        receiveMessages(listenerClient);
        RoutePlanner routePlanner = new RoutePlanner(taskManager, gatewayManager);
        this.allRoutes = routePlanner.createFixedTestRoutes(Config.NUMBER_OF_ROUTES);

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
                logger.info("Received message: subject={}, payload={}",
                        message.getSubject(), optionalObject.map(Object::toString).orElse("null"));

                if (optionalObject.isPresent()) {
                    switch (message.getSubject()) {
                        case TasksEvents.TASK_BEGIN:
                            String rawBeginPayload = optionalObject.get().toString();
                            var parseBeginPayload = Json.parse(rawBeginPayload).asObject();
                            String beginSetId = parseBeginPayload.get("setId").asString();
                            String beginThingId = parseBeginPayload.get("thingId").asString();
                            JsonArray assignedArray = parseBeginPayload.get("targetThings").asArray();
                            List<String> assignedThings = new ArrayList<>();
                            for(int i = 0; i < assignedArray.size(); i++){
                                assignedThings.add(assignedArray.get(i).asString());
                            }
                            routeExecutor = routeRegister.getRegister(beginSetId);
                            if(routeExecutor.getRoute().getExecutor() == null || routeExecutor.getRoute().getExecutor().isEmpty()) {
                                routeExecutor.getRoute().addRouteEVent(beginThingId + " STARTED");
                                routeExecutor.getRoute().setExecutor(assignedThings);
                            }
                            break;

                        case TasksEvents.TASK_FINISHED:
                            //successfulTasks++;
                            String rawPayload = optionalObject.get().toString();
                            var parsePayload = Json.parse(rawPayload).asObject();
                            String finishedSetId = parsePayload.get("setId").asString();
                            String thingId = parsePayload.get("thingId").asString();
                            JsonArray assignedFinishedArray = parsePayload.get("targetThings").asArray();
                            List<String> assignedFinishedThings = new ArrayList<>();
                            for(int i = 0; i < assignedFinishedArray.size(); i++){
                                assignedFinishedThings.add(assignedFinishedArray.get(i).asString());
                            }
                            taskManager.deleteTask(thingId);
                            routeExecutor = routeRegister.getRegister(finishedSetId);


                            if(routeExecutor == null){
                                logger.warn("No RouteExecutor found for setId {}", finishedSetId);
                                return;
                            }

                            successfulTasks++;
                            routeExecutor.getRoute().addRouteEVent(thingId + " FINISHED");
                            routeExecutor.getTaskQueue().remove();
                            routeExecutor.getRoute().setExecutor(assignedFinishedThings);


                            //scheduledExecutorService.schedule(() -> {
                            if(routeExecutor.getTaskQueue().isEmpty()){
                                synchronized (overallTime){
                                    successfulRoutes++;
                                    overallTime.add(routeExecutor.getTaskTimes().stream().mapToDouble(Double::doubleValue).sum());
                                    logger.info("Current average route time: {}", getAverageTime());
                                }

                                TaskGateway.releaseTrucks(routeExecutor.getRoute().getExecutor());

                            }
                            routeExecutor.startNewTask();
                            //}, 5, TimeUnit.SECONDS);
                            break;

                        case TasksEvents.TASK_TIMER:
                            String rawTimedPayload = optionalObject.get().toString();
                            var parseTimedPayload = Json.parse(rawTimedPayload).asObject();
                            String timedSetId = parseTimedPayload.get("setId").asString();
                            double time = parseTimedPayload.get("time").asDouble();
                            routeExecutor = routeRegister.getRegister(timedSetId);
                            routeExecutor.getRoute().addTotalTimeMinutes(time);
                            routeExecutor.getTaskTimes().add(time);
                            break;

                        case TasksEvents.TASK_PAUSED:
                            String rawPausedPayload = optionalObject.get().toString();
                            var parsePausedPayload = Json.parse(rawPausedPayload).asObject();
                            String pausedThingId = parsePausedPayload.get("thingId").asString();

                            String pausedSetId = parsePausedPayload.get("setId").asString();
                            routeExecutor = routeRegister.getRegister(pausedSetId);

                            routeExecutor.getRoute().addRouteEVent(pausedThingId + " PAUSED: " );
                            routeExecutor.delayTask();
                            break;

                        case TasksEvents.TASK_ESCALATED:

                            String rawEscalatedPayload = optionalObject.get().toString();
                            var parseEscalatedPayload = Json.parse(rawEscalatedPayload).asObject();
                            String escalatedSetId = parseEscalatedPayload.get("setId").asString();
                            String escalatedThingId = parseEscalatedPayload.get("thingId").asString();
                            String escalatedEventMessage = parseEscalatedPayload.get("eventMessage").asString();
                            String failedThing = parseEscalatedPayload.get("failedThing").asString();

                            routeExecutor = routeRegister.getRegister(escalatedSetId);
                            if(routeExecutor != null){
                                escalatedTasks++;
                                routeExecutor.removeExecutor(failedThing);

                                routeExecutor.getRoute().addRouteEVent(escalatedThingId + " ESCALATED. REASON: " + escalatedEventMessage);
                            }
                            break;

                        case TasksEvents.TASK_FAILED:

                            failedRoutes++;
                            String rawFailedPayload = optionalObject.get().toString();
                            var parseFinishedPayload = Json.parse(rawFailedPayload).asObject();
                            String failedSetId = parseFinishedPayload.get("setId").asString();
                            String failedThingId = parseFinishedPayload.get("thingId").asString();
                            String failedEventMessage = parseFinishedPayload.get("eventMessage").asString();

                            routeExecutor = routeRegister.getRegister(failedSetId);

                            //for(RoutePlanner.Segment segment : routeExecutor.getRoute().getSegments()){
                            //    failedTasks++;
                            //}

                            failedTasks++;
                            routeExecutor.getRoute().addRouteEVent(failedThingId + " FAILED. REASON: " + failedEventMessage);
                            //routeExecutor.removeExecutor();
                            routeExecutor.deleteRoute();
                            break;
                    }
                }
                message.reply().httpStatus(HttpStatus.OK).payload("response sent for " + message.getPayload()).send();

                //scheduledExecutorService.submit(() -> handleMessages(message));
            });
    }






    public void startSimulation(){
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        scheduledExecutorService.execute(() -> startNextRoute(5));

        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                startNextRoute(5);
            }catch (Exception e){
                logger.error("ERROR WHILE STARTING ROUTE");
            }
        }, 1, 5, TimeUnit.MINUTES);
    }

    private synchronized void startNextRoute(int number){
        for (int i = 0; i < number; i++){
              if(roundCounter >= allRoutes.size()){

                scheduledExecutorService.shutdownNow();
                return;
            }

            RoutePlanner.Route route = allRoutes.get(roundCounter++);
            startFixedProcess(route);
        }
    }
    public void startFixedProcess(RoutePlanner.Route route){

        String routeId = route.getRouteId();
        Queue<Task> taskQueue = new LinkedList<>();
            for (RoutePlanner.Segment segment : route.getSegments()) {
                Map<String, Object> data = new HashMap<>();
                data.put("from", segment.getFrom().getThingId());
                data.put("fromWarehouse", segment.getFrom());
                data.put("to", segment.getTo().getThingId());
                data.put("toWarehouse", segment.getTo());
                data.put("quantity", segment.getQuantity());
                data.put("setId", segment.getSetId());

                Task task = taskManager.createTask(segment.getTaskType(), data);
                task.setSetId(routeId);
                overallTasks++;
                taskQueue.add(task);
            }

            RouteExecutor routeExecutor = new RouteExecutor(taskManager, taskQueue, route);
            overallRoutes++;
            routeRegister.registerExecutor(routeId, routeExecutor);
            routeExecutor.startNewTask();

    }

    public void startRandomProcess(){
        String routeId = "route-" + UUID.randomUUID().toString().substring(0, 6);
        Queue<Task> taskQueue = new LinkedList<>();
        RoutePlanner routePlanner = new RoutePlanner(taskManager, gatewayManager);
        RoutePlanner.Route route = routePlanner.createRandomRoute();
        route.setRouteId(routeId);



       for (RoutePlanner.Segment segment : route.getSegments()) {
            Map<String, Object> data = new HashMap<>();
            data.put("from", segment.getFrom().getThingId());
            data.put("fromWarehouse", segment.getFrom());
            data.put("to", segment.getTo().getThingId());
            data.put("toWarehouse", segment.getTo());
            data.put("quantity", segment.getQuantity());
            data.put("setId", segment.getSetId());

            Task task = taskManager.createTask(segment.getTaskType(), data);
            task.setSetId(routeId);
            overallTasks++;
            taskQueue.add(task);
        }

        RouteExecutor routeExecutor = new RouteExecutor(taskManager, taskQueue, route);
        overallRoutes++;
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
    public int getFailedTasks() {
        return failedTasks;
    }

    public int getSuccessfullTasks() {
        return successfulTasks;
    }

    public int getSuccessfullRoutes() {
        return successfulRoutes;
    }

    public int getOveralLTasks() {
        return overallTasks;
    }

    public int getOverallRoutes() {
        return overallRoutes;
    }

    public int getEscalatedTasks() {
        return escalatedTasks;
    }

    public int getFailedRoutes() {
        return failedRoutes;
    }

    public List<RoutePlanner.Route> getAllRoutes() {
        return allRoutes;
    }
}
