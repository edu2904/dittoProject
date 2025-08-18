package org.example.process;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.OnboardingRequest;
import org.eclipse.ditto.base.model.common.HttpStatus;
import org.eclipse.ditto.client.DittoClient;
import org.example.Client.DittoClientBuilder;
import org.example.Factory.ConcreteFactories.TaskFactory;
import org.example.Gateways.GatewayManager;
import org.example.TaskManager;
import org.example.Things.TaskThings.Task;
import org.example.Things.TaskThings.TaskType;
import org.example.Things.TruckThing.Truck;
import org.example.Things.TruckThing.TruckEventsActions;
import org.example.util.ThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class TruckProcess {
    DittoClient processClient;
    DittoClient taskClient;
    DittoClientBuilder dittoClientBuilder = new DittoClientBuilder();
    private final Logger logger = LoggerFactory.getLogger(Truck.class);
    GatewayManager gatewayManager;
    private TaskFactory taskFactory;
    ThingHandler thingHandler = new ThingHandler();
    InfluxDBClient influxDBClient;
    TaskManager taskManager;
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
                if(!Objects.equals(thingChange.getAction().toString(), "MERGED")) {
                    logger.info("{}", thingChange.getAction());
                }
            });

    }
    public void receiveMessages(DittoClient dittoClient) {
         dittoClient.live().registerForMessage("test2", "*", message -> {
                switch (message.getSubject()) {
                    case TruckEventsActions.TRUCKARRIVED, TruckEventsActions.TRUCKWAITNGTOOLONG:
                        logger.info(message.getPayload().toString());
                        break;
                }
                message.reply().httpStatus(HttpStatus.OK).payload("response sent for " + message.getPayload()).send();
            });


    }
    public void startProcess(){
        taskManager = new TaskManager(processClient, influxDBClient);
        RoutePlanner routePlanner = new RoutePlanner(taskManager, gatewayManager);
        RoutePlanner.Route route = routePlanner.createRoute();
        deleteAllTasksForNewIteration();

        for(RoutePlanner.Segment segment : route.getSegments()) {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("from", segment.getFrom());
                data.put("to", segment.getTo());
                data.put("quantity", segment.getQuantity());

                Task task = taskFactory.startTask(segment.getTaskType(), data);
                taskManager.startTask(task);

            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
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
