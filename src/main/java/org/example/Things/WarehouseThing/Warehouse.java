package org.example.Things.WarehouseThing;

import org.example.Things.TruckThing.Truck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Warehouse {

    private final Logger logger = LoggerFactory.getLogger(Warehouse.class);

    private double inventory;
    private double capacity;
    private WarehouseStatus status;
    private int workers;
    private String thingId;

    public Warehouse (){

    }

    public String getThingId() {
        return thingId;
    }

    public void setThingId(String thingID) {
        this.thingId = thingID;
    }

    public double getInventory() {
        return inventory;
    }

    public void setInventory(double inventory) {
        this.inventory = inventory;
    }

    public double getCapacity() {
        return capacity;
    }

    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }

    public WarehouseStatus getStatus() {
        return status;
    }

    public void setStatus(WarehouseStatus warehouseStatus) {
        this.status = warehouseStatus;
    }

    public int getWorkers() {
        return workers;
    }

    public void setWorkers(int workers) {
        this.workers = workers;
    }

    public void setStarterValues(String thingId){
        setThingId(thingId);
        double capacity = (Math.random() * 101) + 400;
        setCapacity(capacity);
        setInventory(capacity - (Math.random() * 101) + 200);
        setWorkers(1);
        setStatus(WarehouseStatus.WAITING);
    }

    public void featureSimulation(){

    }
}
