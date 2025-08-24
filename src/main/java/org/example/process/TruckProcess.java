package org.example.process;

import com.eclipsesource.json.Json;
import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.base.model.common.HttpStatus;
import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.client.changes.ChangeAction;
import org.example.Client.DittoClientBuilder;
import org.example.Factory.ConcreteFactories.TaskFactory;
import org.example.Gateways.GatewayManager;
import org.example.TaskManager;
import org.example.Things.TaskThings.Task;
import org.example.Things.TruckThing.Truck;
import org.example.Things.TaskThings.TasksEvents;
import org.example.util.ThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class TruckProcess {
    private final DittoClient processClient;
    private final DittoClient taskClient;
    private final DittoClientBuilder dittoClientBuilder = new DittoClientBuilder();
    private final Logger logger = LoggerFactory.getLogger(Truck.class);
    private final GatewayManager gatewayManager;
    private final TaskFactory taskFactory;
    private final ThingHandler thingHandler = new ThingHandler();
    private final InfluxDBClient influxDBClient;
    private TaskManager taskManager;
    private final RouteRegister routeRegister = new RouteRegister();
    public TruckProcess(DittoClient processClient, DittoClient taskClient, InfluxDBClient influxDBClient, GatewayManager gatewayManager) throws ExecutionException, InterruptedException {
        this.taskClient = taskClient;
        this.processClient = processClient;
        this.influxDBClient = influxDBClient;
        this.gatewayManager = gatewayManager;
        this.taskFactory = new TaskFactory(processClient);
        subscribeForChanges(processClient);
        receiveMessages(processClient);
        startProcess();
        //receiveActions(processClient);
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
    public void receiveMessages(DittoClient dittoClient) {
            dittoClient.live().registerForMessage("test2", "*", message -> {

                switch (message.getSubject()) {
                    case TasksEvents.TASKFINISHED:
                        Optional<?> optionalObject = message.getPayload();
                        if(optionalObject.isPresent()) {
                            String rawPayload = optionalObject.get().toString();
                            var parsePayload = Json.parse(rawPayload).asObject();
                            String finishedSetId = parsePayload.get("setId").asString();
                            RouteExecutor routeExecutor = routeRegister.getRegister(finishedSetId);
                            if(routeExecutor != null){
                                routeExecutor.startNewTask();
                            }
                        }
                        break;
                }
                message.reply().httpStatus(HttpStatus.OK).payload("response sent for " + message.getPayload()).send();
            });
    }
    public void startProcess(){
        String routeId = "route-" + UUID.randomUUID().toString().substring(0, 6);
        Queue<Task> taskQueue = new LinkedList<>();
        taskManager = new TaskManager(taskClient, influxDBClient);
        RoutePlanner routePlanner = new RoutePlanner(taskManager, gatewayManager);
        RoutePlanner.Route route = routePlanner.createRoute();
        deleteAllTasksForNewIteration();

        for(RoutePlanner.Segment segment : route.getSegments()) {
                Map<String, Object> data = new HashMap<>();
                data.put("from", segment.getFrom());
                data.put("to", segment.getTo());
                data.put("quantity", segment.getQuantity());
                data.put("setId", segment.getSetId());

                Task task = taskFactory.createTask(segment.getTaskType(), data);
                task.setSetId(routeId);
                taskQueue.add(task);
        }
        RouteExecutor routeExecutor = new RouteExecutor(taskManager, taskQueue, taskFactory);
        routeRegister.registerExecutor(routeId, routeExecutor);
        routeExecutor.startNewTask();
    }

    public void deleteAllTasksForNewIteration(){
        List<Task> taskList = taskManager.getallTasks();
        for(Task task : taskList){
            taskManager.deleteTask(task);
            logger.info("Task {} was deleted", task.getThingId());
        }
    }

    public void receiveActions(DittoClient dittoClient){
        dittoClient.live().registerForClaimMessage("test3", String.class, claimMessage -> {
            logger.info("Received claim Message from client1: {}", claimMessage.getSubject());
            claimMessage.reply().httpStatus(HttpStatus.ACCEPTED).payload("claim").send();
        });

    }



}
