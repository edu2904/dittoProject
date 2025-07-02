package org.example.SustainableCodeTest.Gateways;

import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.example.SustainableCodeTest.AbstractGateway;
import org.example.Things.GasStationThing.GasStation;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class GasStationGateway extends AbstractGateway<GasStation> {


    public GasStationGateway(DittoClient dittoClient, InfluxDBClient influxDBClient) {
        super(dittoClient, influxDBClient);
    }


    @Override
    public void startGateway() {

    }

    @Override
    public String getWOTURL() {
        return null;
    }

    @Override
    public void updateAttributes(GasStation thing) {

    }

    @Override
    public void startUpdating(GasStation thing) {

    }

    @Override
    public void logToInfluxDB(GasStation thing) {

    }

    @Override
    public void handleEvents(GasStation thing) {

    }

    @Override
    public void handelActions(GasStation thing) {

    }

    @Override
    public void subscribeForEventsAndActions(List<GasStation> things) {

        for(GasStation gasStation : things){

        }
    }
}