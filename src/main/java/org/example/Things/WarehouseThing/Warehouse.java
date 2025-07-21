package org.example.Things.WarehouseThing;

import org.example.Config;
import org.example.Things.GasStationThing.GasStationStatus;
import org.example.Things.TruckThing.Truck;
import org.example.Things.TruckThing.TruckStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Warehouse {

    private final Logger logger = LoggerFactory.getLogger(Warehouse.class);

    private int inventory;
    private int capacity;
    private WarehouseStatus status;
    private int workers;
    private String thingId;
    private ScheduledFuture<?> currentTask;
    Queue<Truck> queue = new LinkedList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public Warehouse (){

    }

    public String getThingId() {
        return thingId;
    }

    public void setThingId(String thingID) {
        this.thingId = thingID;
    }

    public int getInventory() {
        return inventory;
    }

    public void setInventory(int inventory) {
        this.inventory = inventory;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
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

    public void setStarterValues(String thingId){
        setThingId(thingId);
        int capacity = (int) ((Math.random() * 101) + 400);
        setCapacity(capacity);
        setInventory((int) (capacity - (Math.random() * 101) + 200));
        setWorkers(1);
        setStatus(WarehouseStatus.WAITING);
    }

    public void featureSimulation(){

    }

    public void startLoading(Truck truck){

        logger.info("Truck {} requested loading", truck.getThingId());

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
            int currentInventory = truck.getInventory();
            TruckStatus truckStatus = truck.getStatus();
            if(currentInventory != Config.CAPACITY_STANDARD_TRUCK && truckStatus == TruckStatus.LOADING) {
                logger.info("CurrentInventory {}", currentInventory);
                int newInventory = Math.min(10,100 - currentInventory);
                truck.setInventory(currentInventory + newInventory);
                setInventory(getInventory() - newInventory);

            }
            else {
                currentTask.cancel(false);
                logger.info("Cancel Task for {}" , truck.getThingId());
                if (!queue.isEmpty()) {
                    logger.info("Entering QUEUE");
                    Truck nextTruck = queue.poll();
                    assert nextTruck != null;
                    startLoadingProcess(nextTruck);
                } else {
                    logger.info("GASSTATION WAITING AGAIN");
                    setStatus(WarehouseStatus.WAITING);
                }

            }

        }, 0, Config.STANDARD_TICK_RATE, TimeUnit.SECONDS);


    }
}
