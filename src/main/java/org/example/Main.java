package org.example;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.eclipse.ditto.base.model.common.HttpStatus;
import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.client.messaging.MessagingProviders;
import org.eclipse.ditto.client.options.Options;
import org.eclipse.ditto.json.JsonFieldSelector;
import org.example.Client.DittoClientBuilder;
import org.example.Factory.ConcreteFactories.TruckFactory;
import org.example.Factory.DigitalTwinFactoryMain;
import org.example.Gateways.GatewayManager;
import org.example.Things.Location;
import org.example.Things.TruckThing.Truck;
import org.example.Things.TruckThing.TruckSimulation;
import org.example.process.TruckProcess;
import org.example.util.ThingHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ExecutionException;


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


        char[] token = "qRQO5nOdFeWKC0Zt_3Uz7ZWImtgFcaUZTOhAcUMrO9dzHzODRMRFainLa380V56XtsjHRMHcSI7Fw2f2RZooWA==".toCharArray();
        //char[] token = "kKchCrZe-eJ2MSXnzpyjKUJn4SXOaq_GHNLwS0qIJFRayxN_ngpz5ZaysqZPDfRwfK0V5heUkGS0mw1Ll72H_A==".toCharArray();
        String org = "admin";
        //String org = "dittoProject";
        String bucket = "ditto";
        InfluxDBClient influxDBClient = InfluxDBClientFactory.create("http://localhost:8086/", token, org, bucket);


        GatewayManager gatewayManager = new GatewayManager(thingClient, listenerClient, influxDBClient);
        gatewayManager.startGateways();
        Thread.sleep(10000);

        TruckProcess truckProcess = new TruckProcess(listenerClient, thingClient, influxDBClient, gatewayManager);
        truckProcess.startProcess();
        Thread.sleep(10000);
        truckProcess.startProcess();

        TruckController truckController = new TruckController(gatewayManager, truckProcess);
        truckController.control();

    }
}

