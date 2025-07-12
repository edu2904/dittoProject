package org.example.SustainableCodeTest.Gateways;

import org.example.SustainableCodeTest.DigitalTwinsGateway;
import org.example.Things.WarehouseThing.Warehouse;

import java.util.concurrent.ExecutionException;

public class WarehouseGateway implements DigitalTwinsGateway<Warehouse> {
    @Override
    public void startGateway() throws ExecutionException, InterruptedException {

    }

    @Override
    public String getWOTURL() {
        return null;
    }

    @Override
    public void updateAttributes(Warehouse thing) {

    }

    @Override
    public void startUpdating(Warehouse thing) throws ExecutionException, InterruptedException {

    }

    @Override
    public void updateFeatures(Warehouse thing) throws ExecutionException, InterruptedException {

    }

    @Override
    public void logToInfluxDB(Warehouse thing, String measurementType) {

    }

    @Override
    public void handleEvents(Warehouse thing) {

    }

    @Override
    public void handelActions(Warehouse thing) {

    }

    @Override
    public void subscribeForEventsAndActions() {

    }
}
