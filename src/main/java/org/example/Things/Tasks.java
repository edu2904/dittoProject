package org.example.Things;

import org.example.Things.TruckThing.Truck;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Tasks {

    private String thingId;
    private String status;
    private String targetTruck;
    private String creationTime;


    public String getThingId() {
        return thingId;
    }

    public void setThingId(String thingId) {
        this.thingId = thingId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTargetTruck() {
        return targetTruck;
    }

    public void setTargetTruck(String targetTruck) {
        this.targetTruck = targetTruck;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }

    public void createRefuelTask(Truck truck){
        setThingId("task:refuel");
        setStatus("STARTING");
        setTargetTruck(truck.getThingId());
        setCreationTime("Created at: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));


    }
}
