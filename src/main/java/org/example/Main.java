package org.example;

import org.eclipse.ditto.client.DittoClient;
import org.example.Client.DittoClientBuilder;
import org.example.Things.LKW;

import java.io.IOException;
import java.util.concurrent.ExecutionException;


public class Main {
    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {

        DittoClientBuilder dittoClientBuilder = new DittoClientBuilder();
        DittoClient dittoClient = dittoClientBuilder.getDittoClient();


        ThingHandler lkwThing = new ThingHandler();

        LKW lkw1 = new LKW();
        lkw1.featureSimulation();

        System.out.println(lkw1.getThingId());
        Gateway gateway = new Gateway();


        String officialWoTExampleUrl = "https://eclipse-ditto.github.io/ditto-examples/wot/models/floor-lamp-1.0.0.tm.jsonld";
        String lkwPolicy = "https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/lkwpolicy";
        String LKWWOT = "https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/LKW/lkwMain?cb=" + System.currentTimeMillis();
        String policyID = lkwThing.getPolicyFromURL(lkwPolicy).get();




        if(!(lkwThing.policyExists(dittoClient, policyID).get())){
            lkwThing.createTwinAndPolicy(dittoClient, LKWWOT, lkwPolicy, lkw1.getThingId()).thenRun(() -> {
                try {
                    gateway.startGateway(dittoClient, lkw1);
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }else {
            lkwThing.deleteThingandPolicy(dittoClient,  policyID, lkw1.getThingId());
            lkwThing.createTwinAndPolicy(dittoClient, LKWWOT, lkwPolicy, lkw1.getThingId());
            gateway.startGateway(dittoClient, lkw1);
        }



    }


}