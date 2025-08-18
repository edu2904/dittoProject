package org.example.Factory.ConcreteFactories;

import org.eclipse.ditto.client.DittoClient;
import org.example.Factory.DigitalTwinFactory;
import org.example.Things.TruckThing.TruckStatus;
import org.example.util.Config;
import org.example.util.ThingHandler;
import org.example.Things.TruckThing.Truck;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class TruckFactory implements DigitalTwinFactory<Truck> {

    DittoClient dittoClient;
    List<Truck> truckList = new ArrayList<>();
    ThingHandler thingHandler;

    public TruckFactory(DittoClient dittoClient, ThingHandler thingHandler){
        this.dittoClient = dittoClient;
        this.thingHandler = thingHandler;
    }
    @Override
    public void createTwinsForDitto() throws ExecutionException, InterruptedException {
        initializeThings();

        for (Truck truck : truckList) {
            thingHandler.createTwinAndPolicy(dittoClient, getWOTURL(), getPolicyURL(), truck.getThingId()).toCompletableFuture();
        }
    }
    @Override
    public String getWOTURL() {
        return "https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/LKW/lkwMain?cb=" + System.currentTimeMillis();

    }

    @Override
    public String getPolicyURL() {
        return "https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/lkwpolicy";
    }

    @Override
    public void initializeThings() {
        Truck truck1 = createDefaultTruck(1);

        //Truck truck2 = new Truck();
        //truck2.setStarterValues(2);

        truckList.add(truck1);
        //truckList.add(truck2);

        //truck1.featureSimulation1(dittoClient);
        //truck2.featureSimulation2(dittoClient);


    }

    @Override
    public List<Truck> getThings() {
        return truckList;
    }

    private Truck createDefaultTruck(int number) {
        Truck truck = new Truck();
        truck.setThingId("truck:Truck-" + number);
        truck.setStatus(TruckStatus.IDLE);
        truck.setWeight(Config.WEIGHT_STANDARD_TRUCK);
        truck.setVelocity(0);
        truck.setTirePressure(Config.TIRE_PRESSURE_MAX_VALUE_STANDARD_TRUCK);
        truck.setProgress(0);
        truck.setFuel(51);
        truck.setCapacity(Config.CAPACITY_STANDARD_TRUCK);
        truck.setInventory(Config.CAPACITY_STANDARD_TRUCK);
        return truck;
    }




}
