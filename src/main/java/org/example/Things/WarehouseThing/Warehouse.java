package org.example.Things.WarehouseThing;

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


    public void featureSimulation(){
        scheduler.scheduleAtFixedRate(() -> {
        setUtilization(calculateUtilization());
        setInventory(200);
        }, 0, Config.STANDARD_TICK_RATE, TimeUnit.SECONDS);
    }




    public synchronized void startLoading(Truck truck, TaskType taskType, Consumer<Truck> onComplete){

        logger.info("Truck {} requested loading", truck.getThingId());
        trucksInWarehouse.add(truck);

        if(status == WarehouseStatus.WAITING){
            logger.info("Start loading process for {}", truck.getThingId());
            setStatus(WarehouseStatus.LOADING);
            startLoadingProcess(truck,taskType, onComplete);
        }else {
            logger.info("Warehouse already loading. {} waiting in queue", truck.getThingId());
            truck.setStatus(TruckStatus.WAITING);
            queue.add(truck);
        }

    }
    public synchronized void startLoadingProcess(Truck truck, TaskType taskType, Consumer<Truck> onComplete){



        final Truck currentTruck = truck;
        double cargoToBeDelivered = currentTruck.getCargoToBeDelivered();

        setTruckStatus(currentTruck, taskType);

        AtomicReference<ScheduledFuture<?>> taskRef = new AtomicReference<>();

        ScheduledFuture<?> currentTask = scheduler.scheduleAtFixedRate(() -> {
            double currentInventory = currentTruck.getInventory();
            TruckStatus truckStatus = currentTruck.getStatus();
            if(truckStatus == TruckStatus.LOADING && currentInventory < cargoToBeDelivered) {

                logger.info("CurrentInventory {} for {}", currentInventory, currentTruck.getThingId());
                double newInventory = Math.min(10,cargoToBeDelivered - currentInventory);
                currentTruck.setInventory(currentInventory + newInventory);
                setInventory(getInventory() - newInventory);


            }else if(truckStatus == TruckStatus.UNLOADING && currentInventory > 0){

                logger.info("CurrentInventory {} for {}", currentInventory, currentTruck.getThingId());
                double newInventory = Math.min(10,currentInventory);
                currentTruck.setInventory(currentInventory - newInventory);
                setInventory(getInventory() + newInventory);

            }
            else {


                    ScheduledFuture<?> selfTask = taskRef.get();
                    if(selfTask != null){
                        selfTask.cancel(true);
                    }

                logger.info("Cancel Task for {}" , currentTruck.getThingId());



                boolean success;
                if (truckStatus == TruckStatus.LOADING) {
                    success = currentTruck.getInventory() >= cargoToBeDelivered;
                } else {
                    success = currentTruck.getInventory() <= 0;
                }



                currentTruck.setTarget(null);
                currentTruck.setTaskActive(false);
                currentTruck.setTaskSuccess(success);
                trucksInWarehouse.remove(currentTruck);

                onComplete.accept(currentTruck);


                synchronized (this) {
                    if (!queue.isEmpty()) {
                        logger.info("Entering QUEUE");
                        Truck nextTruck = queue.poll();
                        assert nextTruck != null;
                        scheduler.schedule(() -> startLoadingProcess(nextTruck, nextTruck.getTask(), onComplete), 0, TimeUnit.MILLISECONDS);
                    } else {
                        logger.info("WAREHOUSE WAITING AGAIN");
                        setStatus(WarehouseStatus.WAITING);
                    }
                }
            }

        }, 0, Config.STANDARD_TICK_RATE, TimeUnit.SECONDS);

        taskRef.set(currentTask);
    }

    public void setTruckStatus(Truck truck, TaskType taskType){
        if(taskType == TaskType.LOAD) truck.setStatus(TruckStatus.LOADING);
        if(taskType == TaskType.UNLOAD) truck.setStatus(TruckStatus.UNLOADING);
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
    /*
    public void arriveTruck(Truck truck, TaskType taskType, Consumer<Truck> onComplete){
        synchronized (warehouseTruckManager){
            if(!warehouseTruckManager.isTruckInQueue()){
                warehouseSimulation.startLoading(truck, taskType, onComplete);
            }else {
                truck.setStatus(TruckStatus.WAITING);
                warehouseTruckManager.addTruckToQueue(truck);
            }
        }
    }

     */

}
