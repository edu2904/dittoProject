package org.example;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import org.eclipse.ditto.base.model.common.HttpStatus;
import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.client.messaging.MessagingProviders;
import org.eclipse.ditto.client.options.Options;
import org.eclipse.ditto.json.JsonFieldSelector;
import org.example.Client.DittoClientBuilder;
import org.example.Factory.DigitalTwinFactoryMain;
import org.example.Gateways.GatewayManager;
import org.example.Things.TruckThing.Truck;
import org.example.process.TruckProcess;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;


public class Main {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        DittoClientBuilder dittoClientBuilder = new DittoClientBuilder();
        DittoClientBuilder dittoClientBuilder1 = new DittoClientBuilder();
        DittoClientBuilder dittoClientBuilder2 = new DittoClientBuilder();

        DittoClient processClient = dittoClientBuilder1.getDittoClient();
        DittoClient taskClient = dittoClientBuilder.getDittoClient();
        DittoClient thingClient = dittoClientBuilder.getDittoClient();


        taskClient.live().startConsumption().toCompletableFuture().get();
        System.out.println("ENGINE LIVE CLIENT ESTABLISHED");
        taskClient.twin().startConsumption().toCompletableFuture().get();
        System.out.println("ENGINE TWIN CLIENT ESTABLISHED");
        processClient.live().startConsumption().toCompletableFuture().get();
        System.out.println("PROCESS LIVE CLIENT ESTABLISHED");
        processClient.twin().startConsumption().toCompletableFuture().get();
        System.out.println("PROCESS TWIN CLIENT ESTABLISHED");
        thingClient.live().startConsumption(
                Options.Consumption.filter("exists(attributes/targetThing)")
        ).toCompletableFuture().get();
        System.out.println("THING LIVE CLIENT ESTABLISHED");
        thingClient.twin().startConsumption(
                Options.Consumption.filter("exists(attributes/targetThing)")
        ).toCompletableFuture().get();
        System.out.println("THING TWIN CLIENT ESTABLISHED");



        //char[] token = "qRQO5nOdFeWKC0Zt_3Uz7ZWImtgFcaUZTOhAcUMrO9dzHzODRMRFainLa380V56XtsjHRMHcSI7Fw2f2RZooWA==".toCharArray();
        char[] token = "kKchCrZe-eJ2MSXnzpyjKUJn4SXOaq_GHNLwS0qIJFRayxN_ngpz5ZaysqZPDfRwfK0V5heUkGS0mw1Ll72H_A==".toCharArray();
        //String org = "admin";
        String org = "dittoProject";
        String bucket = "ditto";
        InfluxDBClient influxDBClient = InfluxDBClientFactory.create("http://localhost:8086/", token, org, bucket);



        GatewayManager gatewayManager = new GatewayManager(thingClient, influxDBClient);
        gatewayManager.startGateways();

        TruckProcess truckProcess = new TruckProcess(processClient, taskClient, influxDBClient, gatewayManager);





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


/*
    public Truck selectTrucks(){
        List<Truck> truckList = gatewayManager.getTruckList();
        Truck bestTruck;
        bestTruck = truckList.get(0);
        for(Truck truck : truckList){
            if(truck.getUtilization() < bestTruck.getUtilization()){
                bestTruck = truck;
            }
        }
        return bestTruck;
    }

 */
}

