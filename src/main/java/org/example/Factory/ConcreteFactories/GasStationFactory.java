package org.example.Factory.ConcreteFactories;

import org.eclipse.ditto.client.DittoClient;
import org.example.Factory.DigitalTwinFactory;
import org.example.Things.GasStationThing.GasStationStatus;
import org.example.util.ThingHandler;
import org.example.Things.GasStationThing.GasStation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class GasStationFactory implements DigitalTwinFactory<GasStation> {

    ThingHandler thingHandler;
    DittoClient dittoClient;

    List<GasStation> gasStationsList = new ArrayList<>();

    public GasStationFactory(DittoClient dittoClient, ThingHandler thingHandler){
        this.thingHandler = thingHandler;
        this.dittoClient = dittoClient;

    }
    @Override
    public void createTwinsForDitto() throws ExecutionException, InterruptedException {
        initializeThings();
        for(GasStation gasStation : gasStationsList) {
            thingHandler.createTwinAndPolicy(dittoClient, getWOTURL(), getPolicyURL(), gasStation.getThingId()).toCompletableFuture();
        }
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
    public void initializeThings(){
        GasStation gasStation1 = createDefaultGasStation(1, 48.1107, 11.5394);
        GasStation gasStation2 = createDefaultGasStation(2, 48.1652, 11.5700);
        GasStation gasStation3 = createDefaultGasStation(3, 48.2178, 11.4200);

        gasStationsList.add(gasStation1);
        gasStationsList.add(gasStation2);
        gasStationsList.add(gasStation3);

    }

    public GasStation createDefaultGasStation(int number, double lat, double lon){
        GasStation gasStation = new GasStation();
        gasStation.setThingId("mything:GasStation-" + number);
        gasStation.setGasStationStatus(GasStationStatus.WAITING);
        gasStation.setGasStationFuelAmount(3000);
        gasStation.setLocation(lat, lon);
        return gasStation;
    }

    @Override
    public List<GasStation> getThings() {
        return gasStationsList;
    }

}
