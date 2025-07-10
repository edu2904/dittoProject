package org.example.Things.GasStationThing;

import org.example.Things.TruckThing.Truck;
import org.example.Things.TruckThing.TruckStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class GasStation {
    private final Logger logger = LoggerFactory.getLogger(GasStation.class);

    Queue<Truck> queue = new LinkedList<>();

    private String thingId;
    private GasStationStatus gasStationStatus;
    private double gasStationFuelAmount;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private ScheduledFuture<?> currentTask;

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


    public void setStarterValues(){
        setThingId("mything:GasStation-1");
        setGasStationStatus(GasStationStatus.WAITING);
        setGasStationFuelAmount(3000);
    }

    public  void featureSimulation(){
        scheduler.scheduleAtFixedRate(() -> {
            double currentFuelAmount = getGasStationFuelAmount();
            GasStationStatus currentGasStationStatus = getGasStationStatus();
        }, 0, 3, TimeUnit.SECONDS);
    }

    public void startRefuel(Truck truck){

        logger.info("Truck {} requested refuel", truck.getThingId());

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
                logger.info("CurrentFuel {}", currentFuel);
                double newFuel = Math.min(50,300 - currentFuel);
                truck.setFuel(currentFuel + newFuel);
                setGasStationFuelAmount(getGasStationFuelAmount() - newFuel);

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

}
