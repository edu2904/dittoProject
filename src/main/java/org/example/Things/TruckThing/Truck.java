package org.example.Things.TruckThing;

import org.eclipse.ditto.client.DittoClient;
import org.example.Config;
import org.example.ThingHandler;
import org.example.Things.GasStationThing.GasStation;
import org.example.Things.TaskThings.Tasks;
import org.example.Things.WarehouseThing.Warehouse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Truck {

    private final Logger logger = LoggerFactory.getLogger(Truck.class);
    private final AtomicBoolean taskActive = new AtomicBoolean(false);
    private GasStation gasStation;
    private String thingId;
    private TruckStatus truckStatus;
    private double weight;
    private double velocity;
    private double tirePressure;
    private double progress;
    private double fuel;
    private int capacity;
    private int inventory;
    private ArrayList<Integer> stops;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ThingHandler thingHandler = new ThingHandler();
    private Queue<String> tasksQueue = new LinkedList<>();
    private Warehouse warehouseMain;





    public String getThingId() {
        return thingId;
    }

    public void setThingId(String thingId) {
        this.thingId = thingId;
    }

    public TruckStatus getStatus() {
        return truckStatus;
    }

    public void setStatus(TruckStatus status) {
        this.truckStatus = status;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getVelocity() {
        return velocity;
    }

    public void setVelocity(double velocity) {
        this.velocity = velocity;
    }

    public double getTirePressure() {
        return tirePressure;
    }

    public void setTirePressure(double tirePressure) {
        this.tirePressure = tirePressure;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public void setFuel(double fuel) {
        this.fuel = fuel;
    }

    public double getFuel() {
        return fuel;
    }
    public int getInventory() {
        return inventory;
    }
    public void setInventory(int inventory) {
        this.inventory = inventory;
    }
    public void setStops(ArrayList<Integer> stops) {
        this.stops = stops;
    }

    public ArrayList<Integer> getStops() {
        return stops;
    }

    public void setGasStation(GasStation gasStation){
        this.gasStation = gasStation;
    }
    public GasStation getGasStation(){
        return gasStation;
    }

    public Warehouse getWarehouseMain() {
        return warehouseMain;
    }

    public void setWarehouseMain(Warehouse warehouseMain) {
        this.warehouseMain = warehouseMain;
    }

    public boolean isTaskActive(){
        return taskActive.get();
    }
    public void setTaskActive(boolean currentTaskActive){
        taskActive.set(currentTaskActive);
    }



    public void setStarterValues(int truckNumber) {
        setThingId("mytest:LKW-" + truckNumber);
        setStatus(TruckStatus.IDLE);
        setWeight(Config.WEIGHT_STANDARD_TRUCK);
        setVelocity(0);
        setTirePressure(Config.TIRE_PRESSURE_MAX_VALUE_STANDARD_TRUCK);
        setProgress(0);
        setFuel(51);
        setCapacity(Config.CAPACITY_STANDARD_TRUCK);
        setInventory(Config.CAPACITY_STANDARD_TRUCK);
    }



    public void checkForNewTasks(DittoClient dittoClient) throws ExecutionException, InterruptedException {
        String[] tasks = {
                "task:refuel_"+ getThingId(),
                "task:tirePressureLow_" + getThingId()
        };

        for(String taskID : tasks){
            if(thingHandler.thingExists(dittoClient, taskID).get() && !tasksQueue.contains(taskID)){
                tasksQueue.add(taskID);
            }
        }
    }


    public void setDestinations(int destinations){
        //setStops(new ArrayList<>(Collections.nCopies(destinations, 0)));
        ArrayList<Integer> listDestinations = new ArrayList<>();
        for(int i = 0; i < destinations; i++){
            listDestinations.add(0);
        }
        setStops(listDestinations);
    }

    public void runSimulation(String truckName, int destinations, double fuelConsumption, double progressMade, DittoClient dittoClient){
        setDestinations(destinations);
        AtomicBoolean truckArrived = new AtomicBoolean(false);
        AtomicInteger currentStopIndex = new AtomicInteger();

        scheduler.scheduleAtFixedRate(() -> {
            double currentFuelTank = getFuel();
            double currentVelocity = getVelocity();
            double currentProgress = getProgress();
            double currentTirePressure = getTirePressure();
            int currentInventory = getInventory();
            TruckStatus currentStatus = getStatus();



            ArrayList<Integer> currentStops = getStops();


            if (truckArrived.get() || currentFuelTank <= 0) {
                setStatus(TruckStatus.IDLE);
                setVelocity(0);
                logger.warn("Drive ended for {}", truckName);
                scheduler.shutdown();
            }else {
                try {
                    checkForNewTasks(dittoClient);




                    if(thingHandler.thingExists(dittoClient, "task:refuel_"+ truckName).get()){
                       if(currentStatus != TruckStatus.REFUELING && !isTaskActive()) {
                           setTaskActive(true);
                           gasStation.startRefuel(this);
                       }
                    } else if(thingHandler.thingExists(dittoClient, "task:tirePressureLow_" + truckName).get()) {
                        if (currentStatus != TruckStatus.ADJUSTINGTIREPRESSURE && !isTaskActive()) {
                            setTaskActive(true);
                            gasStation.startTirePressureAdjustment(this);
                        }
                    }else if(thingHandler.thingExists(dittoClient, "task:loadingTruck_" + truckName).get()){
                        if(currentStatus != TruckStatus.LOADING && !isTaskActive()){
                            setTaskActive(true);
                            warehouseMain.startLoading(this);
                        }
                    }




                    else {

                        setStatus(TruckStatus.DRIVING);
                        //logger.info("{} driving", truckName);
                        logger.debug("current Progress {}: {}", truckName, currentProgress);
                        logger.debug("current FuelTank {}: {}", truckName, currentFuelTank);

                        //setTirePressure(tirePressure);
                        tirePressureDecreases(currentTirePressure);
                        setVelocity(75 + Math.random() * 10);
                        setFuel(currentFuelTank - fuelConsumption);
                        setProgress(currentProgress + progressMade);
                        setInventory(Math.max(0, currentInventory - 20));

                        if(progress == 100 && currentStopIndex.get() < currentStops.size()){
                            stops.set(currentStopIndex.get(), 1);
                            logger.info("{} arrived at destination {}", truckName, currentStopIndex);
                            currentStopIndex.getAndIncrement();
                        }
                        if(currentStopIndex.get() == currentStops.size()){
                            truckArrived.set(true);
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }


        }, 0, Config.STANDARD_TICK_RATE, TimeUnit.SECONDS);


    }


    public void featureSimulation1(DittoClient dittoClient) {
        runSimulation(getThingId(), 5, 0.1, 5, dittoClient);
    }

    public void featureSimulation2(DittoClient dittoClient) {
        runSimulation(getThingId(), 3, 1.0, 10, dittoClient);
    }

    public void tirePressureDecreases(double tirePressure){
        if(Math.random() <= Config.TIRE_PRESSURE_DECREASE_RATE){
            double tirePressureReduction = Math.random() * 100;
            setTirePressure(tirePressure - tirePressureReduction);
        }
    }
}