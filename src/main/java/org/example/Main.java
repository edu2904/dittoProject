package org.example;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.client.options.Options;
import org.example.Client.DittoClientBuilder;
import org.example.Gateways.Permanent.GatewayManager;
import org.example.Things.TaskThings.TaskActions;
import org.example.process.RoutePlanner;
import org.example.process.TruckProcess;
import org.example.util.Config;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class Main {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        DittoClientBuilder dittoClientBuilder = new DittoClientBuilder();
        DittoClientBuilder dittoClientBuilder1 = new DittoClientBuilder();

        DittoClient listenerClient = dittoClientBuilder1.getDittoClient();
        DittoClient thingClient = dittoClientBuilder.getDittoClient();


        listenerClient.live().startConsumption().toCompletableFuture().join();
        System.out.println("PROCESS LIVE CLIENT ESTABLISHED");
        listenerClient.twin().startConsumption(Options.Consumption.filter("exists(attributes/targetThing)")).toCompletableFuture().join();
        System.out.println("PROCESS TWIN CLIENT ESTABLISHED");
        thingClient.live().startConsumption(
                Options.Consumption.filter("exists(attributes/targetThing)")
        ).toCompletableFuture().join();
        System.out.println("THING LIVE CLIENT ESTABLISHED");
        thingClient.twin().startConsumption(
                Options.Consumption.filter("and(exists(attributes/targetThing),ne(attributes/targetThing,\"\"))")
        ).toCompletableFuture().join();
        System.out.println("THING TWIN CLIENT ESTABLISHED");


        //char[] token = "qRQO5nOdFeWKC0Zt_3Uz7ZWImtgFcaUZTOhAcUMrO9dzHzODRMRFainLa380V56XtsjHRMHcSI7Fw2f2RZooWA==".toCharArray();
        //char[] token = "kKchCrZe-eJ2MSXnzpyjKUJn4SXOaq_GHNLwS0qIJFRayxN_ngpz5ZaysqZPDfRwfK0V5heUkGS0mw1Ll72H_A==".toCharArray();
        String org = "admin";
        //String org = "dittoProject";
        String bucket = "ditto";
        InfluxDBClient influxDBClient = InfluxDBClientFactory.create("http://localhost:8086/", Config.INFLUX_TOKEN.toCharArray(), Config.INFLUX_ORG, Config.INFLUX_BUCKET);


        GatewayManager gatewayManager = new GatewayManager(thingClient, listenerClient, influxDBClient);
        gatewayManager.startGateways();
        Thread.sleep(5000);

        TruckProcess truckProcess = new TruckProcess(thingClient, listenerClient, influxDBClient, gatewayManager);
        truckProcess.startSimulation();

        TruckControllerHTTPServer truckControllerHTTPServer = new TruckControllerHTTPServer(gatewayManager, truckProcess);
        truckControllerHTTPServer.control();


        TruckControllerGUI truckControllerGUI = new TruckControllerGUI(gatewayManager, truckProcess);
        truckControllerGUI.create();

/*
        List<RoutePlanner.Route> routes = truckProcess.getAllRoutes();

        Path out = Path.of(
                System.getProperty("user.home"),
                "Downloads",
                "routes2.xlsx"
        );

        new ExcelWriter().exportRoutesToExcel(routes, truckProcess.getAverageTime(), out);
        System.out.println("EXCEL EXPORT");

 */



       // ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
/*
        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("Starting new task at " + System.currentTimeMillis());
                truckProcess.startRandomProcess();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 4, TimeUnit.MINUTES);


 */


    }
}



