package org.example.Gateways.ConcreteGateways;

import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.example.Things.Location;
import org.example.util.Config;
import org.example.Gateways.AbstractGateway;
import org.example.TaskManager;
import org.example.Things.TaskThings.TaskType;
import org.example.Things.TruckThing.Truck;
import org.example.Things.TruckThing.TruckEventsActions;
import org.example.Things.TruckThing.TruckStatus;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class TruckGateway extends AbstractGateway<Truck> {


    List<Truck> trucks;
    private final TruckEventsActions truckEventsActions = new TruckEventsActions(dittoClient);

    private final TaskManager taskManager;
    public TruckGateway(DittoClient dittoClient, InfluxDBClient influxDBClient, List<Truck> trucks){
        super(dittoClient, influxDBClient);
        this.trucks = trucks;
        this.taskManager = new TaskManager(dittoClient, influxDBClient);

    }

    @Override
    public void startGateway() {
          for(Truck truck : trucks){
              startUpdating(truck);
          }
    }


    @Override
    public void startUpdating(Truck truck){
            try {

                updateAttributes(truck);
                updateFeatures(truck);

                //truckEventsActions.progressResetAction(this.dittoClient, truck.getThingId(), truck, getProgressFromDitto(truck));
                truckEventsActions.weightEvent(truck.getThingId(), getWeightFromDitto(truck));
                truckEventsActions.fuelAmountEvents(truck.getThingId(), getFuelFromDitto(truck));
               // truckEventsActions.taskSearchAction(truck.getThingId(), getWeightFromDitto(truck), 7000);
                truckEventsActions.arrivalEvent(truck.getThingId(), getTargetLocationFromDitto(truck), getLocationFromDitto(truck), getTargetNameFromDitto(truck));
                truckEventsActions.checkForTruckWithoutTask(truck.getThingId(), truck.getStatus());

                checkRefuelTask(getFuelFromDitto(truck), truck);
                checkTirePressureTask(getTirePressureFromDitto(truck), truck);
                checkLoadingTask(getInventoryFromDitto(truck), truck);

                logToInfluxDB(truck, "Truck");
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }

        }


    @Override
    public String getWOTURL() {
        return "https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/LKW/lkwMain?cb=" + System.currentTimeMillis();
    }

    @Override
    public void updateAttributes(Truck truck) {
        updateAttributeValue("weight", truck.getWeight(), truck.getThingId());
        updateAttributeValue("status", truck.getStatus().toString(), truck.getThingId());
        updateAttributeValue("capacity", truck.getCapacity(), truck.getThingId());


        updateAttributeValue("location/geo:lat", truck.getLocation().getLat(), truck.getThingId());
        updateAttributeValue("location/geo:long", truck.getLocation().getLon(), truck.getThingId());

        if(truck.getTarget() != null) {
            updateAttributeValue("targetLocation/name", truck.getTarget().getTargetName(), truck.getThingId());
            updateAttributeValue("targetLocation/geo:lat", truck.getTarget().getTargetLocation().getLat(), truck.getThingId());
            updateAttributeValue("targetLocation/geo:long", truck.getTarget().getTargetLocation().getLon(), truck.getThingId());
        }
    }
    @Override
    public void updateFeatures(Truck truck) throws ExecutionException, InterruptedException {
        updateFeatureValue("TirePressure", "amount", truck.getTirePressure(), truck.getThingId());
        updateFeatureValue("Velocity", "amount", truck.getVelocity(), truck.getThingId());
        updateFeatureValue("Progress","amount", truck.getProgress(), truck.getThingId());
        updateFeatureValue("Progress","destinationStatus", truck.getStops(), truck.getThingId());
        updateFeatureValue("FuelTank","amount", truck.getFuel(), truck.getThingId());
        updateFeatureValue("Inventory","amount", truck.getInventory(), truck.getThingId());

    }

    @Override
    public void logToInfluxDB(Truck truck, String measurementType) throws ExecutionException, InterruptedException {
        System.out.println("Fuel before logging " + getProgressFromDitto(truck));
        String thingID = truck.getThingId();
        //startLoggingToInfluxDB(measurementType, thingID, "FuelAmount", getFuelFromDitto(truck));
        startLoggingToInfluxDB(measurementType, thingID, "ProgressAmount", getProgressFromDitto(truck));
        //startLoggingToInfluxDB(measurementType, thingID, "TirePressureAmount", getTirePressureFromDitto(truck));
        //startLoggingToInfluxDB(measurementType, thingID, "VelocityAmount", truck.getVelocity());
       // startLoggingToInfluxDB(measurementType, thingID, "InventoryAmount", getInventoryFromDitto(truck));


    }

    public void checkRefuelTask(double currentFuel, Truck truck){
        try {
            if (currentFuel < Config.FUEL_MIN_VALUE_STANDARD_TRUCK) {
                taskManager.startTask(TaskType.REFUEL, truck);
            }
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error creating Refuel Task for " + truck.getThingId(), e);
        }
    }

    public void checkTirePressureTask(double currentTirePressure, Truck truck) throws ExecutionException, InterruptedException {
        try {
            if(currentTirePressure < Config.TIRE_PRESSURE_MIN_VALUE_STANDARD_TRUCK){
            taskManager.startTask(TaskType.TIREPRESSUREADJUSTMENT, truck);
            }
        } catch (ExecutionException | InterruptedException e) {
        logger.error("Error Creating TirePressure task for " + truck.getThingId(), e);
        }

    }
    public void checkLoadingTask(double currentInventory, Truck truck) {
        try{
            if(currentInventory == 0 && !(truck.getStatus() == TruckStatus.WAITING)){
                taskManager.startTask(TaskType.LOAD, truck);
            }
        }catch (ExecutionException | InterruptedException e) {
            logger.error("Error creating Loading Task for " + truck.getThingId(), e);
        }
    }

    public double getWeightFromDitto(Truck truck) throws ExecutionException, InterruptedException {
        return (double) getAttributeValueFromDitto("weight", truck.getThingId());
    }
    public double getFuelFromDitto(Truck truck) throws ExecutionException, InterruptedException {
        return (double) getFeatureValueFromDitto("FuelTank", truck.getThingId());
    }
    public double getProgressFromDitto(Truck truck) throws ExecutionException, InterruptedException {
        return (double) getFeatureValueFromDitto("Progress", truck.getThingId());
    }

    public double getTirePressureFromDitto(Truck truck) throws ExecutionException, InterruptedException {
        return (double) getFeatureValueFromDitto("TirePressure", truck.getThingId());
    }
    public double getInventoryFromDitto(Truck truck) throws ExecutionException, InterruptedException {
        return (double) getFeatureValueFromDitto("Inventory", truck.getThingId());
    }
    public Location getLocationFromDitto(Truck truck) throws ExecutionException, InterruptedException {
         double lat = (Double) getAttributeValueFromDitto("location/geo:lat", truck.getThingId());
         double lon = (Double) getAttributeValueFromDitto("location/geo:long", truck.getThingId());
        return new Location(lat, lon);
    }
    public Location getTargetLocationFromDitto(Truck truck) throws ExecutionException, InterruptedException {
        if (truck.getTarget() != null) {
            double lat = (Double) getAttributeValueFromDitto("targetLocation/geo:lat", truck.getThingId());
            double lon = (Double) getAttributeValueFromDitto("targetLocation/geo:long", truck.getThingId());
           return new Location(lat, lon);
        }
        return null;
    }

    public String getTargetNameFromDitto(Truck truck) throws ExecutionException, InterruptedException {
        if (truck.getTarget() != null){
        return (String) getAttributeValueFromDitto("targetLocation/name", truck.getThingId());
        }
        return null;
    }

}



