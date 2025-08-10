package org.example.Things.WarehouseThing;

import org.example.util.Config;
import org.example.Things.TruckThing.Truck;
import org.example.Things.TruckThing.TruckStatus;
import org.example.util.GeoConst;
import org.example.util.GeoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;

public class Warehouse {


    Map<String, Object> location = new HashMap<>();
    private boolean mainWarehouse;


    private final Logger logger = LoggerFactory.getLogger(Warehouse.class);
    private boolean loadingSuccess;

    private double inventory;
    private double capacity;
    private WarehouseStatus status;
    private int workers;
    private String thingId;
    private ScheduledFuture<?> currentTask;
    Queue<Truck> queue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private double utilization;

    public Warehouse (){

    }
    private final Set<Truck> trucksInWarehouse = ConcurrentHashMap.newKeySet();

    public String getThingId() {
        return thingId;
    }

    public void setThingId(String thingID) {
        this.thingId = thingID;
    }

    public double getInventory() {
        return inventory;
    }

    public void setInventory(double inventory) {
        if (inventory > capacity) {
            this.inventory = capacity;
        } else if (inventory < 0) {
            this.inventory = 0;
        } else {
            this.inventory = inventory;
        }
    }

    public double getCapacity() {
        return capacity;
    }

    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }

    public WarehouseStatus getStatus() {
        return status;
    }

    public void setStatus(WarehouseStatus warehouseStatus) {
        this.status = warehouseStatus;
    }

    public int getWorkers() {
        return workers;
    }

    public void setWorkers(int workers) {
        this.workers = workers;
    }

    public Map<String, Object> getLocation() {
        return location;
    }

    public void setLocation(double lat, double lon) {
        if(this.location == null){
            this.location = new HashMap<>();
        }
        this.location.put(GeoConst.LAT, lat);
        this.location.put(GeoConst.LON, lon);
    }

    public double getUtilization() {
        return utilization;
    }

    public void setUtilization(double utilization) {
        this.utilization = utilization;
    }

    public boolean isMainWareHouse(){
        return mainWarehouse;
    }
    public void setMainWarehouse(boolean mainWarehouse){
        this.mainWarehouse = mainWarehouse;
    }

   /* public void featureSimulationMainWarehouse(){
        scheduler.scheduleAtFixedRate(() -> {
            setUtilization(calculateUtilization());
            setInventory(getInventory() + workers * 10);
        }, 0, Config.STANDARD_TICK_RATE, TimeUnit.SECONDS);
    };

    */

    public void featureSimulation(){
        scheduler.scheduleAtFixedRate(() -> {
        setUtilization(calculateUtilization());
        if(isMainWareHouse()){
            setInventory(getInventory() + workers * 10);
        }else {
            setInventory(getInventory() - workers * 10);
        }
        }, 0, Config.STANDARD_TICK_RATE, TimeUnit.SECONDS);
    }


    public synchronized void startLoading(Truck truck){

        logger.info("Truck {} requested loading", truck.getThingId());
        trucksInWarehouse.add(truck);

        if(status == WarehouseStatus.WAITING){
            logger.info("Start loading process for {}", truck.getThingId());
            setStatus(WarehouseStatus.LOADING);
            startLoadingProcess(truck);
        }else {
            logger.info("Warehouse already loading. {} waiting in queue", truck.getThingId());
            truck.setStatus(TruckStatus.WAITING);
            queue.add(truck);
        }

    }
    public void startLoadingProcess(Truck truck){
        truck.setStatus(TruckStatus.LOADING);
        currentTask = scheduler.scheduleAtFixedRate(() ->
        {
            double currentInventory = truck.getInventory();
            TruckStatus truckStatus = truck.getStatus();
            if(currentInventory != Config.CAPACITY_STANDARD_TRUCK && truckStatus == TruckStatus.LOADING && truck.getInventory() > 0) {
                logger.info("CurrentInventory {} for {}", currentInventory, truck.getThingId());
                double newInventory = Math.min(10,100 + currentInventory);
                truck.setInventory(currentInventory - newInventory);
                setInventory(getInventory() + newInventory);

            }
            else {
                currentTask.cancel(false);
                logger.info("Cancel Task for {}" , truck.getThingId());
                truck.setTarget(null);
                truck.setTaskActive(false);
                trucksInWarehouse.remove(truck);
                if (!queue.isEmpty()) {
                    logger.info("Entering QUEUE");
                    Truck nextTruck = queue.poll();
                    assert nextTruck != null;
                    startLoadingProcess(nextTruck);
                } else {
                    logger.info("WAREHOUSE WAITING AGAIN");
                    setStatus(WarehouseStatus.WAITING);
                }

            }

        }, 0, Config.STANDARD_TICK_RATE, TimeUnit.SECONDS);


    }

    public void setLoadingSuccess(boolean loadingSuccess) {
        this.loadingSuccess = loadingSuccess;
    }

    public boolean loadingSuccess(){
        return loadingSuccess;
    }

    public double calculateUtilization(){
        double weightTrucks = 0.7;
        double weightInventory = 0.3;


        int currentTrucks = trucksInWarehouse.size();
        double truckUtilization = Math.log(currentTrucks + 1) / Math.log(10);


        double currentInventory = getInventory();
        double maxCapacity = getCapacity();
        double inventoryUtilization = currentInventory / maxCapacity;

        double combinedUtilization = weightTrucks * truckUtilization + weightInventory * inventoryUtilization;

        return Math.min(100.0, Math.max(0.0, combinedUtilization * 100.0));
    }
}
