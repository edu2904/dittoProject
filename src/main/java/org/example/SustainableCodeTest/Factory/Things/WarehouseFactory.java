package org.example.SustainableCodeTest.Factory.Things;

import org.example.SustainableCodeTest.Factory.DigitalTwinFactory;
import org.example.Things.WarehouseThing.Warehouse;

import java.util.concurrent.ExecutionException;

public class WarehouseFactory implements DigitalTwinFactory<Warehouse> {
    @Override
    public void createTwinsForDitto() throws ExecutionException, InterruptedException {

    }

    @Override
    public String getWOTURL() {
        return null;
    }

    @Override
    public String getPolicyURL() {
        return null;
    }

    @Override
    public void initializeThings() throws InterruptedException {

    }
}
