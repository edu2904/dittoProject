
package org.example;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.exceptions.InfluxException;
import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.json.JsonValue;
import org.eclipse.ditto.things.model.Feature;
import org.eclipse.ditto.things.model.ThingId;
import org.example.DittoEventAction.DittoEventActionHandler;
import org.example.Things.GasStationThing.GasStation;
import org.example.Things.TaskThings.TaskStatus;
import org.example.Things.TaskThings.Tasks;
import org.example.Things.TaskThings.TasksEventsActions;
import org.example.Things.TruckThing.Truck;
import org.example.Things.TruckThing.TruckEventsActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.influxdb.client.write.Point;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class GatewayMain {

    private GasStation gasStation;
    private List<Truck> truckList = new ArrayList<>();

    private final String refuelTaskURL = "https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/instructionThings/refuelTruck";
    private String policyURL = "https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/lkwpolicy";

    private final Logger logger = LoggerFactory.getLogger(GatewayMain.class);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final DittoEventActionHandler dittoEventActionHandler = new DittoEventActionHandler();
    public TruckEventsActions truckEventsActions = new TruckEventsActions();
    public TasksEventsActions tasksEventsActions = new TasksEventsActions();
    public ThingHandler thingHandler = new ThingHandler();

    private static char[] token = "qRQO5nOdFeWKC0Zt_3Uz7ZWImtgFcaUZTOhAcUMrO9dzHzODRMRFainLa380V56XtsjHRMHcSI7Fw2f2RZooWA==".toCharArray();
    private static String org = "admin";
    private static String bucket = "ditto";
    InfluxDBClient influxDBClient = InfluxDBClientFactory.create("http://localhost:8086/", token, org, bucket);


    public void initializeThings(){
        gasStation = new GasStation();
        gasStation.setStarterValues();

        for(int i = 1; i <= 2; i++){
            Truck truck = new Truck();
            truck.setStarterValues(i);
            truck.setGasStation(gasStation);
            truckList.add(truck);
        }
    }

    public List<Truck> getTruckList(){
        return truckList;
    }

    public GasStation getGasStation(){
        return gasStation;
    }

   //Nimmt einen Attribut Wert von LKW und schickt es nach eclipse ditto
    public void updateAttributeValue(DittoClient dittoClient, String attributeName, Object attributeAmount, String thingId){
        dittoClient.twin()
                .forId(ThingId.of(thingId))
                .mergeAttribute(attributeName, JsonValue.of(attributeAmount))
                .whenComplete(((adaptable, throwable) -> {
                     if (throwable != null) {
                         logger.error("Received error while sending Attribute MergeThing for: {} {}",  attributeName, throwable.getMessage());
                     } else {
                         logger.debug("Attribute Merge operation completed successfully for: {}",  thingId);
            }
        }));

    }

    //Nimmt einen Feature Wert von LKW und schickt es nach eclipse ditto
    public void updateFeatureValue(DittoClient dittoClient, String featureID, String featurePropertyName, Object featureAmount, String thingId) throws ExecutionException, InterruptedException {
        dittoClient.twin().startConsumption().toCompletableFuture();

        dittoClient.twin()
                .forFeature(ThingId.of(thingId), featureID)
                .mergeProperty(featurePropertyName, JsonValue.of(featureAmount))
                .whenComplete(((adaptable, throwable) -> {
                    if (throwable != null) {
                        logger.error("Received error while sending Feature MergeThing for: {} {}",  featureID, throwable.getMessage());
                    } else {
                        logger.debug("Feature Merge operation completed successfully for: {}",  thingId);
                    }
                }));

    }


    public double getFeatureValueFromDitto(String featureProperty, DittoClient dittoClient, String thingId) throws InterruptedException, ExecutionException {
        CompletableFuture<Double> featureAmount = new CompletableFuture<>();

        dittoClient.twin().forId(ThingId.of(thingId))
                .retrieve()
                .thenCompose(thing -> {
                    JsonValue feature = thing.getFeatures().
                            flatMap(features -> features.getFeature(featureProperty)).
                            flatMap(Feature::getProperties).
                            flatMap(fuelTank -> fuelTank.getValue("amount"))
                            .orElse(JsonValue.nullLiteral());

                    featureAmount.complete(feature.asDouble());
                    return CompletableFuture.completedFuture(null);
                });
        return featureAmount.get();
    }

    public Object getAttributeValueFromDitto(String attributeProperty, DittoClient dittoClient, String thingId) throws InterruptedException, ExecutionException {
        CompletableFuture<Double> attributeAmount = new CompletableFuture<>();

        dittoClient.twin().forId(ThingId.of(thingId))
                .retrieve()
                .thenCompose(thing -> {
                    JsonValue feature = thing.getAttributes().
                            flatMap(attributes -> attributes.getValue(attributeProperty))
                            .orElse(JsonValue.nullLiteral());

                    attributeAmount.complete(feature.asDouble());
                    return CompletableFuture.completedFuture(null);
                });
        return attributeAmount.get();
    }

    public void createRefuelTask(DittoClient dittoClient, Truck truck, double fuelAmount) throws ExecutionException, InterruptedException {

        Tasks refuelTask = new Tasks();
        refuelTask.initializeRefuelTask(truck);
        if(fuelAmount <= 39 && !truck.isFuelTaskActive()) {
            thingHandler.createTwinAndPolicy(dittoClient, refuelTaskURL, policyURL, refuelTask.getThingId()).thenRun(() -> {
                try {
                    startUpdatingTask(dittoClient, refuelTask, truck);

                    truck.setFuelTaskActive(true);
                } catch (Exception e) {
                    logger.info(e.getMessage());
                }
            });
            refuelTask.setStatus(TaskStatus.UNDERGOING);
        }
    }

    public void startUpdatingTask(DittoClient dittoClient, Tasks tasks, Truck truck){
        TasksEventsActions tasksEventsActions = new TasksEventsActions();
        tasksEventsActions.startTaskLogging(tasks.getThingId());
        final ScheduledFuture<?>[] future = new ScheduledFuture<?>[1];

        Runnable updateTask = () -> {
            updateAttributeValue(dittoClient, "status", tasks.getStatus().toString(), tasks.getThingId());

            updateAttributeValue(dittoClient, "targetTruck", tasks.getTargetTruck(), tasks.getThingId());
            updateAttributeValue(dittoClient, "creationDate", tasks.getCreationTime(), tasks.getThingId());

            double truckCurrentFuelAmount = 0;
            try {
                truckCurrentFuelAmount = getFeatureValueFromDitto("FuelTank", dittoClient, truck.getThingId());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }


            if(truckCurrentFuelAmount == 300){
                tasks.setStatus(TaskStatus.FINISHED);
                updateAttributeValue(dittoClient, "status", tasks.getStatus().toString(), tasks.getThingId());

                try {
                    Thread.sleep(1000);
                    thingHandler.deleteThing(dittoClient, tasks.getThingId());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                future[0].cancel(false);
                truck.setFuelTaskActive(false);
            }
            try {
                tasksEventsActions.handleRefuelTaskEvents(dittoClient, tasks);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        };
        ScheduledFuture<?> future1 = scheduler.scheduleAtFixedRate(updateTask, 0, 3, TimeUnit.SECONDS);
        future[0] = future1;
    }




    //Zusammenfassung aller gewünschten Attribut/Feature updates für den LKW
    public void startUpdatingTruck(DittoClient dittoClient, Truck truck){

        truckEventsActions.startTruckLogging(truck.getThingId());
        Runnable updateTask = () -> {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();

            try {
                // Updates for the attribute Values
                updateAttributeValue(dittoClient, "weight", truck.getWeight(), truck.getThingId());
                updateAttributeValue(dittoClient, "status", truck.getStatus().toString(), truck.getThingId());
                // Updates for the feature Values
                updateFeatureValue(dittoClient, "TirePressure", "amount", truck.getTirePressure(), truck.getThingId());
                updateFeatureValue(dittoClient, "Velocity", "amount", truck.getVelocity(), truck.getThingId());
                updateFeatureValue(dittoClient, "Progress","amount", truck.getProgress(), truck.getThingId());
                updateFeatureValue(dittoClient, "Progress","destinationStatus", truck.getStops(), truck.getThingId());
                updateFeatureValue(dittoClient, "FuelTank","amount", truck.getFuel(), truck.getThingId());


                try {


                    Point point = Point.measurement("Truck")
                            .addTag("thingID", truck.getThingId())
                            .addField("FuelTankAmount", truck.getFuel())
                            .time(Instant.now().toEpochMilli(), WritePrecision.MS);

                    writeApi.writePoint(point);
                }catch (InfluxException e){
                    logger.error(e.getMessage());
                }
                //Values for Events and Actions
                double truckCurrentWeight = (double) getAttributeValueFromDitto("weight", dittoClient, truck.getThingId());
                double truckCurrentFuelAmount = getFeatureValueFromDitto("FuelTank", dittoClient, truck.getThingId());
                double truckCurrentProgress = getFeatureValueFromDitto("Progress", dittoClient, truck.getThingId());

                //Handle Events and Actions
                truckEventsActions.progressResetAction(dittoClient, truck.getThingId(), truck, truckCurrentProgress);
                truckEventsActions.weightEvent(dittoClient, truck.getThingId(), truckCurrentWeight);
                truckEventsActions.fuelAmountEvents(dittoClient, truck.getThingId(), truckCurrentFuelAmount);

                // Handle measures to solve problems
                createRefuelTask(dittoClient, truck, truckCurrentFuelAmount);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        scheduler.scheduleAtFixedRate(updateTask, 0, 3, TimeUnit.SECONDS);
    }

    public void startUpdatingGasStation(DittoClient dittoClient, GasStation gasStation){
        Runnable updateTask = () -> {
            try {
               updateAttributeValue(dittoClient, "status", gasStation.getGasStationStatus().toString(), gasStation.getThingId());
               updateFeatureValue(dittoClient, "GasStationFuel", "amount", gasStation.getGasStationFuelAmount(), gasStation.getThingId());
                //checkFuelAmountEvents(dittoClient, lkw.getThingId());
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        scheduler.scheduleAtFixedRate(updateTask, 0, 3, TimeUnit.SECONDS);
    }


    public void startLKWGateway(DittoClient dittoClient, Truck truck) throws ExecutionException, InterruptedException{
          startUpdatingTruck(dittoClient, truck);
    }
    public void startGasStationGateway(DittoClient dittoClient, GasStation gasStation) throws ExecutionException, InterruptedException {
        startUpdatingGasStation(dittoClient, gasStation);
    }



















/*
    public void messageTest(DittoClient dittoClient, String thingID) throws ExecutionException, InterruptedException {
        dittoClient.live().startConsumption().toCompletableFuture().get();
        dittoClient
                .live().forId(ThingId.of(thingID))
                .registerForMessage("statushandler", "showStatus", message -> {
                    System.out.println("status empfangen" + message.getSubject());
                    message.reply()
                            .httpStatus(HttpStatus.ACCEPTED)
                            .payload("Hello, I'm just a Teapot!")
                            .send();
                });

        System.out.println("Listener registered for showStatus");
    }
*/
}


