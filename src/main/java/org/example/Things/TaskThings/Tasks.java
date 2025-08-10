package org.example.Things.TaskThings;

import org.example.Things.TruckThing.Truck;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Tasks {

    private String thingId;
    private TaskStatus status;
    private String targetTruck;
    private String creationTime;
    private  TaskType taskType;


    public String getThingId() {
        return thingId;
    }

    public void setThingId(String thingId) {
        this.thingId = thingId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
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

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

}
