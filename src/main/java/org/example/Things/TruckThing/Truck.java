package org.example.Things.TruckThing;

import org.eclipse.ditto.client.DittoClient;
import org.example.Things.Location;
import org.example.Things.TaskThings.Task;
import org.example.Things.TaskThings.TaskType;
import org.example.util.Config;
import org.example.util.GeoConst;
import org.example.util.GeoUtil;
import org.example.util.ThingHandler;
import org.example.Things.GasStationThing.GasStation;
import org.example.Things.WarehouseThing.Warehouse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Truck {

    Location location;

    private final Logger logger = LoggerFactory.getLogger(Truck.class);
    private final AtomicBoolean taskActive = new AtomicBoolean(false);
    private final AtomicBoolean truckArrived = new AtomicBoolean(false);
    private final AtomicInteger currentStopIndex = new AtomicInteger(1);
    private String thingId;
    private TruckStatus truckStatus;
    private double weight;
    private double velocity;
    private double tirePressure;
    private double progress;
    private double fuel;
    private double capacity;
    private double inventory;
    private double utilization;
    private ArrayList<Integer> stops;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ThingHandler thingHandler = new ThingHandler();
    private final Queue<String> tasksQueue = new LinkedList<>();
    private List<Warehouse> warehouseList = new ArrayList<>();
    private List<GasStation> gasStationList = new ArrayList<>();
    private TruckTargetDecision<?> target;
    private TruckTargetDecision<?> recommendedTarget;
    private TaskType taskType;

    double fuelConsumption;
    Warehouse startWarehouse;
    Warehouse targetWarehouse;
    double cargoToBeDelivered;
    boolean taskSuccess;

    public String getThingId() {
        return thingId;
    }

    public void setThingId(String thingId) {
        this.thingId = thingId;
    }

    public TruckStatus getStatus() {
        return truckStatus;
    }

    public void setStatus(TruckStatus status) {
        this.truckStatus = status;
    }

    public double getCapacity() {
        return capacity;
    }

    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getVelocity() {
        return velocity;
    }

    public void setVelocity(double velocity) {
        this.velocity = velocity;
    }

    public double getTirePressure() {
        return tirePressure;
    }

    public void setTirePressure(double tirePressure) {
        this.tirePressure = tirePressure;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public void setFuel(double fuel) {
        this.fuel = fuel;
    }

    public double getFuel() {
        return fuel;
    }
    public double getInventory() {
        return inventory;
    }
    public void setInventory(double inventory) {
        this.inventory = inventory;
    }
    public void setStops(ArrayList<Integer> stops) {
        this.stops = stops;
    }

    public ArrayList<Integer> getStops() {
        return stops;
    }

    public void setGasStationList(List<GasStation> gasStation){
        this.gasStationList = gasStation;
    }
    public List<GasStation> getGasStation(){
        return gasStationList;
    }

    public List<Warehouse> getWarehouseList() {
        return warehouseList;
    }

    public void setWarehouseList(List<Warehouse> warehouse) {
        this.warehouseList = warehouse;
    }

    public boolean isTaskActive(){
        return taskActive.get();
    }
    public void setTaskActive(boolean currentTaskActive){
        taskActive.set(currentTaskActive);
    }

    public Location getLocation() {
        return location;
    }


    public void setLocation(Location currentLocation){
        this.location = new Location(currentLocation.getLat(), currentLocation.getLon());
    }

    public double getUtilization() {
        return utilization;
    }

    public void setUtilization(double utilization) {
        this.utilization = utilization;
    }

    public synchronized TruckTargetDecision<?> getTarget() {
        return target;
    }

    public synchronized void setTarget(TruckTargetDecision<?> target) {
        this.target = target;
    }

    public void setRecommendedTarget(TruckTargetDecision<?> recommendedTarget) {
        this.recommendedTarget = recommendedTarget;
    }

    public TruckTargetDecision<?> getRecommendedTarget() {
        return recommendedTarget;
    }

    public TaskType getTask() {
        return taskType;
    }

    public void setTask(TaskType taskType) {
        this.taskType = taskType;
    }

    public double getFuelConsumption() {
        return fuelConsumption;
    }

    public void setFuelConsumption(double fuelConsumption) {
        this.fuelConsumption = fuelConsumption;
    }

    public Warehouse getTargetWarehouse() {
        return targetWarehouse;
    }

    public void setTargetWarehouse(Warehouse targetWarehouse) {
        this.targetWarehouse = targetWarehouse;
    }

    public Warehouse getStartWarehouse() {
        return startWarehouse;
    }

    public void setStartWarehouse(Warehouse startWarehouse) {
        this.startWarehouse = startWarehouse;
    }

    public double getCargoToBeDelivered() {
        return cargoToBeDelivered;
    }

    public void setCargoToBeDelivered(double cargoToBeDelivered) {
        this.cargoToBeDelivered = cargoToBeDelivered;
    }

    public void setTaskSuccess(boolean taskSuccess) {
        this.taskSuccess = taskSuccess;
    }

    public boolean isTaskSuccess() {
        return taskSuccess;
    }

    public void setAssignedTaskValues(String from, String to, double cargoToBeDelivered, TaskType taskType){
        Warehouse fromWarehouse = getWarehouseList().stream().filter(t -> t.getThingId().equals(from)).findFirst().orElse(null);
        setStartWarehouse(fromWarehouse);
        Warehouse toWarehouse = getWarehouseList().stream().filter(t -> t.getThingId().equals(to)).findFirst().orElse(null);
        setTargetWarehouse(toWarehouse);
        setCargoToBeDelivered(cargoToBeDelivered);
        setTask(taskType);

    }
    public Map<String, Double> calculateDistances(Warehouse warehouse){
        Map<String, Double> distances = new HashMap<>();

        double warehouseDistance = GeoUtil.calculateDistance(getLocation(), warehouse.getLocation());
        distances.put(warehouse.getThingId(), warehouseDistance);

        for(GasStation gasStation : getGasStation()){
            double gasStationDistance = GeoUtil.calculateDistance(getLocation(), gasStation.getLocation());
            distances.put(gasStation.getThingId(), gasStationDistance);
        }
        return distances;
    }
    public double calculateUtilization(){
        double utilization = 1.0 - Math.min(1.0, Math.max(0.0, getFuel() / Config.FUEL_MAX_VALUE_STANDARD_TRUCK));

        return utilization * 100;

    }
    public double calculateLocationUtilization(Warehouse toWarehouse){

        double maxDistance =  8000.0;
        double distance = GeoUtil.calculateDistance(getLocation(), toWarehouse.getLocation());
        double normalizedDistance = Math.min(1.0, Math.max(0.0, distance / maxDistance));
        return normalizedDistance * 100.0;
    }

    public double getAverageUtilizationForTask(double utilFuel, double utilLocation){
        System.out.println("++++++++++++++++++++++++++");
        System.out.println(getThingId() + " " + (utilFuel + utilLocation)/ 2);
        System.out.println("++++++++++++++++++++++++++");
        return (utilFuel + utilLocation)/ 2;
    }
}