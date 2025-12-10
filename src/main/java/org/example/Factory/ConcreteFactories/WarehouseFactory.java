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
// Creates the Warehouse that will send data to Ditto and be present in the scenario.
// They can be added, removed, and altered as long as they correspond to its WoT
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
        warehouseMain.setInventory(2300);
        Warehouse warehouse1 = createDefaultWarehouse(1, 48.1145, 11.4859, false);
        warehouse1.setInventory(2150);
        Warehouse warehouse2 = createDefaultWarehouse(2, 48.1361, 11.5010, false);
        warehouse2.setInventory(2300);
        Warehouse warehouse3 = createDefaultWarehouse(3, 48.1812, 11.5096, false);
        warehouse3.setInventory(2450);
        Warehouse warehouse4 = createDefaultWarehouse(4, 48.2234, 11.4706, false);
        warehouse4.setInventory(2150);
        Warehouse warehouse5 = createDefaultWarehouse(5, 48.2806, 11.5694, false);
        warehouse5.setInventory(2450);
        warehouseList.add(warehouseMain);
        warehouseList.add(warehouse1);
        warehouseList.add(warehouse2);
        warehouseList.add(warehouse3);
        warehouseList.add(warehouse4);
        warehouseList.add(warehouse5);
    }

    public Warehouse createDefaultWarehouse(int number, double lat, double lon, boolean mainWarehouse){
        //int capacity = (int) ((Math.random() * 101) + 400);
        int capacity = 4000;
        Warehouse warehouse = new Warehouse(capacity);

        if(mainWarehouse){
            warehouse.setThingId("warehousemain:Warehouse-Main");
        }else {
            warehouse.setThingId("warehouse:Warehouse-" + number);
        }


        warehouse.setCapacity(capacity);
        warehouse.setInventory(0);//(int) (capacity * (0.8 + Math.random() * 0.2)));
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
