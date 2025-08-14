package org.example.process;

import org.eclipse.ditto.base.model.common.HttpStatus;
import org.eclipse.ditto.client.DittoClient;
import org.example.Client.DittoClientBuilder;
import org.example.Gateways.GatewayManager;
import org.example.Things.TruckThing.Truck;
import org.example.Things.TruckThing.TruckEventsActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class TruckProcess {
    DittoClient dittoClient;
    DittoClientBuilder dittoClientBuilder = new DittoClientBuilder();
    private final Logger logger = LoggerFactory.getLogger(Truck.class);
    GatewayManager gatewayManager;

    public TruckProcess(DittoClient dittoClient, GatewayManager gatewayManager) throws ExecutionException, InterruptedException {
        this.dittoClient = dittoClient;
        this.gatewayManager = gatewayManager;
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
                    case TruckEventsActions.TRUCKARRIVED, TruckEventsActions.TRUCKWAITNGTOOLONG:
                        logger.info(message.getPayload().toString());

                        break;
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
