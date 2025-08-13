package org.example.Gateways.ConcreteGateways;

import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.example.Gateways.AbstractGateway;
import org.example.Things.GasStationThing.GasStation;
import org.example.Things.TruckThing.Truck;
import org.example.util.GeoConst;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class GasStationGateway extends AbstractGateway<GasStation> {

    List<GasStation> gasStations;

    public GasStationGateway(DittoClient dittoClient, InfluxDBClient influxDBClient, List<GasStation> gasStations) {
        super(dittoClient, influxDBClient);
        this.gasStations = gasStations;
    }


    @Override
    public void startGateway() throws ExecutionException, InterruptedException {
        for (GasStation gasStation : gasStations) {
            startUpdating(gasStation);
        }
    }

    @Override
    public void startUpdating(GasStation gasStation) throws ExecutionException, InterruptedException {
        updateAttributes(gasStation);
        updateFeatures(gasStation);
    }

    @Override
    public String getWOTURL() {
        return "https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/GasStation/gasstationmain?cb=" + System.currentTimeMillis();
    }

    @Override
    public void updateAttributes(GasStation gasStation) {
        updateAttributeValue("status", gasStation.getGasStationStatus().toString(), gasStation.getThingId());
        updateAttributeValue("location/geo:lat", gasStation.getLocation().getLat(), gasStation.getThingId());
        updateAttributeValue("location/geo:long", gasStation.getLocation().getLon(), gasStation.getThingId());
        updateAttributeValue("utilization", gasStation.getUtilization(), gasStation.getThingId());


    }
    @Override
    public void updateFeatures(GasStation gasStation) throws ExecutionException, InterruptedException {
        updateFeatureValue("GasStationFuel", "amount", gasStation.getGasStationFuelAmount(), gasStation.getThingId());
    }


    @Override
    public void logToInfluxDB(GasStation thing, String measurementType) {

    }

    public double getUtilizationFromDitto(GasStation gasStation) throws ExecutionException, InterruptedException {
        return (double) getAttributeValueFromDitto("utilization", gasStation.getThingId());
    }
}