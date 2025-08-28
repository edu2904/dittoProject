package org.example.Things.TruckThing;

import org.eclipse.ditto.client.DittoClient;
import org.example.Gateways.GatewayManager;
import org.example.Things.GasStationThing.GasStation;
import org.example.Things.WarehouseThing.Warehouse;
import org.example.util.Config;
import org.example.util.GeoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TruckSimulation {
    Truck truck;
    private final Logger logger = LoggerFactory.getLogger(TruckSimulation.class);

    DittoClient dittoClient;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    TruckEventsActions truckEventsActions;

    public TruckSimulation(DittoClient dittoClient, Truck truck) throws ExecutionException, InterruptedException {
        this.dittoClient = dittoClient;
        this.truck = truck;
        truckEventsActions = new TruckEventsActions(dittoClient);
    }

    public double calculateUtilization(){
        double weightFuel = 1;
        double combinedUtilization = weightFuel * truck.getFuel();

        return Math.min(100.0, Math.max(0.0, combinedUtilization * 100.0));

    }
    public void updateTarget(GatewayManager gatewayManager){
        if(truck.getTargetWarehouse() != null && truck.getTarget() == null){
            try {
                gatewayManager.setDecisionForNextDestination(truck, truck.getTargetWarehouse());
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if(truck.getTarget() == null && truck.getRecommendedTarget() != null){
            truck.setTarget(truck.getRecommendedTarget());
            truck.setProgress(0);
            truck.setRecommendedTarget(null);
        }
    }


    public void runSimulation(DittoClient dittoClient, GatewayManager gatewayManager){


        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("######################");
            System.out.println(truck.getThingId() + " " + truck.getTargetWarehouse());
            System.out.println("######################");
            try {
                updateTarget(gatewayManager);

                if (truck.getFuel() <= 0 || truck.getTarget() == null) {
                    stopTruck();
                    if (truck.getFuel() <= 0) {
                        logger.warn("Truck {} stopped: fuel empty", truck.getThingId());
                    }
                    if (truck.getTarget() == null) {
                        logger.warn("Truck {} stopped: no target assigned", truck.getThingId());
                    }
                }else if(truck.getTarget() != null) {
                    drive(truck.getTarget(), dittoClient);
                }
            } catch (Exception e) {
                logger.error("Error in truck {}: {}" , truck.getThingId(), e);
            }
        }, 0, Config.STANDARD_TICK_RATE, TimeUnit.SECONDS);
    }

    public void drive(TruckTargetDecision<?> target, DittoClient dittoClient){

        if(truck.isTaskActive()) return;
        truck.setStatus(TruckStatus.DRIVING);

        tirePressureDecreases(truck.getTirePressure());
        truck.setVelocity(75 + Math.random() * 10);
        truck.setFuel(truck.getFuel() - truck.getFuelConsumption());
        truck.setUtilization(calculateUtilization());

        double targetDistance = target.getDistance() * 1000;
        double progressPerTick = (truck.getVelocity() * Config.STANDARD_TICK_RATE / targetDistance) * 100;
        truck.setProgress(Math.min(100, truck.getProgress() + progressPerTick));
        double distanceTravelled = (truck.getProgress() / 100) * targetDistance;


        logger.info("{} travelled {}/{} Meters", truck.getThingId(), distanceTravelled, targetDistance);

        if(truck.getProgress() >= 100){ //&& truck.getCurrentStopIndex().get() <= truck.getStops().size()){
            startTask(dittoClient, target);

        }/*
        else if(truck.getCurrentStopIndex().get() > truck.getStops().size()){
            logger.info("Truck {} finished tour", truck.getThingId());
            truck.getCurrentStopIndex().set(1);
            Collections.fill(truck.getStops(), 0);
        }
        */
    }

    public void stopTruck(){
        truck.setStatus(TruckStatus.IDLE);
        truck.setVelocity(0);
        logger.warn("Drive stopped for {}", truck.getThingId());
    }

    public void startTask(DittoClient dittoClient, TruckTargetDecision<?> target){
        if(target.getDecidedTarget() instanceof GasStation){
            if(truck.getStatus() != TruckStatus.REFUELING && !truck.isTaskActive()) {
                truck.setTaskActive(true);
                truck.setLocation(((GasStation) target.getDecidedTarget()).getLocation());
                ((GasStation) target.getDecidedTarget()).startRefuel(truck);
            }
        }
        else if(target.getDecidedTarget() instanceof Warehouse) {
            if (truck.getStatus() != TruckStatus.LOADING && !truck.isTaskActive()) {
                truck.setTaskActive(true);
                truck.setLocation(((Warehouse) target.getDecidedTarget()).getLocation());
                ((Warehouse) target.getDecidedTarget()).startLoading(truck, truck.getTask(), then ->{
                if(truck.isTaskSuccess()){
                    System.out.println("TASK SUCCESS for " + truck.getThingId());
                    truck.setTargetWarehouse(null);
                    truckEventsActions.sendSuccessEvent(truck);
                }else {
                    System.out.println("TASK NOT SUCCESS for " + truck.getThingId());
                    truck.setTargetWarehouse(null);
                    truck.setStatus(TruckStatus.IDLE);
                    truckEventsActions.sendTaskFailEvent(truck);
                }
                });

                //if(!((Warehouse) target.getDecidedTarget()).isMainWareHouse()) {
                //    truck.getStops().set(truck.getCurrentStopIndex().get() - 1, 1);
                //    truck.getCurrentStopIndex().getAndIncrement();
                //}
            }
        }
    }




    public void tirePressureDecreases(double tirePressure){
        if(Math.random() <= Config.TIRE_PRESSURE_DECREASE_RATE){
            double tirePressureReduction = Math.random() * 100;
            truck.setTirePressure(tirePressure - tirePressureReduction);
        }
    }

    public Map<String, Double> calculateDistances(){
        Map<String, Double> distances = new HashMap<>();
        for(Warehouse warehouse : truck.getWarehouseList()){
            double warehouseDistance = GeoUtil.calculateDistance(truck.getLocation(), warehouse.getLocation());
            distances.put(warehouse.getThingId(), warehouseDistance);
        }
        for(GasStation gasStation : truck.getGasStation()){
            double gasStationDistance = GeoUtil.calculateDistance(truck.getLocation(), gasStation.getLocation());
            distances.put(gasStation.getThingId(), gasStationDistance);
        }
        return distances;
    }
    public void setStarterLocation(){
        for(Warehouse warehouse : truck.getWarehouseList()){
            if(warehouse.isMainWareHouse()){
                truck.setLocation(warehouse.getLocation());
                break;
            }
        }
    }
}
