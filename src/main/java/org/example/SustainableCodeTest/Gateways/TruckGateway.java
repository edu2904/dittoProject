package org.example.SustainableCodeTest.Gateways;

import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.example.SustainableCodeTest.AbstractGateway;
import org.example.SustainableCodeTest.Factory.Things.TaskFactory;
import org.example.Things.TaskThings.TaskType;
import org.example.Things.TruckThing.Truck;
import org.example.Things.TruckThing.TruckEventsActions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TruckGateway extends AbstractGateway<Truck> {


    List<Truck> trucks;
    private final TruckEventsActions truckEventsActions = new TruckEventsActions();
    public TruckGateway(DittoClient dittoClient, InfluxDBClient influxDBClient, List<Truck> trucks){
        super(dittoClient, influxDBClient);
        this.trucks = trucks;
        subscribeForEventsAndActions();

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

                System.out.println(this.dittoClient);
                updateAttributes(truck);
                updateFeatures(truck);

                double truckCurrentWeight = (double) getAttributeValueFromDitto("weight", truck.getThingId());
                double truckCurrentFuelAmount = (double) getFeatureValueFromDitto("FuelTank", truck.getThingId());
                double truckCurrentProgress = (double) getFeatureValueFromDitto("Progress", truck.getThingId());

                truckEventsActions.progressResetAction(this.dittoClient, truck.getThingId(), truck, truckCurrentProgress);
                truckEventsActions.weightEvent(this.dittoClient, truck.getThingId(), truckCurrentWeight);
                truckEventsActions.fuelAmountEvents(this.dittoClient, truck.getThingId(), truckCurrentFuelAmount);

                checkRefuelTask(this.dittoClient, truckCurrentFuelAmount, truck);

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

    }
    @Override
    public void updateFeatures(Truck truck) throws ExecutionException, InterruptedException {
        updateFeatureValue("TirePressure", "amount", truck.getTirePressure(), truck.getThingId());
        updateFeatureValue("Velocity", "amount", truck.getVelocity(), truck.getThingId());
        updateFeatureValue("Progress","amount", truck.getProgress(), truck.getThingId());
        updateFeatureValue("Progress","destinationStatus", truck.getStops(), truck.getThingId());
        updateFeatureValue("FuelTank","amount", truck.getFuel(), truck.getThingId());

    }

    @Override
    public void logToInfluxDB(Truck truck, String measurementType) {
        String thingID = truck.getThingId();
        startLoggingToInfluxDB(measurementType, thingID, "FuelAmount", truck.getFuel());
        startLoggingToInfluxDB(measurementType, thingID, "ProgressAmount", truck.getProgress());
        startLoggingToInfluxDB(measurementType, thingID, "TirePressureAmount", truck.getTirePressure());
        startLoggingToInfluxDB(measurementType, thingID, "VelocityAmount", truck.getVelocity());

    }

    @Override
    public void handleEvents(Truck thing) {

    }

    @Override
    public void handelActions(Truck thing) {

    }

    @Override
    public void subscribeForEventsAndActions() {
        for(Truck truck: trucks){
            truckEventsActions.startTruckLogging(truck.getThingId());
        }
    }

    public void checkRefuelTask(DittoClient dittoClient, double currentFuel, Truck truck) throws ExecutionException, InterruptedException {
        if(currentFuel < 45) {
            if (!truck.isFuelTaskActive()) {
                TaskFactory taskFactory = new TaskFactory(dittoClient, TaskType.REFUEL, truck);

                taskFactory.createTwinsForDitto();

                TaskGateway taskGateway = new TaskGateway(dittoClient, this.influxDBClient, taskFactory.getTasks(), truck);
                taskGateway.startUpdating(taskFactory.getTasks());

                truck.setFuelTaskActive(true);
            }
        }
    }


}
