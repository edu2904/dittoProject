package org.example.Things.WarehouseThing;

import org.example.Things.TaskThings.TaskType;
import org.example.Things.TruckThing.Truck;
import org.example.Things.TruckThing.TruckSimulation;
import org.example.Things.TruckThing.TruckStatus;
import org.example.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;


public class WarehouseSimulation {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseSimulation.class);
    private WarehouseInventory warehouseInventory;
    private WarehouseTruckManager warehouseTruckManager;
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public WarehouseSimulation(WarehouseInventory warehouseInventory, WarehouseTruckManager warehouseTruckManager){
        this.warehouseInventory = warehouseInventory;
        this.warehouseTruckManager = warehouseTruckManager;
    }

    public void startLoading(Truck truck, TaskType taskType,  Consumer<Truck> onComplete){
        setTruckStatus(truck, taskType);
        warehouseTruckManager.setActiveTruck(truck);

        AtomicReference<ScheduledFuture<?>> schedulerRegister = new AtomicReference<>();
        ScheduledFuture<?> currentTask = scheduler.scheduleAtFixedRate(() ->{
            double currentInventory = truck.getInventory();
            TruckStatus truckStatus = truck.getStatus();
            if(truckStatus == TruckStatus.LOADING && currentInventory < truck.getCargoToBeDelivered()) {

                logger.info("CurrentInventory {} for {} with Warehouse inventory left {}", currentInventory, truck.getThingId(), warehouseInventory.getInventory());
                double warehouseStock = warehouseInventory.getInventory();
                double remainingTruckCapacity = truck.getCargoToBeDelivered() - currentInventory;

                double newInventory = Math.min(10, Math.min(remainingTruckCapacity, warehouseStock));
                if(newInventory > 0) {
                    truck.setInventory(currentInventory + newInventory);
                    warehouseInventory.remove(newInventory);
                }else {
                    ScheduledFuture<?> task = schedulerRegister.get();
                    if(task != null) task.cancel(false);
                    finishTruck(truck, onComplete);
                }


            }else if(truckStatus == TruckStatus.UNLOADING && currentInventory > 0){

                logger.info("CurrentInventory {} for {}", currentInventory, truck.getThingId());
                double warehouseStock = warehouseInventory.getInventory();
                double freeSpace = warehouseInventory.getCapacity() - warehouseStock;

                double newInventory = Math.min(10,Math.min(currentInventory, freeSpace));

                if(newInventory > 0) {
                    truck.setInventory(currentInventory - newInventory);
                    warehouseInventory.add(newInventory);
                }else {
                    ScheduledFuture<?> task = schedulerRegister.get();
                    if(task != null) task.cancel(false);
                    finishTruck(truck, onComplete);
                }
            }else {

                ScheduledFuture<?> task = schedulerRegister.get();
                if(task != null) task.cancel(false);
                finishTruck(truck, onComplete);
            }

        }, 0, Config.STANDARD_TICK_RATE, TimeUnit.SECONDS);
        schedulerRegister.set(currentTask);
    }


    public boolean isTruckFinished(Truck truck, TaskType taskType){
        if (taskType == TaskType.LOAD) return truck.getInventory() >= truck.getCargoToBeDelivered();
        else return truck.getInventory() <= 0;
    }

    public void finishTruck(Truck truck, Consumer<Truck> onComplete){
        TruckStatus truckStatus = truck.getStatus();
        logger.info("Cancel Task for {}" , truck.getThingId());



        boolean success;
        if (truckStatus == TruckStatus.LOADING) {
            success = truck.getInventory() >= truck.getCargoToBeDelivered();
        } else {
            success = truck.getInventory() <= 0;
        }

        if(!success){
            resetWarehouseAndTruck(truck);
        }

        truck.setTarget(null);
        truck.setTaskActive(false);
        truck.setTaskSuccess(success);

        warehouseTruckManager.setActiveTruck(null);


        onComplete.accept(truck);

        Truck nextTruck = warehouseTruckManager.getTruckFromQueue();
        if(nextTruck != null){
            startLoading(nextTruck, nextTruck.getTask(), onComplete);
    }
    }
    public void setTruckStatus(Truck truck, TaskType taskType){
        if(taskType == TaskType.LOAD) truck.setStatus(TruckStatus.LOADING);
        if(taskType == TaskType.UNLOAD) truck.setStatus(TruckStatus.UNLOADING);
    }

    public void resetWarehouseAndTruck(Truck truck){
        warehouseInventory.setInventory(1000);
        truck.setInventory(0.0);
    }

}
