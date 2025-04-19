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
        lkw.createLKWThing(dittoClient);

        // lkw.getFuelTankValue(dittoClient);
       // lkw.startUpdatingFuel(dittoClient, 0.5);



    }
}