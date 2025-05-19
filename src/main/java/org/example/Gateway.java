
package org.example;

import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.json.JsonValue;
import org.eclipse.ditto.things.model.Feature;
import org.eclipse.ditto.things.model.ThingId;
import org.example.Things.GasStation;
import org.example.Things.LKW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.concurrent.*;

public class Gateway {

    private final Logger logger = LoggerFactory.getLogger(Gateway.class);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private DittoEventActionHandler dittoEventActionHandler = new DittoEventActionHandler();


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
    public void updateFeatureValue(DittoClient dittoClient, String featureID, String featurePropertyName, double featureAmount, String thingId) throws ExecutionException, InterruptedException {
        dittoClient.twin().startConsumption().toCompletableFuture();

        dittoClient.twin()
                .forFeature(ThingId.of(thingId), featureID)
                .mergeProperty(featurePropertyName, featureAmount)
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

    public void triggerProgressResetAction(DittoClient dittoClient, String thingID, LKW lkw) throws ExecutionException, InterruptedException {
        double currentprogress = getFeatureValueFromDitto("Progress", dittoClient, thingID);

        if(currentprogress == 100) {



            JsonObject object = JsonObject.newBuilder().set("message", "Progress was resettet").build();
            dittoClient
                    .live()
                    .forId(ThingId.of(thingID))
                    .message()
                    .to()
                    .subject("resetProgress")
                    .payload(object)
                    .contentType("application/json")
                    .send();

            lkw.setProgress(0);

        }

    }

    public void triggerWeightEvent(DittoClient dittoClient, String thingID) throws ExecutionException, InterruptedException, TimeoutException {
        double weightAmount = (double) getAttributeValueFromDitto("weight", dittoClient, thingID);

        JsonObject weightmessage = JsonObject.newBuilder()
                .set("amount", weightAmount)
                .build();
        JsonObject jsonWeight = JsonObject.newBuilder()
                .set("title", "Weight Status Payload")
                .set("type", "object")
                .set("properties", weightmessage).build();



        dittoClient.live()
                .forId(ThingId.of(thingID))
                .message()
                .from()
                .subject("showStatus")
                .payload(jsonWeight)
                .contentType("application/json")
                .send();




    }
    public void checkFuelAmountEvents(DittoClient dittoClient, String thingID)  {
       try {


        double fuelAmount = getFeatureValueFromDitto("FuelTank", dittoClient, thingID);
        CompletableFuture<JsonObject> fuelevent = new CompletableFuture<>();
        JsonObject lowfuelmessage = JsonObject.newBuilder()
                .set("amount", fuelAmount)
                .build();
        JsonObject jsonData = JsonObject.newBuilder()
                .set("title", "Fuel Status Payload")
                .set("type", "object")
                .set("properties", lowfuelmessage).build();
        JsonObject lowFuelFULL = JsonObject.newBuilder()
                .set("title", "Truck is low on fuel")
                .set("description", "Emittet when the trucks fuel supply is low")
                .set("data", jsonData).build();


        if (fuelAmount < 50) {




            dittoClient.live()
                    .forId(ThingId.of(thingID))
                    .forFeature("FuelTank")
                    .message()
                    .from()
                    .subject("lowfuel")
                    .payload(lowFuelFULL)
                    .contentType("application/json")
                    .send();
                       /*
                            JsonObject.class, (response, throwable) -> {
                        System.out.println("Callback wurde aufgerufen");
                        if (response != null) {
                            System.out.println("Got response: " + response);
                        } else {
                            System.out.println("Sending payload: " + lowfuelmessage);
                            System.err.println("Sending error: " + throwable.toString());
                        }

                    });


                        */


        }
       }catch (ExecutionException| InterruptedException ex){
           logger.error("Messaging Error: {}", ex.getMessage());
       }



    }



    //Zusammenfassung aller gewünschten Attribut/Feature updates für den LKW
    public void startUpdatingTruck(DittoClient dittoClient, LKW lkw){
        Runnable updateTask = () -> {
            try {
                updateAttributeValue(dittoClient, "weight", lkw.getWeight(), lkw.getThingId());
                updateAttributeValue(dittoClient, "status", lkw.getStatus().toString(), lkw.getThingId());


                updateFeatureValue(dittoClient, "Velocity", "amount", lkw.getVelocity(), lkw.getThingId());

                updateFeatureValue(dittoClient, "Progress","amount", lkw.getProgress(), lkw.getThingId());
                updateFeatureValue(dittoClient, "FuelTank","amount", lkw.getFuel(), lkw.getThingId());
                triggerProgressResetAction(dittoClient, lkw.getThingId(), lkw);
                triggerWeightEvent(dittoClient, lkw.getThingId());
                //checkFuelAmountEvents(dittoClient, lkw.getThingId());
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                throw new RuntimeException(e);
            }
        };
        scheduler.scheduleAtFixedRate(updateTask, 0, 3, TimeUnit.SECONDS);
    }

    public void startUpdatingGasStation(DittoClient dittoClient, GasStation gasStation){
        Runnable updateTask = () -> {
            try {
               updateAttributeValue(dittoClient, "status", gasStation.getGasStationStatus().toString(), gasStation.getThingId());
               updateFeatureValue(dittoClient, "GasStationFuel", "amount", gasStation.getFuelAmount(), gasStation.getThingId());
                //checkFuelAmountEvents(dittoClient, lkw.getThingId());
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        scheduler.scheduleAtFixedRate(updateTask, 0, 3, TimeUnit.SECONDS);
    }








    public void startLKWGateway(DittoClient dittoClient, LKW lkw) throws ExecutionException, InterruptedException {
        dittoEventActionHandler.createEventLogging(lkw.getThingId(), "showStatus");
        dittoEventActionHandler.createActionLogging(lkw.getThingId(), "resetProgress");
        startUpdatingTruck(dittoClient, lkw);
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


