package org.example.Things.GasStationThing;

import org.example.Things.Location;
import org.example.Things.TruckThing.Truck;
import org.example.Things.TruckThing.TruckStatus;
import org.example.util.Config;
import org.example.util.GeoConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class GasStation {

    Location location;

    private final Logger logger = LoggerFactory.getLogger(GasStation.class);

    Queue<Truck> queue = new LinkedList<>();

    public Set<Truck> trucksInGasStation = new HashSet<>();
    private String thingId;
    private GasStationStatus gasStationStatus;
    private double gasStationFuelAmount;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private ScheduledFuture<?> currentTask;
    private double utilization;

    public GasStation(){

    }
    public String getThingId() {
        return thingId;
    }

    public void setThingId(String thingId) {
        this.thingId = thingId;
    }

    public GasStationStatus getGasStationStatus() {
        return gasStationStatus;
    }

    public void setGasStationStatus(GasStationStatus gasStationStatus) {
        this.gasStationStatus = gasStationStatus;
    }

    public double getGasStationFuelAmount() {
        return gasStationFuelAmount;
    }

    public void setGasStationFuelAmount(double gasStationFuelAmount) {
        this.gasStationFuelAmount = gasStationFuelAmount;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(double lat, double lon) {
        this.location = new Location(lat, lon);
    }

    public double getUtilization() {
        return utilization;
    }

    public void setUtilization(double utilization) {
        this.utilization = utilization;
    }

    public  void featureSimulation(){
        scheduler.scheduleAtFixedRate(() -> {
            fixInventory();
            setUtilization(calculateUtilization());
            setGasStationFuelAmount(getGasStationFuelAmount());
        }, 0, 3, TimeUnit.SECONDS);
    }

    public synchronized void startRefuel(Truck truck){

        logger.info("Truck {} requested refuel", truck.getThingId());

        trucksInGasStation.add(truck);

        if(gasStationStatus == GasStationStatus.WAITING){
            logger.info("Start refuel process for {}", truck.getThingId());
            setGasStationStatus(GasStationStatus.REFUELING);
            startRefuelProcess(truck);
        }else {
            logger.info("Gas Station already refueling. {} waiting in queue", truck.getThingId());
            truck.setStatus(TruckStatus.WAITING);
            queue.add(truck);
        }

    }
    public void startRefuelProcess(Truck truck){
        truck.setStatus(TruckStatus.REFUELING);
        currentTask = scheduler.scheduleAtFixedRate(() ->
        {
            double currentFuel = truck.getFuel();
            TruckStatus truckStatus = truck.getStatus();
            if(currentFuel != 300 && truckStatus == TruckStatus.REFUELING) {
                logger.info("CurrentFuel {} for {}", currentFuel, truck.getThingId());
                double newFuel = Math.min(50,300 - currentFuel);
                truck.setFuel(currentFuel + newFuel);
                setGasStationFuelAmount(getGasStationFuelAmount() - newFuel);

            }
            else {
                currentTask.cancel(false);
                logger.info("Cancel Task for {}" , truck.getThingId());
                truck.setTarget(null);
                truck.setTaskActive(false);
                trucksInGasStation.remove(truck);
                if (!queue.isEmpty()) {
                    logger.info("Entering QUEUE");
                    Truck nextTruck = queue.poll();
                    assert nextTruck != null;
                    startRefuelProcess(nextTruck);
                } else {
                    logger.info("GASSTATION WAITING AGAIN");
                    setGasStationStatus(GasStationStatus.WAITING);
                }

            }

        }, 0, Config.STANDARD_TICK_RATE, TimeUnit.SECONDS);


    }

    public void startTirePressureAdjustment(Truck truck){

        logger.info("Truck {} requested tire pressure adjustment", truck.getThingId());

        if(gasStationStatus == GasStationStatus.WAITING){
            logger.info("Start tire pressure adjustment process for {}", truck.getThingId());
            setGasStationStatus(GasStationStatus.ADJUSTINGTIREPRESSURE);
            startTireAdjustmentProcess(truck);
        }else {
            logger.info("Gas Station already tire pressure adjustment process. {} waiting in queue", truck.getThingId());
            truck.setStatus(TruckStatus.WAITING);
            queue.add(truck);
        }

    }

    public void startTireAdjustmentProcess(Truck truck){
        truck.setStatus(TruckStatus.ADJUSTINGTIREPRESSURE);
        currentTask = scheduler.scheduleAtFixedRate(() ->
        {
            double currentTirePressure = truck.getTirePressure();
            TruckStatus truckStatus = truck.getStatus();
            if(currentTirePressure != 300 && truckStatus == TruckStatus.ADJUSTINGTIREPRESSURE) {
                logger.info("Current Tire Pressure {}", currentTirePressure);
                double newTirePressure = Math.min(100,9000 - currentTirePressure);
                truck.setTirePressure(currentTirePressure + newTirePressure);


            }
            else {
                currentTask.cancel(false);
                logger.info("Cancel Task for {}" , truck.getThingId());
                if (!queue.isEmpty()) {
                    logger.info("Entering QUEUE");
                    Truck nextTruck = queue.poll();
                    assert nextTruck != null;
                    startRefuelProcess(nextTruck);
                } else {
                    logger.info("GASSTATION WAITING AGAIN");
                    setGasStationStatus(GasStationStatus.WAITING);
                }

            }

        }, 0, 3, TimeUnit.SECONDS);


    }

    public double calculateUtilization(){
        double weightTrucks = 0.7;
        double weightFuel = 0.3;


        double currentTrucks = trucksInGasStation.size();
        double truckUtilization = currentTrucks / (currentTrucks + 5);


        double currentFuel = getGasStationFuelAmount();
        double maxCapacity = 5000;
        double inventoryUtilization = 1.0 - currentFuel / maxCapacity;

        double combinedUtilization = weightTrucks * truckUtilization + weightFuel * inventoryUtilization;

        return Math.min(100.0, Math.max(0.0, combinedUtilization * 100.0));
    }
    public void fixInventory(){
        if(trucksInGasStation.isEmpty()){
            if(gasStationFuelAmount < 300){
                setGasStationFuelAmount(gasStationFuelAmount);

            }
        }
    }

}
