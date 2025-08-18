package org.example.process;

import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.base.model.common.HttpStatus;
import org.eclipse.ditto.client.DittoClient;
import org.example.Client.DittoClientBuilder;
import org.example.Factory.ConcreteFactories.TaskFactory;
import org.example.Factory.ConcreteFactories.TruckFactory;
import org.example.Gateways.GatewayManager;
import org.example.TaskManager;
import org.example.Things.TaskThings.Task;
import org.example.Things.TaskThings.TaskType;
import org.example.Things.TruckThing.Truck;
import org.example.Things.TruckThing.TruckEventsActions;
import org.example.util.ThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TruckProcess {
    DittoClient processClient;
    DittoClient taskClient;
    DittoClientBuilder dittoClientBuilder = new DittoClientBuilder();
    private final Logger logger = LoggerFactory.getLogger(Truck.class);
    GatewayManager gatewayManager;
    ThingHandler thingHandler = new ThingHandler();
    InfluxDBClient influxDBClient;
    TaskManager taskManager;
    public TruckProcess(DittoClient processClient, DittoClient taskClient, InfluxDBClient influxDBClient) throws ExecutionException, InterruptedException {
        this.taskClient = taskClient;
        this.processClient = processClient;
        this.influxDBClient = influxDBClient;
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
        deleteAllTasksForNewIteration();
        try {
            taskManager.startTask(TaskType.LOAD);
            /*ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.schedule(() -> {
                List<Task> tasks = taskManager.searchTask();
                for(Task task : tasks){
                    System.out.println("*********************");
                    System.out.println(task.getThingId());
                    System.out.println("*********************");
                    //taskManager.deleteTask(task);
                }
            }, 1, TimeUnit.MINUTES);

             */

        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
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
