package org.example.SustainableCodeTest.Factory.Things;

import org.eclipse.ditto.client.DittoClient;
import org.example.SustainableCodeTest.Factory.DigitalTwinFactory;
import org.example.ThingHandler;
import org.example.Things.WarehouseThing.Warehouse;

import java.util.concurrent.ExecutionException;

public class WarehouseFactory implements DigitalTwinFactory<Warehouse> {

    ThingHandler thingHandler = new ThingHandler();
    DittoClient dittoClient;
    Warehouse warehouseMain;


    public WarehouseFactory(DittoClient dittoClient){
        this.dittoClient = dittoClient;
    }
    @Override
    public void createTwinsForDitto() throws ExecutionException, InterruptedException {
        initializeThings();
        thingHandler.createTwinAndPolicy(dittoClient, getWOTURL(), getPolicyURL(), warehouseMain.getThingId()).toCompletableFuture();
    }

    @Override
    public String getWOTURL() {
        return "https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/Warehouse/WarehouseMain?cb=" + System.currentTimeMillis();
    }

    @Override
    public String getPolicyURL() {
        return "https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/lkwpolicy";
    }

    @Override
    public void initializeThings() throws InterruptedException {
        warehouseMain = new Warehouse();
        warehouseMain.setStarterValues("mything:Warehouse-Main");
    }

    public Warehouse getWarehouseMain() {
        return warehouseMain;
    }
}
