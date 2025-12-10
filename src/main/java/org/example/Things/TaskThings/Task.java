package org.example.Things.TaskThings;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Task {

    private String thingId;
    private TaskStatus status;
    private List<String> targetTrucks;
    private String creationTime;
    private  TaskType taskType;
    private String setId;
    private double time;
    private boolean tracked = false;
    private double startTime;
    private String eventInformation;

    private Map<String, Double> thingAllocation = new HashMap<>();

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

    public List<String> getTargetTrucks() {
        return targetTrucks;
    }

    public void setTargetTrucks(List<String> targetTrucks) {
        this.targetTrucks = targetTrucks;
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

    public Map<String, Object> getAllData(){
        return data;
    }

    public void setSetId(String setId) {
        this.setId = setId;
    }
    public String getSetId() {
        return setId;
    }

    public void setTime(double time) {
        this.time = time;
    }
    public double getTime() {
        return time;
    }

    public double getStartTime() {
        return startTime;
    }

    public void setEventInformation(String eventInformation) {
        this.eventInformation = eventInformation;
    }

    public String getEventInformation() {
        return eventInformation;
    }

    public void startTimeTracking(){
        if(tracked) return;
        else this.startTime = System.currentTimeMillis();
        tracked = true;
    }


    public Map<String, Double> getThingAllocation() {
        return thingAllocation;
    }
    public void setThingAllocation(Map<String, Double> allocation){
        this.thingAllocation = allocation;
    }
}
