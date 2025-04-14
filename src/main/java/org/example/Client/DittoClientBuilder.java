package org.example.Client;
import com.eclipsesource.json.Json;
import org.eclipse.ditto.client.DisconnectedDittoClient;
import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.client.configuration.BasicAuthenticationConfiguration;
import org.eclipse.ditto.client.configuration.ClientCredentialsAuthenticationConfiguration;
import org.eclipse.ditto.client.configuration.MessagingConfiguration;
import org.eclipse.ditto.client.DittoClients;
import org.eclipse.ditto.client.configuration.WebSocketMessagingConfiguration;
import org.eclipse.ditto.client.messaging.AuthenticationProviders;
import org.eclipse.ditto.client.messaging.MessagingProvider;
import org.eclipse.ditto.client.messaging.MessagingProviders;
import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.json.JsonValue;
import org.eclipse.ditto.things.model.*;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class DittoClientBuilder {

    public DittoClient dittoClient;

    public DittoClientBuilder() throws ExecutionException, InterruptedException {
        var authentication = AuthenticationProviders.basic(BasicAuthenticationConfiguration
                .newBuilder()
                .username("ditto")
                .password("ditto")
                .build());

        MessagingProvider messagingProvider = MessagingProviders.webSocket(
                WebSocketMessagingConfiguration
                        .newBuilder()
                        .endpoint("ws://localhost:8080")
                        .build(), authentication);

        DisconnectedDittoClient disconnectedDittoClient = DittoClients.newInstance(messagingProvider);

        CompletableFuture<DittoClient> dittoClientCompletableFuture = new CompletableFuture<>();

        disconnectedDittoClient.connect()
                .thenAccept(dittoClient -> {dittoClientCompletableFuture.complete(dittoClient);
                    System.out.println("Verbunden");})
                .exceptionally(error -> {
                    dittoClientCompletableFuture.completeExceptionally(error);
                    System.out.println("Nicht Verbunden");
                    return null;
                }).toCompletableFuture().get();

        this.dittoClient = dittoClientCompletableFuture.get();
    }


    public void createFirstThing() throws ExecutionException, InterruptedException {
       // var future = new CompletableFuture<Boolean>();


        ThingId thingId = ThingId.of("org.test:LKW-1");
        ThingsModelFactory.newThingBuilder()
                .setId(thingId)
                .build();


        JsonObject tirePressureProperties = JsonObject.newBuilder()
                .set("amount", 800)
                .set("unit", "kPa")
                .build();

        Feature tirePressureFeature = Feature.newBuilder()
                .properties(tirePressureProperties)
                .withId("tirePressure").build();

        JsonObject fuelTankProperties = JsonObject.newBuilder()
                .set("amount", 100.5)
                .set("unit", "L")
                .build();

        Feature fuelTankFeature = Feature.newBuilder()
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


    public double getFuelTankValue() throws InterruptedException, ExecutionException {
        CompletableFuture<Double> fuelAmount = new CompletableFuture<>();

        dittoClient.twin().forId(ThingId.of("org.test:LKW-1"))
                    .retrieve()
                    .thenCompose(thing -> {
                        JsonValue feature = thing.getFeatures().
                                flatMap(features -> features.getFeature("fuelTank")).
                                flatMap(Feature::getProperties).
                                flatMap(fuelTank -> fuelTank.getValue("amount"))
                                .get();

                        fuelAmount.complete(feature.asDouble());
                        return CompletableFuture.completedFuture(null);
                    });


        System.out.println(fuelAmount.get());
        return fuelAmount.get();
        }

    }



