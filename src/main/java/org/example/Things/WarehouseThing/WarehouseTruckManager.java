package org.example.Things.WarehouseThing;

import org.example.Things.TruckThing.Truck;
import org.example.Things.TruckThing.TruckTargetDecision;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class WarehouseTruckManager {

    private final Queue<Truck> queue = new ConcurrentLinkedQueue<>();
    private Truck activeTruck = null;

    public synchronized boolean hasActiveTruck(){
        return activeTruck != null;
    }

    public synchronized void setActiveTruck(Truck activeTruck) {
        this.activeTruck = activeTruck;
    }
    public synchronized Truck getActiveTruck() {
        return activeTruck;
    }
    public void addTruckToQueue(Truck truck){
        queue.add(truck);
    }
    public Truck getTruckFromQueue(){
        return queue.poll();
    }
    public boolean isTruckInQueue(){
        return !queue.isEmpty();
    }
}
