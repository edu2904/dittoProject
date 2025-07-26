package org.example.Gateways;

import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.example.Config;
import org.example.Factory.DigitalTwinFactoryMain;
import org.example.Factory.ConcreteFactories.GasStationFactory;
import org.example.Factory.ConcreteFactories.TruckFactory;
import org.example.Factory.ConcreteFactories.WarehouseFactory;
import org.example.Gateways.ConcreteGateways.GasStationGateway;
import org.example.Gateways.ConcreteGateways.TruckGateway;
import org.example.Gateways.ConcreteGateways.WarehouseGateway;
import org.example.ThingHandler;
import org.example.Things.GasStationThing.GasStation;
import org.example.Things.TruckThing.Truck;
import org.example.Things.WarehouseThing.Warehouse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GatewayManager {

    private final DigitalTwinFactoryMain digitalTwinFactoryMain;
    private final DittoClient dittoClient;
    InfluxDBClient influxDBClient;
    ThingHandler thingHandler = new ThingHandler();

    protected final Logger logger = LoggerFactory.getLogger(AbstractGateway.class);


    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);


    public GatewayManager(DittoClient dittoClient, InfluxDBClient influxDBClient) throws ExecutionException, InterruptedException {
        this.dittoClient = dittoClient;
        this.influxDBClient = influxDBClient;
        this.digitalTwinFactoryMain = new DigitalTwinFactoryMain(dittoClient);
    }

    public void safeDeleteTasksBeforeRestart(List<Truck> truckList) throws ExecutionException, InterruptedException {
        for (Truck truck : truckList) {
            if (thingHandler.thingExists(dittoClient, "task:refuel_" + truck.getThingId()).get()) {
                thingHandler.deleteThing(dittoClient, "task:refuel_" + truck.getThingId()).toCompletableFuture().get();
            }
            if (thingHandler.thingExists(dittoClient, "task:tirePressureLow_" + truck.getThingId()).get()) {
                thingHandler.deleteThing(dittoClient, "task:tirePressureLow_" + truck.getThingId()).toCompletableFuture().get();
            }
            if (thingHandler.thingExists(dittoClient, "task:loadingTruck_" + truck.getThingId()).get()) {
                thingHandler.deleteThing(dittoClient, "task:loadingTruck_" + truck.getThingId()).toCompletableFuture().get();
            }
        }
    }

    public void createPermanentThings() throws ExecutionException, InterruptedException {
        digitalTwinFactoryMain.getTruckFactory().createTwinsForDitto();
        digitalTwinFactoryMain.getGasStationFactory().createTwinsForDitto();
        digitalTwinFactoryMain.getWarehouseFactory().createTwinsForDitto();
    }

    public void startGateways() throws ExecutionException, InterruptedException {

        createPermanentThings();

        List<Truck> trucks = ((TruckFactory) digitalTwinFactoryMain.getTruckFactory()).getTruckList();
        GasStation gasStation1 = ((GasStationFactory) digitalTwinFactoryMain.getGasStationFactory()).getGasStation();
        Warehouse warehouseMain = ((WarehouseFactory) digitalTwinFactoryMain.getWarehouseFactory()).getWarehouseMain();

        for(Truck truck: trucks){
            truck.setGasStation(gasStation1);
            truck.setWarehouseMain(warehouseMain);
        }



        TruckGateway truckGateway = new TruckGateway(dittoClient, influxDBClient,trucks);
        GasStationGateway gasStationGateway = new GasStationGateway(dittoClient, influxDBClient, gasStation1);
        WarehouseGateway warehouseGateway = new WarehouseGateway(dittoClient, influxDBClient, warehouseMain);



        safeDeleteTasksBeforeRestart(trucks);


        Runnable updateTask = () -> {
            try {
                truckGateway.startGateway();
                gasStationGateway.startGateway();
                warehouseGateway.startGateway();
            } catch (ExecutionException | InterruptedException e) {
                logger.error("ERROR in updating", e);
            }

        };
        scheduler.scheduleAtFixedRate(updateTask, 0, Config.STANDARD_TICK_RATE, TimeUnit.SECONDS);
    }

}
