package org.example.Gateways;

import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.example.Things.TruckThing.TruckTargetDecision;
import org.example.util.Config;
import org.example.Factory.DigitalTwinFactoryMain;
import org.example.Gateways.ConcreteGateways.GasStationGateway;
import org.example.Gateways.ConcreteGateways.TruckGateway;
import org.example.Gateways.ConcreteGateways.WarehouseGateway;
import org.example.util.ThingHandler;
import org.example.Things.GasStationThing.GasStation;
import org.example.Things.TruckThing.Truck;
import org.example.Things.WarehouseThing.Warehouse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
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
    List<Truck> truckList;
    List<Warehouse> warehouseList;
    List<GasStation> gasStationList;

    TruckGateway truckGateway;
    WarehouseGateway warehouseGateway;
    GasStationGateway gasStationGateway;


    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);


    public GatewayManager(DittoClient dittoClient, InfluxDBClient influxDBClient) throws ExecutionException, InterruptedException {
        this.dittoClient = dittoClient;
        this.influxDBClient = influxDBClient;
        this.digitalTwinFactoryMain = new DigitalTwinFactoryMain(dittoClient);
    }


    public void createPermanentThings() throws ExecutionException, InterruptedException {
        digitalTwinFactoryMain.getTruckFactory().createTwinsForDitto();
        digitalTwinFactoryMain.getGasStationFactory().createTwinsForDitto();
        digitalTwinFactoryMain.getWarehouseFactory().createTwinsForDitto();
    }

    public void startGateways() throws ExecutionException, InterruptedException {

        createPermanentThings();

        truckList = digitalTwinFactoryMain.getTruckFactory().getThings();
        gasStationList = digitalTwinFactoryMain.getGasStationFactory().getThings();
        warehouseList = digitalTwinFactoryMain.getWarehouseFactory().getThings();

        for(Truck truck: truckList) {
            for (GasStation gasStation : gasStationList) {
                gasStation.featureSimulation();
                truck.setGasStation(gasStation);
            }
            for (Warehouse warehouse : warehouseList){
                warehouse.featureSimulation();

                truck.setWarehouseList(warehouse);
        }
            truck.featureSimulation1(dittoClient);
        }

        truckGateway = new TruckGateway(dittoClient, influxDBClient,truckList);
        gasStationGateway = new GasStationGateway(dittoClient, influxDBClient, gasStationList);
        warehouseGateway = new WarehouseGateway(dittoClient, influxDBClient, warehouseList);

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

    public List<Warehouse> getWarehouseList() {
        return warehouseList;
    }

    public List<Truck> getTruckList() {
        return truckList;
    }






    public void setDecisionForNextDestination(Truck truck) throws ExecutionException, InterruptedException {
        double fuel = truckGateway.getFuelFromDitto(truck);
        Map<String, Double> distances = truck.calculateDistances();

        TruckTargetDecision<?> bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        double weightDistance = 0.6;
        double weightUtilization = 0.4;
        //double weightFuel = 0.1;

        for(GasStation gasStation : gasStationList){
            Double distanceGasStation = distances.get(gasStation.getThingId());
            if(distanceGasStation == null){
                continue;
            }
            double utilizationGasStation = gasStationGateway.getUtilizationFromDitto(gasStation);
            double urgencyLowFuel = fuel < 40 ? (1-(fuel/Config.FUEL_MAX_VALUE_STANDARD_TRUCK)) * 75 : 0;
            double urgencyHighFuel = fuel > 150 ? (fuel/Config.FUEL_MAX_VALUE_STANDARD_TRUCK) * 75 : 0;


            double cost = weightDistance * distanceGasStation + weightUtilization * utilizationGasStation - urgencyLowFuel + urgencyHighFuel;

            if(cost < bestScore){
                bestScore = cost;
                bestTarget = new TruckTargetDecision<>(gasStation, distanceGasStation, gasStation.getLocation(), gasStation.getThingId());
            }
        }
        int currentStopIndex = truck.getCurrentStopIndex().get();
        System.out.println("CURRENT INDEX " + currentStopIndex + " WarehouseSize: " + warehouseList.size());
        if(currentStopIndex >= 1 && currentStopIndex < warehouseList.size()) {
                Warehouse nextWarehouse = warehouseList.get(currentStopIndex);
                logger.info("Check for potential travel to {}", nextWarehouse.getThingId());
                Double distanceWarehouse = distances.get(nextWarehouse.getThingId());
                if (distanceWarehouse != null) {

                    double utilizationWarehouse = warehouseGateway.getUtilizationFromDitto(nextWarehouse);
                    System.out.println("Warehouse utilization " + utilizationWarehouse);
                    double cost = weightDistance * distanceWarehouse + weightUtilization * utilizationWarehouse;

                    System.out.println(nextWarehouse.getThingId() + ": " + cost);
                    if (cost < bestScore) {
                        bestScore = cost;
                        bestTarget = new TruckTargetDecision<>(nextWarehouse, distanceWarehouse, nextWarehouse.getLocation(), nextWarehouse.getThingId());
                    }
                }
            } else if(currentStopIndex >= warehouseList.size()){
                Warehouse nextWarehouse = warehouseList.get(0);
                logger.info("Check for potential travel to {}", nextWarehouse.getThingId());
                Double distanceWarehouse = distances.get(nextWarehouse.getThingId());
                if (distanceWarehouse != null) {

                    double utilizationWarehouse = warehouseGateway.getUtilizationFromDitto(nextWarehouse);

                    double cost = weightDistance * distanceWarehouse + weightUtilization * utilizationWarehouse;

                    System.out.println(nextWarehouse.getThingId() + ": " + cost);
                    if (cost < bestScore) {
                        bestScore = cost;
                        bestTarget = new TruckTargetDecision<>(nextWarehouse, distanceWarehouse, nextWarehouse.getLocation(), nextWarehouse.getThingId());
                    }
                }
            }
            if(bestTarget != null){
            truck.setRecommendedTarget(bestTarget);
            logger.info("Next Target for {} is {}" , truck.getThingId(), bestTarget.getDecidedTarget() instanceof GasStation ? ((GasStation) bestTarget.getDecidedTarget()).getThingId() : ((Warehouse) bestTarget.getDecidedTarget()).getThingId());
        }

    }
}
