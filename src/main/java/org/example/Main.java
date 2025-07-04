package org.example;

import org.eclipse.ditto.client.DittoClient;
import org.example.Client.DittoClientBuilder;
import org.example.SustainableCodeTest.Factory.Things.TruckFactory;
import org.example.SustainableCodeTest.GatewayCoordinator;
import org.example.Things.GasStationThing.GasStation;
import org.example.Things.TruckThing.Truck;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class Main {
    public static void main(String[] args) throws ExecutionException, InterruptedException {

        DittoClientBuilder dittoClientBuilder = new DittoClientBuilder();
        DittoClient dittoClient = dittoClientBuilder.getDittoClient();

        GatewayCoordinator gatewayCoordinator = new GatewayCoordinator(dittoClient);
        gatewayCoordinator.startGateways();

/*
        GatewayMain gateway = new GatewayMain();

        gateway.initializeThings();


        ThingHandler thing = new ThingHandler();
        thing.deleteThing(dittoClient, "task:refuel");
        List<Truck> truckList = gateway.getTruckList();

        //Truck truck1 = new Truck(gateway);
        //truck1.setStarterValues(1);
        //Truck truck2 = new Truck(gateway);
        //truck2.setStarterValues(2);
        //truckList.add(truck1);
        //truckList.add(truck2);

        for (Truck truck : truckList) {
            if (thing.thingExists(dittoClient, "task:refuel_" + truck.getThingId()).get()) {
                thing.deleteThing(dittoClient, "task:refuel_" + truck.getThingId());
            }
        }

        truckList.get(0).featureSimulation1(dittoClient);
        truckList.get(1).featureSimulation2(dittoClient);

        GasStation gasStation = gateway.getGasStation();
        gasStation.featureSimulation();





        String officialWoTExampleUrl = "https://eclipse-ditto.github.io/ditto-examples/wot/models/floor-lamp-1.0.0.tm.jsonld";
        String lkwPolicy = "https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/lkwpolicy";
        String LKWWOT = "https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/LKW/lkwMain?cb=" + System.currentTimeMillis();
        String GasStationWOTLink = "https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/GasStation/gasstationmain?cb=" + System.currentTimeMillis();
        String policyID = thing.getPolicyFromURL(lkwPolicy).get();



        thing.createTwinAndPolicy(dittoClient, GasStationWOTLink, lkwPolicy, gasStation.getThingId()).thenRun(() -> {
                    try {
                        gateway.startGasStationGateway(dittoClient, gasStation);
                    } catch (ExecutionException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
        ).toCompletableFuture();

        for (int i = 0; i < truckList.size(); i++ ){
            int finalI = i;
            thing.createTwinAndPolicy(dittoClient, LKWWOT, lkwPolicy, truckList.get(finalI).getThingId()).thenRun(() -> {
                try {
                    gateway.startLKWGateway(dittoClient, truckList.get(finalI));
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).toCompletableFuture();
        }


*/
    }




}