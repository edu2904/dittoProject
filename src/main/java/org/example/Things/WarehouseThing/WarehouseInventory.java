package org.example.Things.WarehouseThing;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class WarehouseInventory {
    private final double capacity;
    private final AtomicReference<Double> inventory = new AtomicReference<>(0.0);

    public WarehouseInventory(double capacity){
        this.capacity = capacity;
    }
    public double getInventory() {
        return inventory.get();
    }
    public void add(double amount){
        inventory.updateAndGet(currentInventory -> Math.min(currentInventory + amount, capacity));
    }
    public void remove(double amount){
        inventory.updateAndGet(currentInventory -> Math.max(currentInventory - amount, capacity));
    }
    public double getCapacity() {
        return capacity;
    }
}
