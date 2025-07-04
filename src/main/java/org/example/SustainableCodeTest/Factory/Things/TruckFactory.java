package org.example.SustainableCodeTest.Factory.Things;

import org.eclipse.ditto.client.DittoClient;
import org.example.SustainableCodeTest.Factory.DigitalTwinFactory;
import org.example.ThingHandler;
import org.example.Things.TruckThing.Truck;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class TruckFactory implements DigitalTwinFactory<Truck> {

    DittoClient dittoClient;
    List<Truck> truckList = new ArrayList<>();
    ThingHandler thingHandler = new ThingHandler();

    public TruckFactory(DittoClient dittoClient){
        this.dittoClient = dittoClient;
    }
    @Override
    public void createTwinsForDitto() throws ExecutionException, InterruptedException {
        initializeThings();

        for (int i = 0; i < truckList.size(); i++ ){
            thingHandler.createTwinAndPolicy(dittoClient, getWOTURL(), getPolicyURL(), truckList.get(i).getThingId()).toCompletableFuture();
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
        Truck truck1 = new Truck();
        truck1.setStarterValues(1);
        Truck truck2 = new Truck();
        truck2.setStarterValues(2);

        truckList.add(truck1);
        truckList.add(truck2);

        truck1.featureSimulation1(dittoClient);
        truck2.featureSimulation2(dittoClient);


    }
    public List<Truck> getTruckList(){
        return truckList;
    }


}
