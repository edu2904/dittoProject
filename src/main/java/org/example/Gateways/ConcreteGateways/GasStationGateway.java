package org.example.Gateways.ConcreteGateways;

import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.example.Gateways.AbstractGateway;
import org.example.Things.GasStationThing.GasStation;

import java.util.concurrent.ExecutionException;

public class GasStationGateway extends AbstractGateway<GasStation> {

    GasStation gasStation;

    public GasStationGateway(DittoClient dittoClient, InfluxDBClient influxDBClient, GasStation gasStation) {
        super(dittoClient, influxDBClient);
        this.gasStation = gasStation;
    }


    @Override
    public void startGateway() throws ExecutionException, InterruptedException {
        startUpdating(gasStation);
    }

    @Override
    public void startUpdating(GasStation thing) throws ExecutionException, InterruptedException {
        updateAttributes(gasStation);
        updateFeatures(gasStation);
    }

    @Override
    public String getWOTURL() {
        return "https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/GasStation/gasstationmain?cb=" + System.currentTimeMillis();
    }

    @Override
    public void updateAttributes(GasStation thing) {
        updateAttributeValue("status", gasStation.getGasStationStatus().toString(), gasStation.getThingId());

    }
    @Override
    public void updateFeatures(GasStation thing) throws ExecutionException, InterruptedException {
        updateFeatureValue("GasStationFuel", "amount", gasStation.getGasStationFuelAmount(), gasStation.getThingId());
    }


    @Override
    public void logToInfluxDB(GasStation thing, String measurementType) {

    }

    @Override
    public void handleEvents(GasStation thing) {

    }

    @Override
    public void handelActions(GasStation thing) {

    }

    @Override
    public void subscribeForEventsAndActions() {

    }
}