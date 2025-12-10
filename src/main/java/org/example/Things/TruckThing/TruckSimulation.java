package org.example.Things.TruckThing;

import org.eclipse.ditto.client.DittoClient;
import org.example.Gateways.Permanent.GatewayManager;
import org.example.Things.GasStationThing.GasStation;
import org.example.Things.WarehouseThing.Warehouse;
import org.example.util.Config;
import org.example.util.GeoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    TruckEventsActions truckEventsActions = new TruckEventsActions();

    public TruckSimulation(DittoClient dittoClient, Truck truck) throws ExecutionException, InterruptedException {
        this.dittoClient = dittoClient;
        this.truck = truck;
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


    public void runSimulation(GatewayManager gatewayManager){
        scheduler.scheduleAtFixedRate(() -> {
            try {
                updateTarget(gatewayManager);

                if (truck.getFuel() <= 0 || truck.getTarget() == null) {
                    if (truck.getFuel() <= 0) {
                        logger.warn("Truck {} stopped: fuel empty", truck.getThingId());
                        resetTruck();
                    }
                    if (truck.getTarget() == null) {
                        logger.warn("Truck {} stopped: no target assigned", truck.getThingId());
                    }
                    stopTruck();
                }else if(truck.getTarget() != null) {
                    drive(truck.getTarget(), dittoClient);
                }
            } catch (Exception e) {
                logger.error("Error in truck {}: {}" , truck.getThingId(), e);
            }
        }, 0, Config.STANDARD_TICK_RATE, TimeUnit.SECONDS);
    }

    public void drive(TruckTargetDecision<?> target, DittoClient dittoClient){

        if(truck.isTaskActive() || truck.getStatus() == TruckStatus.DISABLED) return;
        truck.setStatus(TruckStatus.DRIVING);
        checkTirePressure();


        tirePressureDecreases(truck.getTirePressure());
        truck.setVelocity(75 + Math.random() * 10);
        truck.setFuel(truck.getFuel() - truck.getFuelConsumption());
        truck.setUtilization(truck.calculateUtilization());

        double targetDistance = target.getDistance() * 1000;
        double progressPerTick = (truck.getVelocity() * Config.STANDARD_TICK_RATE / targetDistance) * 100;
        truck.setProgress(Math.min(100, truck.getProgress() + progressPerTick));
        double distanceTravelled = (truck.getProgress() / 100) * targetDistance;


        logger.info("{} travelled {}/{} Meters", truck.getThingId(), String.format("%.0f", distanceTravelled), String.format("%.0f", targetDistance));

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
        if(!truck.getStatus().equals(TruckStatus.DISABLED) || truck.getStatus().equals(TruckStatus.WAITING)) {
            truck.setStatus(TruckStatus.IDLE);
            truck.setVelocity(0);
            recoverTirePressure();
            logger.warn("Drive stopped for {}", truck.getThingId());
        }
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
                ((Warehouse) target.getDecidedTarget()).arriveTruck(truck, truck.getTask(), then ->{
                if(then.isTaskSuccess()){
                    System.out.println("TASK SUCCESS for " + then.getThingId());
                    then.setTargetWarehouse(null);
                    truckEventsActions.sendSuccessEvent(dittoClient, then);
                }else {
                    System.out.println("TASK NOT SUCCESS for " + then.getThingId());
                    then.setTargetWarehouse(null);
                    then.setStatus(TruckStatus.IDLE);
                    truckEventsActions.sendTaskFailEvent(dittoClient, then);
                }
                });
            }
        }
    }
    public void tirePressureDecreases(double tirePressure){
        //if(Math.random() <= Config.TIRE_PRESSURE_DECREASE_RATE){
            double tirePressureReduction = 3;//Math.random() * 100;
            truck.setTirePressure(tirePressure - tirePressureReduction);
        //}
    }

    public void recoverTirePressure(){
        if(truck.getTirePressure() >= Config.TIRE_PRESSURE_MAX_VALUE_STANDARD_TRUCK){
            return;
        }
        double recoveryRate = Config.TIRE_PRESSURE_RECOVERY_RATE;
        double difference = Config.TIRE_PRESSURE_MAX_VALUE_STANDARD_TRUCK - truck.getTirePressure();
        double increase = difference * recoveryRate;

        double newPressure = truck.getTirePressure() + 5;

        if(newPressure > Config.TIRE_PRESSURE_MAX_VALUE_STANDARD_TRUCK){
            newPressure = Config.TIRE_PRESSURE_MAX_VALUE_STANDARD_TRUCK;
        }

        truck.setTirePressure(newPressure);
        logger.debug("TIRE PRESSURE INCREASED FOR {}", truck.getThingId());

    }

    public void checkTirePressure(){
        if(truck.getTirePressure() <= Config.TIRE_PRESSURE_MIN_VALUE_STANDARD_TRUCK){
            logger.info("TIRE PRESSURE LOW FOR TRUCK {}" , truck.getThingId());
            truck.setStatus(TruckStatus.DISABLED);
            truck.setVelocity(0);
            truck.setTaskActive(false);
            truck.setRecommendedTarget(null);
            truck.setTargetWarehouse(null);
            truck.setTarget(null);
            truckEventsActions.sendTirePressureTooLowEvent(dittoClient, truck);
            scheduler.schedule(() -> {
                truck.setTirePressure(9000);
                truck.setStatus(TruckStatus.IDLE);
                logger.info("TRUCK {} ENABLED AGAIN AFTER LOW TIRE PRESSURE", truck.getThingId());

            }, 3, TimeUnit.MINUTES);
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
    public void resetTruck(){
        if(truck.getStatus() == TruckStatus.DISABLED){
            return;
        }
        truck.setStatus(TruckStatus.DISABLED);
        truckEventsActions.sendFuelTooLow(dittoClient, truck);
        truck.setLocation(truck.getWarehouseList().get(0).getLocation());
        truck.setVelocity(0);
        truck.setTaskActive(false);
        truck.setTarget(null);
        truck.setRecommendedTarget(null);
        truck.setTargetWarehouse(null);
        scheduler.schedule(() -> {
            truck.setFuel(300);
            truck.setStatus(TruckStatus.IDLE);
            logger.info("TRUCK {} ENABLED AGAIN AFTER LOW FUEL", truck.getThingId());
        }, 3 , TimeUnit.MINUTES);
    }
}
