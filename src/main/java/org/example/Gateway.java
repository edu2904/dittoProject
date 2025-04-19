package org.example;

import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.json.JsonValue;
import org.eclipse.ditto.things.model.Feature;
import org.eclipse.ditto.things.model.ThingId;
import org.example.Things.LKW;

import java.util.concurrent.*;

public class Gateway {
    public LKW lkw = new LKW();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);




    public void updateFuelTankValue(DittoClient dittoClient) throws ExecutionException, InterruptedException {
        dittoClient.twin().startConsumption().toCompletableFuture();

        dittoClient.twin()
                .forFeature(lkw.getThingId(), "fuelTank")
                .mergeProperty("amount", lkw.getFuelTankValue(dittoClient))
                .whenComplete(((adaptable, throwable) -> {
                    if (throwable != null) {
                        System.out.println("Received error while sending MergeThing: '{}' " + throwable.getMessage());
                    } else {
                        System.out.println("Merge operation completed successfully");
                    }
                }));

    }
    public void startUpdatingFuel(DittoClient dittoClient){
        Runnable updateTask = () -> {
            try {
                updateFuelTankValue(dittoClient);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        scheduler.scheduleAtFixedRate(updateTask, 0, 3, TimeUnit.SECONDS);
    }



    public double getFuelTankValueFromDitto(DittoClient dittoClient) throws InterruptedException, ExecutionException {
        CompletableFuture<Double> fuelAmount = new CompletableFuture<>();

        dittoClient.twin().forId(lkw.getThingId())
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

}
