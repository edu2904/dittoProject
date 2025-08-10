package org.example.process;

import okio.Utf8;
import org.eclipse.ditto.base.model.common.HttpStatus;
import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.client.DittoClients;
import org.eclipse.ditto.json.JsonObject;
import org.example.Client.DittoClientBuilder;
import org.example.Things.TruckThing.Truck;
import org.example.Things.TruckThing.TruckEventsActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

public class TruckProcess {
    DittoClient dittoClient;
    DittoClientBuilder dittoClientBuilder = new DittoClientBuilder();
    private final Logger logger = LoggerFactory.getLogger(Truck.class);

    public TruckProcess(DittoClient dittoClient) throws ExecutionException, InterruptedException {
        this.dittoClient = dittoClient;
        subscribeForChanges(dittoClient);
        receiveMessages(dittoClient);
        //receiveActions(dittoClient);
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
                    case TruckEventsActions.WEIGHTEVENT, TruckEventsActions.TRUCKARRIVED:
                        System.out.println("+++++++++++++++++++++++++++++");
                        logger.info(message.getPayload().toString());
                        System.out.println("+++++++++++++++++++++++++++++");
                        break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                message.reply().httpStatus(HttpStatus.OK).payload("response sent for " + message.getPayload()).send();
            });


    }
    public void receiveActions(DittoClient dittoClient){
        dittoClient.live().registerForClaimMessage("test3", String.class, claimMessage -> {
            logger.info("Received claim Message from client1: {}", claimMessage.getSubject());
            claimMessage.reply().httpStatus(HttpStatus.ACCEPTED).payload("claim").send();
        });

    }



}
