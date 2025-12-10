package org.example.Things.WarehouseThing;

import org.example.Main;
import org.example.Things.Location;
import org.example.Things.TaskThings.Task;
import org.example.Things.TaskThings.TaskType;
import org.example.util.Config;
import org.example.Things.TruckThing.Truck;
import org.example.Things.TruckThing.TruckStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Comparator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class Warehouse {


    Location location;
    private boolean mainWarehouse;


    private final Logger logger = LoggerFactory.getLogger(Warehouse.class);
    private boolean loadingSuccess;
    private final WarehouseInventory warehouseInventory;
    private final WarehouseTruckManager warehouseTruckManager;

    private final WarehouseSimulation warehouseSimulation;



    private double inventory;
    private double capacity;
    private WarehouseStatus status;
    private int workers;
    private String thingId;
    private Queue<Truck> queue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private double utilization;

    private final Set<Truck> trucksInWarehouse = ConcurrentHashMap.newKeySet();

    public String getThingId() {
        return thingId;
    }

    public void setThingId(String thingID) {
        this.thingId = thingID;
    }

    public double getInventory() {
        return warehouseInventory.getInventory();
    }

    public void setInventory(double inventory) {
       warehouseInventory.setInventory(inventory);
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

    public boolean isMainWareHouse(){
        return mainWarehouse;
    }
    public void setMainWarehouse(boolean mainWarehouse){
        this.mainWarehouse = mainWarehouse;
    }

    public Warehouse(double capacity){
        this.warehouseInventory = new WarehouseInventory(capacity);
        this.warehouseTruckManager = new WarehouseTruckManager();
        this.warehouseSimulation = new WarehouseSimulation(warehouseInventory, warehouseTruckManager);
    }
    public void arriveTruck(Truck truck, TaskType taskType, Consumer<Truck> onComplete){
        synchronized (warehouseTruckManager){
            if(!warehouseTruckManager.hasActiveTruck()){
                warehouseSimulation.startLoading(truck, taskType, onComplete);
            }else {
                truck.setStatus(TruckStatus.WAITING);
                warehouseTruckManager.addTruckToQueue(truck);
            }
        }
    }

    public void featureSimulation(){
        scheduler.scheduleAtFixedRate(() -> {
        setUtilization(calculateUtilization());
        fixInventory();
        }, 0, Config.STANDARD_TICK_RATE, TimeUnit.SECONDS);
    }
    public double calculateUtilization(){
        double weightTrucks = 0.7;
        double weightInventory = 0.3;


        double currentTrucks = trucksInWarehouse.size();
        double truckUtilization = currentTrucks / (currentTrucks + 5);


        double currentInventory = getInventory();
        double midCapacity = getCapacity() / 2;

        double distanceToMid = Math.abs(currentInventory - midCapacity);
        double inventoryUtilization = distanceToMid / midCapacity;


        double combinedUtilization = weightTrucks * truckUtilization + weightInventory * inventoryUtilization;

        return Math.min(100.0, Math.max(0.0, combinedUtilization * 100.0));
    }


    public void fixInventory(){
        if(trucksInWarehouse.isEmpty()){
            if(warehouseInventory.getInventory() < 2000){
                warehouseInventory.add(3 * workers);

            } else if(warehouseInventory.getInventory() > 2000){
                warehouseInventory.remove(3 * workers);
            }
        }
    }

}
