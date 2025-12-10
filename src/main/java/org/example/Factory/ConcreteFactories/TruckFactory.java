package org.example.Factory.ConcreteFactories;

import org.eclipse.ditto.client.DittoClient;
import org.example.Factory.DigitalTwinFactory;
import org.example.Things.Location;
import org.example.Things.TruckThing.TruckStatus;
import org.example.util.Config;
import org.example.util.ThingHandler;
import org.example.Things.TruckThing.Truck;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
// Creates the Trucks that will send data to Ditto and be present in the scenario.
// They can be added, removed, and altered as long as they correspond to its WoT
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
        for (Truck truck : truckList) {
            if(!thingHandler.thingExists(dittoClient, truck.getThingId()).get()) {
                thingHandler.createTwinAndPolicy(dittoClient, getWOTURL(), getPolicyURL(), truck.getThingId()).toCompletableFuture();
            }
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
        //******************************************
        //******************************************
        // USE CASE 1
        //******************************************
        //******************************************
        /*
        truckList.add(createSmallTruck(1));
        truckList.add(createSmallTruck(2));
        truckList.add(createSmallTruck(3));
        truckList.add(createSmallTruck(4));
        truckList.add(createSmallTruck(5));
        truckList.add(createDefaultTruck(1));
        truckList.add(createDefaultTruck(2));
        truckList.add(createDefaultTruck(3));
        truckList.add(createDefaultTruck(4));
        truckList.add(createDefaultTruck(5));
        truckList.add(createLargeTruck(1));
        truckList.add(createLargeTruck(2));
        truckList.add(createLargeTruck(3));
        truckList.add(createLargeTruck(4));
        truckList.add(createLargeTruck(5));

         */


        //******************************************
        //******************************************
        // USE CASE 2
        //******************************************
        //******************************************
        /*
        truckList.add(createSmallTruck(1));
        truckList.add(createSmallTruck(2));
        truckList.add(createSmallTruck(3));
        truckList.add(createSmallTruck(4));
        truckList.add(createSmallTruck(5));
        truckList.add(createSmallTruck(6));
        truckList.add(createSmallTruck(7));
        truckList.add(createSmallTruck(8));
        truckList.add(createSmallTruck(9));
        truckList.add(createSmallTruck(10));
        truckList.add(createDefaultTruck(1));
        truckList.add(createDefaultTruck(2));
        truckList.add(createDefaultTruck(3));
        truckList.add(createDefaultTruck(4));
        truckList.add(createDefaultTruck(5));
        truckList.add(createDefaultTruck(6));
        truckList.add(createDefaultTruck(7));
        truckList.add(createDefaultTruck(8));
        truckList.add(createDefaultTruck(9));
        truckList.add(createDefaultTruck(10));
        truckList.add(createLargeTruck(1));
        truckList.add(createLargeTruck(2));
        truckList.add(createLargeTruck(3));
        truckList.add(createLargeTruck(4));
        truckList.add(createLargeTruck(5));
        truckList.add(createLargeTruck(1));
        truckList.add(createLargeTruck(2));
        truckList.add(createLargeTruck(3));
        truckList.add(createLargeTruck(4));
        truckList.add(createLargeTruck(5));
        truckList.add(createLargeTruck(6));
        truckList.add(createLargeTruck(7));
        truckList.add(createLargeTruck(8));
        truckList.add(createLargeTruck(9));
        truckList.add(createLargeTruck(10));
        */

        //******************************************
        //******************************************
        // USE CASE 3
        //******************************************
        //******************************************
        /*
        truckList.add(createSmallTruck(1));
        truckList.add(createSmallTruck(2));
        truckList.add(createSmallTruckLF(1));
        truckList.add(createSmallTruckLF(2));
        truckList.add(createSmallTruckLF(3));
        truckList.add(createDefaultTruck(1));
        truckList.add(createDefaultTruck(2));
        truckList.add(createDefaultTruckLF(1));
        truckList.add(createDefaultTruckLF(2));
        truckList.add(createDefaultTruckLF(3));
        truckList.add(createLargeTruck(1));
        truckList.add(createLargeTruck(2));
        truckList.add(createLargeTruckLF(1));
        truckList.add(createLargeTruckLF(2));
        truckList.add(createLargeTruckLF(3));
         */


        //******************************************
        //******************************************
        // USE CASE 4
        //******************************************
        //******************************************
        /*
        truckList.add(createSmallTruck(1));
        truckList.add(createSmallTruck(2));
        truckList.add(createSmallTruck(3));
        truckList.add(createSmallTruck(4));
        truckList.add(createSmallTruck(5));
        truckList.add(createSmallTruckLF(1));
        truckList.add(createSmallTruckLF(2));
        truckList.add(createSmallTruckLF(3));
        truckList.add(createSmallTruckLF(4));
        truckList.add(createSmallTruckLF(5));
        truckList.add(createDefaultTruck(1));
        truckList.add(createDefaultTruck(2));
        truckList.add(createDefaultTruck(3));
        truckList.add(createDefaultTruck(4));
        truckList.add(createDefaultTruck(5));
        truckList.add(createDefaultTruckLF(1));
        truckList.add(createDefaultTruckLF(2));
        truckList.add(createDefaultTruckLF(3));
        truckList.add(createDefaultTruckLF(4));
        truckList.add(createDefaultTruckLF(5));
        truckList.add(createLargeTruck(1));
        truckList.add(createLargeTruck(2));
        truckList.add(createLargeTruck(3));
        truckList.add(createLargeTruck(4));
        truckList.add(createLargeTruck(5));
        truckList.add(createLargeTruckLF(1));
        truckList.add(createLargeTruckLF(2));
        truckList.add(createLargeTruckLF(3));
        truckList.add(createLargeTruckLF(4));
        truckList.add(createLargeTruckLF(5));
        */



        for(Truck truck : truckList) {

            truck.setUtilization(truck.calculateUtilization());
        }
    }

    @Override
    public List<Truck> getThings() {
        return truckList;
    }

    public Truck createDefaultTruck(int number) {
        Truck truck = new Truck();
        truck.setThingId("truck:Truck-medium-" + number);
        truck.setStatus(TruckStatus.IDLE);
        truck.setWeight(Config.WEIGHT_SMALL_TRUCK);
        truck.setVelocity(0);
        truck.setTirePressure(Config.TIRE_PRESSURE_MAX_VALUE_STANDARD_TRUCK);
        truck.setProgress(0);
        truck.setFuel(300);
        truck.setCapacity(Config.CAPACITY_STANDARD_TRUCK);
        truck.setInventory(0);
        truck.setLocation(new Location(48.0842, 11.5302));
        truck.setFuelConsumption(1.5);
        return truck;
    }
    public Truck createDefaultTruckLF(int number) {
        Truck truck = new Truck();
        truck.setThingId("truck:Truck-mediumLF-" + number);
        truck.setStatus(TruckStatus.IDLE);
        truck.setWeight(Config.WEIGHT_SMALL_TRUCK);
        truck.setVelocity(0);
        truck.setTirePressure(Config.TIRE_PRESSURE_MAX_VALUE_STANDARD_TRUCK);
        truck.setProgress(0);
        truck.setFuel(100);
        truck.setCapacity(Config.CAPACITY_STANDARD_TRUCK);
        truck.setInventory(0);
        truck.setLocation(new Location(48.0842, 11.5302));
        truck.setFuelConsumption(1.5);
        return truck;
    }
    public Truck createSmallTruck(int number) {
        Truck truck = new Truck();
        truck.setThingId("truck:Truck-small-" + number);
        truck.setStatus(TruckStatus.IDLE);
        truck.setWeight(Config.WEIGHT_STANDARD_TRUCK);
        truck.setVelocity(0);
        truck.setTirePressure(Config.TIRE_PRESSURE_MAX_VALUE_STANDARD_TRUCK);
        truck.setProgress(0);
        truck.setFuel(300);
        truck.setCapacity(Config.CAPACITY_SMALL_TRUCK);
        truck.setInventory(0);
        truck.setLocation(new Location(48.0842, 11.5302));
        truck.setFuelConsumption(1.0);
        return truck;
    }
    public Truck createSmallTruckLF(int number) {
        Truck truck = new Truck();
        truck.setThingId("truck:Truck-smallLF-" + number);
        truck.setStatus(TruckStatus.IDLE);
        truck.setWeight(Config.WEIGHT_STANDARD_TRUCK);
        truck.setVelocity(0);
        truck.setTirePressure(Config.TIRE_PRESSURE_MAX_VALUE_STANDARD_TRUCK);
        truck.setProgress(0);
        truck.setFuel(100);
        truck.setCapacity(Config.CAPACITY_SMALL_TRUCK);
        truck.setInventory(0);
        truck.setLocation(new Location(48.0842, 11.5302));
        truck.setFuelConsumption(1.0);
        return truck;
    }
    public Truck createLargeTruck(int number) {
        Truck truck = new Truck();
        truck.setThingId("truck:Truck-large-" + number);
        truck.setStatus(TruckStatus.IDLE);
        truck.setWeight(Config.WEIGHT_LARGE_TRUCK);
        truck.setVelocity(0);
        truck.setTirePressure(Config.TIRE_PRESSURE_MAX_VALUE_STANDARD_TRUCK);
        truck.setProgress(0);
        truck.setFuel(300);
        truck.setCapacity(Config.CAPACITY_LARGE_TRUCK);
        truck.setInventory(0);
        truck.setLocation(new Location(48.0842, 11.5302));
        truck.setFuelConsumption(2);
        return truck;
    }
    public Truck createLargeTruckLF(int number) {
        Truck truck = new Truck();
        truck.setThingId("truck:Truck-largeLF-" + number);
        truck.setStatus(TruckStatus.IDLE);
        truck.setWeight(Config.WEIGHT_LARGE_TRUCK);
        truck.setVelocity(0);
        truck.setTirePressure(Config.TIRE_PRESSURE_MAX_VALUE_STANDARD_TRUCK);
        truck.setProgress(0);
        truck.setFuel(100);
        truck.setCapacity(Config.CAPACITY_LARGE_TRUCK);
        truck.setInventory(0);
        truck.setLocation(new Location(48.0842, 11.5302));
        truck.setFuelConsumption(2);
        return truck;
    }
}
