package org.example.Factory.ConcreteFactories;

import org.eclipse.ditto.client.DittoClient;
import org.example.Factory.DigitalTwinFactory;
import org.example.Things.WarehouseThing.WarehouseStatus;
import org.example.util.ThingHandler;
import org.example.Things.WarehouseThing.Warehouse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class WarehouseFactory implements DigitalTwinFactory<Warehouse> {

    ThingHandler thingHandler;
    DittoClient dittoClient;
    List<Warehouse> warehouseList = new ArrayList<>();

    public WarehouseFactory(DittoClient dittoClient, ThingHandler thingHandler){
        this.dittoClient = dittoClient;
        this.thingHandler = thingHandler;
    }
    @Override
    public void createTwinsForDitto() throws ExecutionException, InterruptedException {
        initializeThings();
        for (Warehouse warehouse : warehouseList) {
            thingHandler.createTwinAndPolicy(dittoClient, getWOTURL(), getPolicyURL(), warehouse.getThingId()).toCompletableFuture().join();
        }
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
    public void initializeThings() {
        Warehouse warehouseMain = createDefaultWarehouse(0,48.0842, 11.5302, true);
        Warehouse warehouse1 = createDefaultWarehouse(1, 48.1145, 11.4859, false);
        Warehouse warehouse2 = createDefaultWarehouse(2, 48.1361, 11.5010, false);
        Warehouse warehouse3 = createDefaultWarehouse(3, 48.1812, 11.5096, false);
        Warehouse warehouse4 = createDefaultWarehouse(4, 48.2234, 11.4706, false);
        Warehouse warehouse5 = createDefaultWarehouse(5, 48.2806, 11.5694, false);
        warehouseList.add(warehouseMain);
        warehouseList.add(warehouse1);
        warehouseList.add(warehouse2);
        warehouseList.add(warehouse3);
        warehouseList.add(warehouse4);
        warehouseList.add(warehouse5);
    }

    public Warehouse createDefaultWarehouse(int number, double lat, double lon, boolean mainWarehouse){
        Warehouse warehouse = new Warehouse();

        if(mainWarehouse){
            warehouse.setThingId("warehousemain:Warehouse-Main");
        }else {
            warehouse.setThingId("warehouse:Warehouse-" + number);
        }

        int capacity = (int) ((Math.random() * 101) + 400);
        warehouse.setCapacity(capacity);
        warehouse.setInventory((int) (capacity * (0.8 + Math.random() * 0.2)));
        warehouse.setWorkers(1);
        warehouse.setStatus(WarehouseStatus.WAITING);
        warehouse.setLocation(lat, lon);
        warehouse.setMainWarehouse(mainWarehouse);
        return warehouse;
    }

    @Override
    public List<Warehouse> getThings() {
        return Collections.unmodifiableList(warehouseList);
    }

}
