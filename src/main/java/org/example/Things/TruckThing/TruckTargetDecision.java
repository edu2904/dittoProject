package org.example.Things.TruckThing;

import java.util.Map;

public class TruckTargetDecision<T> {
    private T target;
    private double distance;
    private Map<String, Object> targetLocation;
    private String targetName;


    public TruckTargetDecision(T target, double distance, Map<String, Object> targetLocation, String targetName){
        this.target = target;
        this.distance = distance;
        this.targetLocation = targetLocation;
        this.targetName = targetName;
    }

    public double getDistance() {
        return distance;
    }

    public T getDecidedTarget() {
        return target;
    }

    public Map<String, Object> getTargetLocation() {
        return targetLocation;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetLocation(Map<String, Object> targetLocation) {
        this.targetLocation = targetLocation;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }
}
