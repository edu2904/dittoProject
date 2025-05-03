package org.example.Things;

import com.eclipsesource.json.Json;
import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.json.JsonFactory;
import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.json.JsonValue;
import org.eclipse.ditto.things.model.Feature;
import org.eclipse.ditto.things.model.ThingId;
import org.eclipse.ditto.things.model.ThingsModelFactory;
import org.eclipse.ditto.things.model.signals.commands.query.RetrieveThing;
import org.example.Client.DittoClientBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Scanner;
import java.util.concurrent.*;

public class LKW {

    public Feature fuelTankFeature;
    public Feature tirePressureFeature;
    public Feature velocityFeature;
    public Feature progressFeature;

    private String status;
    private int weight;

    public ThingId thingId = ThingId.of("mytest:LKW-1ijhoiuhuijohiuzftzdrztdtzrt80oz9iz");
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public LKW() {


    }

    public CompletableFuture<Boolean> createPolicyFromURL(DittoClient dittoClient, String policyURL){
        var future = new CompletableFuture<Boolean>();

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(policyURL))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(jsonString -> {
                    JsonObject policyJson = JsonFactory.readFrom(jsonString).asObject();
                    dittoClient.policies().create(policyJson)
                            .thenAccept(policy -> {
                                System.out.println("Policy created: " + policy);
                                future.complete(true);
                            })
                            .exceptionally(ex -> {
                                System.out.println("Error: " + ex.getMessage());
                                future.completeExceptionally(ex);
                                return null;
                            });
                });

        return future;
    }

    public String getPolicyFromURL(String policyURL) throws IOException {
       try(InputStream inputStream = new URL(policyURL).openStream();
           Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)){
           String jsonText = scanner.useDelimiter("\\A").next();
           JsonObject jsonObject = new JSONObject(jsonText);
       }
    }


    public CompletableFuture<Boolean> createTwinWithWOT(DittoClient client, String wotTDDefinitionURL, String policyURL) {
        var future = new CompletableFuture<Boolean>();

        createPolicyFromURL(client, policyURL);

        //String json =
        var o = JsonObject.newBuilder()
                .set("thingId", thingId.toString()) //ThingId, need to adhere to rules (entityID in Ditto)
                .set("definition", wotTDDefinitionURL) //WoT definition (in GUI WoT TD)
                //.set("policyId", thingId.toString())
                .build();

        client.twin().create(o).handle((createdThing, throwable) -> {
                    if (createdThing != null) {
                        System.out.println("Created new thing: " + createdThing);
                    } else {
                        System.out.println("Thing could not be created due to: " + throwable.getMessage());
                    }
                    return null;
                }).toCompletableFuture()
                .thenRun(() -> future.complete(true))
                .exceptionally((t) -> {
                    future.completeExceptionally(t);
                    return null;
                });
        return future;
    }

    public CompletableFuture<Boolean> policyExists(DittoClient dittoClient){

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
                .set("amount", 0)
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

    public String getFuelFeatureID(){
        return fuelTankFeature.getId();
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


    public String getVelocityFeatureID(){
        return velocityFeature.getId();
    }
    public double getVelocityAmount(){
        return getAmountFromFeature(velocityFeature);
    }

    public void setVelocityAmount(double velocityAmount) {
        JsonObject updatedProperties = velocityFeature.getProperties()
                .get().toBuilder().set("amount", velocityAmount)
                .build();
        velocityFeature = velocityFeature.toBuilder()
                .properties(updatedProperties)
                .build();
    }

    public double getProgress(){
        return getAmountFromFeature(progressFeature);
    }

    public String getProgressFeatureID(){
        return progressFeature.getId();
    }
    public void setProgressFeature(double progressAmount){
        JsonObject updatedProperties = progressFeature.getProperties()
                .get().toBuilder().set("amount", progressAmount)
                .build();
        progressFeature = progressFeature.toBuilder()
                .properties(updatedProperties)
                .build();

    }

    public String getTirePressureFeatureID(){
        return tirePressureFeature.getId();
    }
    public double getTirePressure(){
        return getAmountFromFeature(tirePressureFeature);
    }
    public void setTirePressureFeature(double tirePressureAmount){
        JsonObject updatedProperties = tirePressureFeature.getProperties()
                .get().toBuilder().set("amount", tirePressureAmount)
                .build();
        tirePressureFeature = tirePressureFeature.toBuilder()
                .properties(updatedProperties)
                .build();
    }


    public void featureSimulation(){
         double maxProgress = 100.0;

         scheduler.scheduleAtFixedRate(() -> {
             double currentFuelTank = getFuelAmount();
             double currentVelocity = getVelocityAmount();
             double currentProgress = getProgress();

             if(currentProgress > maxProgress || currentFuelTank <= 0){
                 setVelocityAmount(0);
                 System.out.println("Fahrt Beendet");
                 scheduler.shutdown();
             }

             System.out.println("fahrt läuft");
             System.out.println(currentProgress);
             System.out.println(currentFuelTank);
             setVelocityAmount(75 + Math.random() * 10);
             setFuelAmount(currentFuelTank - 0.5);
             setProgressFeature(currentProgress + 5);


         }, 0, 3, TimeUnit.SECONDS);

    }
}


