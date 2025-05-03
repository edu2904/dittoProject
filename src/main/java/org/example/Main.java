package org.example;

import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.things.model.Thing;
import org.eclipse.ditto.things.model.ThingId;
import org.eclipse.ditto.things.model.ThingsModelFactory;
import org.example.Client.DittoClientBuilder;
import org.example.Things.LKW;

import java.util.concurrent.ExecutionException;


public class Main {
    public static void main(String[] args) throws ExecutionException, InterruptedException {

        DittoClientBuilder dittoClientBuilder = new DittoClientBuilder();
        DittoClient dittoClient = dittoClientBuilder.getDittoClient();
        LKW lkw = new LKW();
        String officialWoTExampleUrl = "https://eclipse-ditto.github.io/ditto-examples/wot/models/floor-lamp-1.0.0.tm.jsonld";
        String lkwPolicy = "https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/lkwpolicy";
        String LKWWOT = "https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/LKW/lkwMain";
       // System.out.println(lkw.thingExist(dittoClient, String.valueOf(ThingId.of("mytest:LKW-10000000000000000000000000000000000000000"))).get());
        lkw.createTwinWithWOT(dittoClient, LKWWOT, lkwPolicy);
        //lkw.createLKWThing(dittoClient);
        //lkw.featureSimulation();
        //Gateway gateway = new Gateway();
        //gateway.startGateway(dittoClient, lkw);

        // lkw.getFuelTankValue(dittoClient);
       // lkw.startUpdatingFuel(dittoClient, 0.5);



    }
}