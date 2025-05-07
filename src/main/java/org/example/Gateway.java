
package org.example;

import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.json.JsonValue;
import org.eclipse.ditto.things.model.Feature;
import org.eclipse.ditto.things.model.ThingId;
import org.example.Things.LKW;

import java.util.concurrent.*;

public class Gateway {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);





    public void updateLKWFeatureValue(DittoClient dittoClient, String featureID, String propertyName, double featureAmount, ThingId thingId) throws ExecutionException, InterruptedException {
        dittoClient.twin().startConsumption().toCompletableFuture();

        dittoClient.twin()
                .forFeature(thingId, featureID)
                .mergeProperty(propertyName, featureAmount)
                .whenComplete(((adaptable, throwable) -> {
                    if (throwable != null) {
                        System.out.println("Received error while sending MergeThing: '{}' " + throwable.getMessage());
                    } else {
                        System.out.println("Merge operation completed successfully");
                    }
                }));

    }
    public void startUpdatingFuel(DittoClient dittoClient, LKW lkw){
        Runnable updateTask = () -> {
            try {
                updateLKWFeatureValue(dittoClient, "Velocity", "amount", lkw.getVelocity(), ThingId.of(lkw.getThingId()));

                updateLKWFeatureValue(dittoClient, "Progress","amount", lkw.getProgress(), ThingId.of(lkw.getThingId()));
                updateLKWFeatureValue(dittoClient, "FuelTank","amount", lkw.getFuel(), ThingId.of(lkw.getThingId()));

            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        scheduler.scheduleAtFixedRate(updateTask, 0, 3, TimeUnit.SECONDS);
    }



    public double getFuelTankValueFromDitto(DittoClient dittoClient, ThingId thingId) throws InterruptedException, ExecutionException {
        CompletableFuture<Double> fuelAmount = new CompletableFuture<>();

        dittoClient.twin().forId(thingId)
                .retrieve()
                .thenCompose(thing -> {
                    JsonValue feature = thing.getFeatures().
                            flatMap(features -> features.getFeature("fuelTank")).
                            flatMap(Feature::getProperties).
                            flatMap(fuelTank -> fuelTank.getValue("amount"))
                            .orElse(JsonValue.nullLiteral());

                    fuelAmount.complete(feature.asDouble());
                    return CompletableFuture.completedFuture(null);
                });


        System.out.println(fuelAmount.get());
        return fuelAmount.get();
    }

    public void startGateway(DittoClient dittoClient, LKW lkw) throws ExecutionException, InterruptedException {
        startUpdatingFuel(dittoClient, lkw);
    }

}


