package org.example.Things.TruckThing;

import org.example.Things.Location;

import java.util.Map;

public class TruckTargetDecision<T> {
    private T target;
    private double distance;
    private Location targetLocation;
    private String targetName;


    public TruckTargetDecision(T target, double distance, Location targetLocation, String targetName){
        this.target = target;
        this.distance = distance;
        this.targetLocation = targetLocation;
        this.targetName = targetName;
    }
    public TruckTargetDecision(Location targetLocation, String targetName){
        this.targetLocation = targetLocation;
        this.targetName = targetName;
    }

    public double getDistance() {
        return distance;
    }

    public T getDecidedTarget() {
        return target;
    }

    public Location getTargetLocation() {
        return targetLocation;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetLocation(Location targetLocation) {
        this.targetLocation = targetLocation;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }
}
