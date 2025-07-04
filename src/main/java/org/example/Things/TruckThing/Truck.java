package org.example.Things.TruckThing;

import org.eclipse.ditto.client.DittoClient;
import org.example.ThingHandler;
import org.example.Things.GasStationThing.GasStation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Truck {

    private final Logger logger = LoggerFactory.getLogger(Truck.class);




    private final AtomicBoolean fuelTaskActive = new AtomicBoolean(false);
    private GasStation gasStation;
    private String thingId;
    private TruckStatus truckStatus;
    private double weight;
    private double velocity;
    private double tirePressure;
    private double progress;
    private double fuel;
    private ArrayList<Integer> stops;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final ThingHandler thingHandler = new ThingHandler();



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
    public boolean isFuelTaskActive(){
        return fuelTaskActive.get();
    }
    public void setFuelTaskActive(boolean taskActive){
        fuelTaskActive.set(taskActive);
    }

    public void setStarterValues(int truckNumber) {
        setThingId("mytest:LKW-" + truckNumber);
        setStatus(TruckStatus.IDLE);
        setWeight(7500);
        setVelocity(0);
        setTirePressure(9000);
        setProgress(0);
        setFuel(51);
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
            TruckStatus currentStatus = getStatus();

            ArrayList<Integer> currentStops = getStops();


            if (truckArrived.get() || currentFuelTank <= 0) {
                setStatus(TruckStatus.IDLE);
                setVelocity(0);
                logger.warn("Drive ended for {}", truckName);
                scheduler.shutdown();
            }else {
                try {
                    if(thingHandler.thingExists(dittoClient, "task:refuel_"+ truckName).get()){
                       if(currentStatus != TruckStatus.REFUELING) {

                           //logger.info("Refuel Task registered for {} ", truckName);


                           gasStation.startRefuel(this);
                           //setStatus(TruckStatus.REFUELING);
                       }
                    } else {

                        setStatus(TruckStatus.DRIVING);
                        //logger.info("{} driving", truckName);
                        logger.debug("current Progress {}: {}", truckName, currentProgress);
                        logger.debug("current FuelTank {}: {}", truckName, currentFuelTank);

                        //setTirePressure(tirePressure);
                        tirePressureDecreases(currentTirePressure);
                        setVelocity(75 + Math.random() * 10);
                        setFuel(currentFuelTank - fuelConsumption);
                        setProgress(currentProgress + progressMade);

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


        }, 0, 3, TimeUnit.SECONDS);


    }


    public void featureSimulation1(DittoClient dittoClient) {
        runSimulation(getThingId(), 5, 0.1, 5, dittoClient);
    }

    public void featureSimulation2(DittoClient dittoClient) {
        runSimulation(getThingId(), 3, 1.0, 10, dittoClient);
    }

    public void tirePressureDecreases(double tirePressure){
        if(Math.random() <= 0.1){
            double tirePressureReduction = Math.random() * 100;
            setTirePressure(tirePressure - tirePressureReduction);
        }
    }
}