package org.example.Things;

import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.json.JsonValue;
import org.eclipse.ditto.things.model.Feature;
import org.eclipse.ditto.things.model.ThingId;
import org.eclipse.ditto.things.model.ThingsModelFactory;
import org.example.Client.DittoClientBuilder;

import java.time.OffsetDateTime;
import java.util.concurrent.*;

public class LKW {

    public Feature fuelTankFeature;
    public Feature tirePressureFeature;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public LKW getLKW() {
        return this;
    }

    public void createLKWThing(DittoClient dittoClient) throws ExecutionException, InterruptedException {
        // var future = new CompletableFuture<Boolean>();


        ThingId thingId = ThingId.of("org.test:LKW-1");
        ThingsModelFactory.newThingBuilder()
                .setId(thingId)
                .build();


        JsonObject tirePressureProperties = JsonObject.newBuilder()
                .set("amount", 800)
                .set("unit", "kPa")
                .build();

        tirePressureFeature = Feature.newBuilder()
                .properties(tirePressureProperties)
                .withId("tirePressure").build();

        JsonObject fuelTankProperties = JsonObject.newBuilder()
                .set("amount", 100.5)
                .set("unit", "L")
                .build();

        fuelTankFeature = Feature.newBuilder()
                .properties(fuelTankProperties)
                .withId("fuelTank")
                .build();


/*
        String stringThingId = "org.test:LKW-1";
        String stringPolicy = "test:LKW-1";
        String wot = "https://example.com/tds/lkw-1.json";

        JsonObject thing = JsonObject.newBuilder()
                .set("thingId", stringThingId)
                .set("policyId", stringThingId)
                .set("wotTD", JsonObject.newBuilder()
                        .set("definition", wot)
                        .build())
                .set("attributes", JsonObject.newBuilder()
                        .set("status", "idle")
                        .set("weight", 6000)
                        .build())
                .set("features", JsonObject.newBuilder()
                        .set("fuelTank", JsonObject.newBuilder()
                                .set("properties", JsonObject.newBuilder()
                                        .set("amount", 100.5)
                                        .set("unit", "L")
                                        .build())
                                .build())
                        .set("tirePressure", JsonObject.newBuilder()
                                .set("properties", JsonObject.newBuilder()
                                        .set("amount", 800)
                                        .set("unit", "kPa")
                                        .build())
                                .build())
                        .build())
                .build();

*/

        dittoClient.twin().create(thingId).handle((createdThing, throwable) -> {
            if(createdThing != null){
                System.out.println("Created new thing: " + createdThing);
            }else {
                System.out.println("Failed to create thing or thing already created");
            }
            return new CompletionStage[]{
                    dittoClient.twin().forId(thingId).putAttribute("first-updated-at", OffsetDateTime.now().toString()),
                    dittoClient.twin().forId(thingId).putAttribute("status", "idle"),
                    dittoClient.twin().forId(thingId).putAttribute("weight", 6000),
                    dittoClient.twin().forId(thingId).putFeature(tirePressureFeature),
                    dittoClient.twin().forId(thingId).putFeature(fuelTankFeature)
            };
            //return null;
        }).toCompletableFuture().get();
        //.thenRun(() -> future.complete(true))
        //.exceptionally((t) -> {
        //    future.completeExceptionally(t);
        //    return null;
        //});
        //return future;
    };


    public double getFuelTankValue(DittoClient dittoClient) throws InterruptedException, ExecutionException {
        CompletableFuture<Double> fuelAmount = new CompletableFuture<>();

        dittoClient.twin().forId(ThingId.of("org.test:LKW-1"))
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


    public void updateFuelTankValue(DittoClient dittoClient, double amount) throws ExecutionException, InterruptedException {
        dittoClient.twin().startConsumption().toCompletableFuture();

        dittoClient.twin()
                .forFeature(ThingId.of("org.test:LKW-1"), "fuelTank")
                .mergeProperty("amount", getFuelTankValue(dittoClient) - amount)
                .whenComplete(((adaptable, throwable) -> {
                    if (throwable != null) {
                        System.out.println("Received error while sending MergeThing: '{}' " + throwable.getMessage());
                    } else {
                        System.out.println("Merge operation completed successfully: " + adaptable);
                    }
                }));

    }
    public void startUpdatingFuel(DittoClient dittoClient, double amount){
        Runnable updateTask = () -> {
            try {
                updateFuelTankValue(dittoClient, amount);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        scheduler.scheduleAtFixedRate(updateTask, 0, 3, TimeUnit.SECONDS);
    }
}


