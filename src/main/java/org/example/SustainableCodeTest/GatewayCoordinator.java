package org.example.SustainableCodeTest;

import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.example.Client.DittoClientBuilder;
import org.example.SustainableCodeTest.Gateways.TruckGateway;
import org.example.Things.TruckThing.Truck;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GatewayCoordinator {
    DittoClient dittoClient;
    InfluxDBClient influxDBClient;

    List<Truck> truckList;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);


    public GatewayCoordinator(List<Truck> trucks, DittoClient dittoClient) throws ExecutionException, InterruptedException {
        this.dittoClient = dittoClient;
        this.truckList = trucks;
    }

    public void startGateways(){
        TruckGateway truckGateway = new TruckGateway(truckList, dittoClient, influxDBClient);
        Runnable updateTask = () -> {


            for(Truck truck: truckList){
                truckGateway.startUpdating(truck);
            }

        };
        scheduler.scheduleAtFixedRate(updateTask, 0, 3, TimeUnit.SECONDS);
    }

}
