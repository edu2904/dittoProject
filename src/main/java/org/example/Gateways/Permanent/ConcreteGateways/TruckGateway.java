package org.example.Gateways.Permanent.ConcreteGateways;

import com.eclipsesource.json.Json;
import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.example.Things.Location;
import org.example.Gateways.AbstractGateway;
import org.example.Things.TaskThings.TaskType;
import org.example.Things.TruckThing.Truck;
import org.example.Things.TruckThing.TruckEventsActions;
import org.example.Things.TaskThings.TaskActions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class TruckGateway extends AbstractGateway<Truck> {


    List<Truck> trucks;

    public TruckGateway(DittoClient dittoClient, DittoClient listenerClient, InfluxDBClient influxDBClient, List<Truck> trucks){
        super(dittoClient, listenerClient, influxDBClient);
        this.trucks = trucks;
        subscribeToAttributeChanges("truck");
        registerForTasks();
    }

    @Override
    public void startGateway() {
        trucks.forEach(this::upDateThing);
    }

    @Override
    public void updateAttributes(Truck truck) {
        var attributes = new HashMap<>(Map.<String, Object>of(
                "thingId", truck.getThingId(),
                "weight", truck.getWeight(),
                "status", truck.getStatus().toString(),
                "capacity", truck.getCapacity(),
                "utilization", truck.getUtilization(),
                "location/geo:lat", truck.getLocation().getLat(),
                "location/geo:long", truck.getLocation().getLon()
        ));

        if(truck.getTarget() != null) {
            attributes.put("targetLocation/name", truck.getTarget().getTargetName());
            attributes.put("targetLocation/geo:lat", truck.getTarget().getTargetLocation().getLat());
            attributes.put("targetLocation/geo:long", truck.getTarget().getTargetLocation().getLon());
            }

        attributes.forEach((attributeName, attributeValue) -> updateAttributeValue(attributeName, attributeValue, truck.getThingId()));
    }
    @Override
    public void updateFeatures(Truck truck){
        var features = new HashMap<String, Map<String, Object>>(Map.of(
                "TirePressure", Map.of("amount", truck.getTirePressure()),
                "Velocity", Map.of("amount", truck.getVelocity()),
                "Progress", Map.of("amount", truck.getProgress()),
                "FuelTank", Map.of("amount", truck.getFuel()),
                "Inventory", Map.of("amount", truck.getInventory())
        ));

        features.forEach((featureName, prop) ->
                prop.forEach((propName, value) ->
                        updateFeatureValue(featureName, propName, value, truck.getThingId())
                )
        );
    }

    @Override
    public void logToInfluxDB(Truck truck, String measurementType) {
        try {
            var loggingValues = new HashMap<>(Map.of(
                    "TirePressure", getTirePressureFromDitto(truck),
                    "Velocity", getVelocityFromDitto(truck),
                    "Progress", getProgressFromDitto(truck),
                    "FuelTank", getFuelFromDitto(truck),
                    "Inventory", getInventoryFromDitto(truck)
            ));
            loggingValues.forEach((influxName, value) ->
                startLoggingToInfluxDB(measurementType, truck.getThingId(), influxName, value)
            );

        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public double getVelocityFromDitto(Truck truck) throws ExecutionException, InterruptedException {
        return (double) getFeatureValueFromDitto("Velocity", "amount", truck.getThingId());
    }
    public double getFuelFromDitto(Truck truck) throws ExecutionException, InterruptedException {
        return (double) getFeatureValueFromDitto("FuelTank", "amount", truck.getThingId());
    }
    public double getProgressFromDitto(Truck truck) throws ExecutionException, InterruptedException {
        return (double) getFeatureValueFromDitto("Progress", "amount", truck.getThingId());
    }
    public double getTirePressureFromDitto(Truck truck) throws ExecutionException, InterruptedException {
        return (double) getFeatureValueFromDitto("TirePressure", "amount", truck.getThingId());
    }
    public double getInventoryFromDitto(Truck truck) throws ExecutionException, InterruptedException {
        return (double) getFeatureValueFromDitto("Inventory", "amount", truck.getThingId());
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
                    logger.info("{} received order {}", truck.getThingId(), action);
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



