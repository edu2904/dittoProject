package org.example.Things.TaskThings;

import org.example.TaskManager;
import org.example.Things.TruckThing.Truck;

import javax.security.auth.Subject;
import javax.swing.table.TableRowSorter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Task {

    private String thingId;
    private TaskStatus status;
    private String targetTruck;
    private String creationTime;
    private  TaskType taskType;
    private String setId;

    private final Map<String, Object> data = new HashMap<>();

    public Task(String thingId, TaskType taskType){
        this.thingId = thingId;
        this.taskType = taskType;
        this.status = TaskStatus.STARTING;
        this.creationTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
    }

    public Task(){
    }


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

    public void putData(String key, Object value){
        data.put(key, value);
    }

    public Object getData(String key) {
        return data.get(key);
    }

    public void setSetId(String setId) {
        this.setId = setId;
    }
    public String getSetId() {
        return setId;
    }

    public Truck lookForOperator(List<Truck> trucks){
        if(trucks == null || trucks.isEmpty()){
            return null;
        }
        Truck minTruck = trucks.get(0);
        for(Truck truck : trucks){
            if(minTruck.getUtilization() > truck.getUtilization()){
                minTruck = truck;
            }
        }
        return minTruck;
    }

}
