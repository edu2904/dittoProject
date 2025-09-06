package org.example.Gateways.ConcreteGateways;

import com.eclipsesource.json.Json;
import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.client.changes.ChangeAction;
import org.eclipse.ditto.protocol.TopicPath;
import org.eclipse.ditto.wot.model.Action;
import org.eclipse.ditto.wot.model.Actions;
import org.example.Things.Location;
import org.example.Things.TaskThings.Task;
import org.example.util.Config;
import org.example.Gateways.AbstractGateway;
import org.example.TaskManager;
import org.example.Things.TaskThings.TaskType;
import org.example.Things.TruckThing.Truck;
import org.example.Things.TruckThing.TruckEventsActions;
import org.example.Things.TruckThing.TruckStatus;
import org.example.Things.TaskThings.TaskActions;

import javax.swing.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class TruckGateway extends AbstractGateway<Truck> {


    List<Truck> trucks;
    private final TruckEventsActions truckEventsActions = new TruckEventsActions(dittoClient);

    public TruckGateway(DittoClient dittoClient, DittoClient listenerClient, InfluxDBClient influxDBClient, List<Truck> trucks){
        super(dittoClient, listenerClient, influxDBClient);
        this.trucks = trucks;
        subscribeToAttributeChanges("truck");
        registerForTasks();

    }

    @Override
    public void startGateway() {
        trucks.forEach(this::startUpdating);
    }


    @Override
    public void startUpdating(Truck truck){
            try {

                updateAttributes(truck);
                updateFeatures(truck);
                logToInfluxDB(truck, "Truck");
            } catch (ExecutionException | InterruptedException e) {
                logger.error("ERROR WHILE UPDATING TRUCK {} WITH ERROR MESSAGE: {}", truck.getThingId(), e);
            }

        }


    @Override
    public String getWOTURL() {
        return "https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/LKW/lkwMain?cb=" + System.currentTimeMillis();
    }

    @Override
    public void updateAttributes(Truck truck) {
        updateAttributeValue("thingId", truck.getThingId(), truck.getThingId());
        updateAttributeValue("weight", truck.getWeight(), truck.getThingId());
        updateAttributeValue("status", truck.getStatus().toString(), truck.getThingId());
        updateAttributeValue("capacity", truck.getCapacity(), truck.getThingId());
        updateAttributeValue("utilization", truck.getUtilization(), truck.getThingId());



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
        //updateFeatureValue("Progress","destinationStatus", truck.getStops(), truck.getThingId());
        updateFeatureValue("FuelTank","amount", truck.getFuel(), truck.getThingId());
        updateFeatureValue("Inventory","amount", truck.getInventory(), truck.getThingId());

    }

    @Override
    public void logToInfluxDB(Truck truck, String measurementType) throws ExecutionException, InterruptedException {
        String thingID = truck.getThingId();
        startLoggingToInfluxDB(measurementType, thingID, "FuelAmount", getFuelFromDitto(truck));
        startLoggingToInfluxDB(measurementType, thingID, "ProgressAmount", getProgressFromDitto(truck));
        startLoggingToInfluxDB(measurementType, thingID, "TirePressureAmount", getTirePressureFromDitto(truck));
        startLoggingToInfluxDB(measurementType, thingID, "VelocityAmount", truck.getVelocity());
        startLoggingToInfluxDB(measurementType, thingID, "InventoryAmount", getInventoryFromDitto(truck));


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
    public void registerForTasks(){
        registerForTaskListener("truckLoad", TaskActions.TASK_LOAD_START, TaskType.LOAD);
        registerForTaskListener("truckUnload", TaskActions.TASK_UNLOAD_START, TaskType.UNLOAD);
    }


    public void registerForTaskListener(String identifier, String action, TaskType tasktype){
        listenerClient.live().registerForMessage(identifier, action, message -> {
            Optional<?> optionalObject = message.getPayload();
           if(optionalObject.isPresent()) {
               try {


                String rawPayload = optionalObject.get().toString();
                var parsePayload = Json.parse(rawPayload).asObject();
                String thingId = parsePayload.get("thingId").asString();
                String to = parsePayload.get("to").asString();
                String from = parsePayload.get("from").asString();
                double quantity = parsePayload.get("quantity").asDouble();
                Truck truck = trucks
                        .stream()
                        .filter(t -> t.getThingId().equals(thingId))
                        .findFirst()
                        .orElse(null);

                if (message.getSubject().equals(action)) {
                    assert truck != null;
                    logger.info("{} reveived order {}", truck.getThingId(), action);
                     truck.setAssignedTaskValues(from, to, quantity, tasktype);
                }else {
                    logger.warn("Truck {} not found for task {}", thingId, action);
                }
            }catch (Exception e){
                   logger.error("ERROR WHILE PROCESSING TASK DATA FOR TRUCK WITH ERROR MESSAGE: {}",e.getMessage());
               }
           }
        });

    }
}



