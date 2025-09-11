package org.example.Gateways;

import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.example.Mapper.TruckMapper;
import org.example.Things.TruckThing.TruckSimulation;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GatewayManager {

    private final DigitalTwinFactoryMain digitalTwinFactoryMain;
    private final DittoClient thingClient;
    private final DittoClient listenerClient;
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


    public GatewayManager(DittoClient thingClient, DittoClient listenerClient, InfluxDBClient influxDBClient) throws ExecutionException, InterruptedException {
        this.thingClient = thingClient;
        this.listenerClient =listenerClient;
        this.influxDBClient = influxDBClient;
        this.digitalTwinFactoryMain = new DigitalTwinFactoryMain(thingClient);
    }


    public void createPermanentThings() throws ExecutionException, InterruptedException {
        digitalTwinFactoryMain.getTruckFactory().initializeThings();
        digitalTwinFactoryMain.getTruckFactory().createTwinsForDitto();
        digitalTwinFactoryMain.getGasStationFactory().createTwinsForDitto();
        digitalTwinFactoryMain.getWarehouseFactory().createTwinsForDitto();
    }


    public void startGateways() throws ExecutionException, InterruptedException {


        deleteAllTrucks();
        createPermanentThings();

        truckList = digitalTwinFactoryMain.getTruckFactory().getThings();
        gasStationList = digitalTwinFactoryMain.getGasStationFactory().getThings();
        warehouseList = digitalTwinFactoryMain.getWarehouseFactory().getThings();

        truckGateway = new TruckGateway(thingClient, listenerClient, influxDBClient,truckList);
        gasStationGateway = new GasStationGateway(thingClient, listenerClient, influxDBClient, gasStationList);
        warehouseGateway = new WarehouseGateway(thingClient, listenerClient, influxDBClient, warehouseList);


        for (GasStation gasStation : gasStationList) {
            gasStation.featureSimulation();
        }
        for (Warehouse warehouse : warehouseList){
            warehouse.featureSimulation();
        }

        for(Truck truck : truckList){
            truck.setGasStationList(new ArrayList<>(gasStationList));
            truck.setWarehouseList(new ArrayList<>(warehouseList));
            TruckSimulation truckSimulation = new TruckSimulation(thingClient, truck);
            truckSimulation.runSimulation(thingClient, this);
        }

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

    public List<GasStation> getGasStationList() {
        return gasStationList;
    }

    public TruckGateway getTruckGateway() {
        return truckGateway;
    }

    public WarehouseGateway getWarehouseGateway() {
        return warehouseGateway;
    }

    public GasStationGateway getGasStationGateway() {
        return gasStationGateway;
    }

    public DittoClient getListenerClient() {
        return listenerClient;
    }

    public DittoClient getThingClient() {
        return thingClient;
    }

    public void deleteAllTrucks(){
        TruckMapper truckMapper = new TruckMapper();
        thingClient.twin().search()
                .stream(queryBuilder -> queryBuilder.filter("like(thingId,'truck:*')")
                        .options(o -> o.size(20)
                                .sort(s -> s.asc("thingId"))).fields("thingId"))
                .toList()
                .forEach(foundThing -> {

                    Truck truck = truckMapper.fromThing(foundThing);
                    System.out.println("Found thing: " + foundThing);
                    thingHandler.deleteThing(thingClient, truck.getThingId());
                    System.out.println("Deleted thing " + truck.getThingId());
                });
    }


    public void setDecisionForNextDestination(Truck truck, Warehouse warehouse) throws ExecutionException, InterruptedException {
        double fuel = truckGateway.getFuelFromDitto(truck);
        Map<String, Double> distances = truck.calculateDistances(warehouse);

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

            System.out.println(gasStation.getThingId() + " cost: " + cost);
            if(cost < bestScore){
                bestScore = cost;
                bestTarget = new TruckTargetDecision<>(gasStation, distanceGasStation, gasStation.getLocation(), gasStation.getThingId());
            }
        }
            logger.info("Check for potential travel to {}", warehouse.getThingId());
            Double distanceWarehouse = distances.get(warehouse.getThingId());
            if (distanceWarehouse != null) {

                double utilizationWarehouse = warehouseGateway.getUtilizationFromDitto(warehouse);
                double cost = weightDistance * distanceWarehouse + weightUtilization * utilizationWarehouse;

                System.out.println(warehouse.getThingId() + " cost: " + cost);
                if (cost < bestScore) {
                    bestScore = cost;
                    bestTarget = new TruckTargetDecision<>(warehouse, distanceWarehouse, warehouse.getLocation(), warehouse.getThingId());
                }
            }
        if(bestTarget != null){
            truck.setRecommendedTarget(bestTarget);
            logger.info("Next Target for {} is {}" , truck.getThingId(), bestTarget.getDecidedTarget() instanceof GasStation ? ((GasStation) bestTarget.getDecidedTarget()).getThingId() : ((Warehouse) bestTarget.getDecidedTarget()).getThingId());
        }

    }
}
