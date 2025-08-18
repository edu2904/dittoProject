package org.example.Things.TruckThing;

import org.eclipse.ditto.client.DittoClient;
import org.example.Things.Location;
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
    private final List<Warehouse> warehouseList = new ArrayList<>();
    private final List<GasStation> gasStationList = new ArrayList<>();
    private TruckTargetDecision<?> target;
    private TruckTargetDecision<?> recommendedTarget;




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

    public void setGasStation(GasStation gasStation){
        this.gasStationList.add(gasStation);
    }
    public List<GasStation> getGasStation(){
        return gasStationList;
    }

    public List<Warehouse> getWarehouseList() {
        return warehouseList;
    }

    public void setWarehouseList(Warehouse warehouse) {
        this.warehouseList.add(warehouse);
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
    public void updateTarget(){
        if(this.target == null && this.recommendedTarget != null){
            this.target = this.recommendedTarget;
            setTarget(target);
            setProgress(0);
            this.recommendedTarget = null;
        }
    }

    public AtomicInteger getCurrentStopIndex() {
        return currentStopIndex;
    }

    public void setDestinations(int destinations){
        //setStops(new ArrayList<>(Collections.nCopies(destinations, 0)));
        ArrayList<Integer> listDestinations = new ArrayList<>();
        for(int i = 0; i < destinations; i++){
            listDestinations.add(0);
        }
        setStops(listDestinations);
    }

    public double calculateUtilization(){
        double weightFuel = 1;
        double combinedUtilization = weightFuel * getFuel();

        return Math.min(100.0, Math.max(0.0, combinedUtilization * 100.0));

    }

    public void runSimulation(int destinations, double fuelConsumption, double progressMade, DittoClient dittoClient){
        setDestinations(destinations);
        setStarterLocation();

        scheduler.scheduleAtFixedRate(() -> {
            try {
            updateTarget();

            if (getFuel() <= 0 || getTarget() == null) {
                stopTruck();
                logger.debug("Truck {} ran out of fuel", getThingId());

            }else if(getTarget() != null) {

                checkForNewTasks(dittoClient);
                drive(fuelConsumption, progressMade, getTarget(), dittoClient);
            }
                } catch (Exception e) {
                    logger.error("Error in truck {}: {}" , thingId, e);
                }
            }, 0, Config.STANDARD_TICK_RATE, TimeUnit.SECONDS);
    }

    public void drive(double fuelConsumption, double progressMade, TruckTargetDecision<?> target, DittoClient dittoClient){

        if(isTaskActive()) return;
        setStatus(TruckStatus.DRIVING);

        tirePressureDecreases(getTirePressure());
        setVelocity(75 + Math.random() * 10);
        setFuel(getFuel() - fuelConsumption);

        double targetDistance = target.getDistance() * 1000;
        double progressPerTick = (progressMade / targetDistance) * 100;
        setProgress(Math.min(100, getProgress() + progressPerTick));
        double distanceTravelled = (getProgress() / 100) * targetDistance;


        logger.info("{} travelled {}/{} Meters", thingId, distanceTravelled, targetDistance);

        if(getProgress() >= 100 && currentStopIndex.get() <= getStops().size()){
            checkForActiveTask(dittoClient, target);
        }else if(currentStopIndex.get() > getStops().size()){
            logger.info("Truck {} finished tour", thingId);
            currentStopIndex.set(1);
            Collections.fill(stops, 0);
        }
    }

    public void stopTruck(){
        setStatus(TruckStatus.IDLE);
        setVelocity(0);
        logger.warn("Drive stopped for {}", getThingId());
    }


    public void checkForNewTasks(DittoClient dittoClient) throws ExecutionException, InterruptedException {
        String[] tasks = {
                "task:refuel_"+ getThingId(),
                "task:tirePressureLow_" + getThingId(),
                "task:loadingTruck_" + getThingId()
        };

        for(String taskID : tasks){
            if(thingHandler.thingExists(dittoClient, taskID).get() && !tasksQueue.contains(taskID)){
                tasksQueue.add(taskID);
            }
        }
    }

    public void checkForActiveTask(DittoClient dittoClient, TruckTargetDecision<?> target){

        //try {
           if(target.getDecidedTarget() instanceof GasStation){
           // if(thingHandler.thingExists(dittoClient, "task:refuel_"+ getThingId()).get()){
                if(getStatus() != TruckStatus.REFUELING && !isTaskActive()) {
                    setTaskActive(true);
                    setLocation(((GasStation) target.getDecidedTarget()).getLocation());
                    ((GasStation) target.getDecidedTarget()).startRefuel(this);
                }

            //} else if(thingHandler.thingExists(dittoClient, "task:tirePressureLow_" + getThingId()).get()) {
            //    if (getStatus() != TruckStatus.ADJUSTINGTIREPRESSURE && !isTaskActive()) {
            //        setTaskActive(true);
            //        setLocation(((GasStation) target.getTarget()).getLocation());
             //       ((GasStation) target.getTarget()).startTirePressureAdjustment(this);
            //    }
            }
          //  } else if(target.getTarget() instanceof Warehouse) {
            else if(target.getDecidedTarget() instanceof Warehouse) {
               //if (thingHandler.thingExists(dittoClient, "task:loadingTruck_" + getThingId()).get()) {
                   if (getStatus() != TruckStatus.LOADING && !isTaskActive()) {
                       setTaskActive(true);
                       setLocation(((Warehouse) target.getDecidedTarget()).getLocation());
                       ((Warehouse) target.getDecidedTarget()).startLoading(this);
                       if(!((Warehouse) target.getDecidedTarget()).isMainWareHouse()) {
                           stops.set(currentStopIndex.get() - 1, 1);
                           currentStopIndex.getAndIncrement();
                       }
                   }
               }
          // }
         //  }
       // } catch (InterruptedException | ExecutionException e) {
       //     throw new RuntimeException(e);
       // }
    }




    public void tirePressureDecreases(double tirePressure){
        if(Math.random() <= Config.TIRE_PRESSURE_DECREASE_RATE){
            double tirePressureReduction = Math.random() * 100;
            setTirePressure(tirePressure - tirePressureReduction);
        }
    }


    public void featureSimulation1(DittoClient dittoClient) {
        runSimulation( 5, 0.1, 1050, dittoClient);
    }

    public void featureSimulation2(DittoClient dittoClient) {
        runSimulation(3, 1.0, 10, dittoClient);
    }

    public Map<String, Double> calculateDistances(){
        Map<String, Double> distances = new HashMap<>();
        for(Warehouse warehouse : getWarehouseList()){
            double warehouseDistance = GeoUtil.calculateDistance(getLocation(), warehouse.getLocation());
            distances.put(warehouse.getThingId(), warehouseDistance);
        }
        for(GasStation gasStation : getGasStation()){
            double gasStationDistance = GeoUtil.calculateDistance(getLocation(), gasStation.getLocation());
            distances.put(gasStation.getThingId(), gasStationDistance);
        }
        return distances;
    }
    public void setStarterLocation(){
        for(Warehouse warehouse : warehouseList){
            if(warehouse.isMainWareHouse()){
                setLocation(warehouse.getLocation());
                break;
            }
        }
    }
}