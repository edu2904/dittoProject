package org.example;

import org.eclipse.ditto.base.model.common.HttpStatus;
import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.things.model.ThingId;
import org.example.Client.DittoClientBuilder;
import org.example.Things.GasStation;
import org.example.Things.GasStationStatus;
import org.example.Things.LKW;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class Main {
    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {

        DittoClientBuilder dittoClientBuilder = new DittoClientBuilder();
        DittoClient dittoClient = dittoClientBuilder.getDittoClient();



        ThingHandler thing = new ThingHandler();

        List<LKW> truckList = new ArrayList<>();

        LKW lkw1 = new LKW();
        lkw1.setStarterVaules(1);
        LKW lkw2 = new LKW();
        lkw2.setStarterVaules(2);
        lkw1.featureSimulation1();
        lkw2.featureSimulation2();
        truckList.add(lkw1);
        truckList.add(lkw2);


        GasStation gasStation = new GasStation();
        gasStation.featureSimulation();

        Gateway gateway = new Gateway();



        String officialWoTExampleUrl = "https://eclipse-ditto.github.io/ditto-examples/wot/models/floor-lamp-1.0.0.tm.jsonld";
        String lkwPolicy = "https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/lkwpolicy";
        String LKWWOT = "https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/LKW/lkwMain?cb=" + System.currentTimeMillis();
        String GasStationWOTLink = "https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/GasStation/gasstationmain?cb=" + System.currentTimeMillis();
        String policyID = thing.getPolicyFromURL(lkwPolicy).get();



        thing.createTwinAndPolicy(dittoClient, GasStationWOTLink, lkwPolicy, gasStation.getThingId()).thenRun(() -> {
                    try {
                        gateway.startGasStationGateway(dittoClient, gasStation);
                        System.out.println("GATEWAY STATATATATATAT");
                    } catch (ExecutionException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
        ).toCompletableFuture();

        for (int i = 0; i < truckList.size(); i++ ){
        //if(!(lkwThing.policyExists(dittoClient, policyID).get())){
            int finalI = i;
            thing.createTwinAndPolicy(dittoClient, LKWWOT, lkwPolicy, truckList.get(finalI).getThingId()).thenRun(() -> {
                try {
                    System.out.println(finalI);

                    gateway.startLKWGateway(dittoClient, truckList.get(finalI));
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).toCompletableFuture();
            /*
        }else {
            int finalI = i;
            lkwThing.deleteThingandPolicy(dittoClient, policyID, truckList.get(0).getThingId());
            lkwThing.createTwinAndPolicy(dittoClient, LKWWOT, lkwPolicy, lkw1.getThingId()).thenRun(() -> {
                try {
                    gateway.startGateway(dittoClient, truckList.get(finalI));
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            ;

        }

             */
        }



    }


}