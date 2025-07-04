package org.example.SustainableCodeTest.Factory.Things;

import org.eclipse.ditto.client.DittoClient;
import org.example.SustainableCodeTest.Factory.DigitalTwinFactory;
import org.example.ThingHandler;
import org.example.Things.GasStationThing.GasStation;

import java.util.concurrent.ExecutionException;

public class GasStationFactory implements DigitalTwinFactory<GasStation> {

    ThingHandler thingHandler = new ThingHandler();
    DittoClient dittoClient;

    GasStation gasStation;

    public GasStationFactory(DittoClient dittoClient){
        this.dittoClient = dittoClient;

    }
    @Override
    public void createTwinsForDitto() throws ExecutionException, InterruptedException {
        initializeThings();
        thingHandler.createTwinAndPolicy(dittoClient, getWOTURL(), getPolicyURL(), gasStation.getThingId()).toCompletableFuture();

    }

    @Override
    public String getWOTURL() {
        return "https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/GasStation/gasstationmain?cb=" + System.currentTimeMillis();
    }

    @Override
    public String getPolicyURL() {
        return "https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/lkwpolicy";
    }

    @Override
    public void initializeThings() throws InterruptedException {
        gasStation = new GasStation();
        gasStation.setStarterValues();
        gasStation.featureSimulation();
    }

    public GasStation getGasStation() {
        return gasStation;
    }
}
