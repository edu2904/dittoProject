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
    public Feature velocityFeature;
    public Feature progressFeature;

    private String status;
    private int weight;

    public ThingId thingId = ThingId.of("org.test:LKW-1");
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public LKW getLKW() {
        return this;
    }

    public void createLKWThing(DittoClient dittoClient) throws ExecutionException, InterruptedException {

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

        JsonObject velocityProperties = JsonObject.newBuilder()
                .set("amount", 20)
                .set("unit", "km/h")
                .build();

        velocityFeature = Feature.newBuilder()
                .properties(velocityProperties)
                .withId("velocity")
                .build();

        JsonObject progressProperties = JsonObject.newBuilder()
                .set("progess", 0)
                .set("unit", "%")
                .build();

        progressFeature = Feature.newBuilder()
                .properties(progressProperties)
                .withId("progress")
                .build();


        status = "idle";
        weight = 6000;
        dittoClient.twin().create(thingId).handle((createdThing, throwable) -> {
            if(createdThing != null){
                System.out.println("Created new thing: " + createdThing);
            }else {
                System.out.println("Failed to create thing or thing already created");
            }
            return new CompletionStage[]{
                    dittoClient.twin().forId(thingId).putAttribute("first-updated-at", OffsetDateTime.now().toString()),
                    dittoClient.twin().forId(thingId).putAttribute("status", status),
                    dittoClient.twin().forId(thingId).putAttribute("weight", weight),
                    dittoClient.twin().forId(thingId).putFeature(tirePressureFeature),
                    dittoClient.twin().forId(thingId).putFeature(fuelTankFeature)
            };

        }).toCompletableFuture().get();

    }

    public ThingId getThingId(){
        return thingId;
    }
    public String getStatus(){
        return status;
    }
    public void setStatus(String status){
        this.status = status;
    }
    public int getWeight(){
        return weight;
    }
    public void setWeight(int weight){
        this.weight = weight;
    }

    public double getAmountFromFeature(Feature feature){
        return feature.getProperties().flatMap(properties -> properties.getValue("amount")).map(JsonValue::asDouble).orElse(0.0);
    }

    public double getFuelAmount(){
        return getAmountFromFeature(fuelTankFeature);
    }
    public void setFuelAmount(double fuelAmount){
        JsonObject updatedProperties = fuelTankFeature.getProperties()
                .get().toBuilder().set("amount", fuelAmount)
                .build();
        fuelTankFeature = fuelTankFeature.toBuilder()
                .properties(updatedProperties)
                .build();
    }


    public double getVelocityAmount(){
        return getAmountFromFeature(velocityFeature);
    }

    public void setVelocityAmount(double velocityAmount) {
        JsonObject updatedProperties = fuelTankFeature.getProperties()
                .get().toBuilder().set("amount", velocityAmount)
                .build();
        fuelTankFeature = fuelTankFeature.toBuilder()
                .properties(updatedProperties)
                .build();
    }

    public double getProgress(){
        return getAmountFromFeature(progressFeature);
    }

    public void setProgressFeature(double progressAmount){
        JsonObject updatedProperties = fuelTankFeature.getProperties()
                .get().toBuilder().set("amount", progressAmount)
                .build();
        fuelTankFeature = fuelTankFeature.toBuilder()
                .properties(updatedProperties)
                .build();

    }

    public double getTirePressure(){
        return getAmountFromFeature(tirePressureFeature);
    }
    public void setTirePressureFeature(double tirePressureAmount){
        JsonObject updatedProperties = fuelTankFeature.getProperties()
                .get().toBuilder().set("amount", tirePressureAmount)
                .build();
        fuelTankFeature = fuelTankFeature.toBuilder()
                .properties(updatedProperties)
                .build();
    }


    public void featureSimulation(){


    }
}


