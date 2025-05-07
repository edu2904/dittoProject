package org.example;

import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.json.JsonFactory;
import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.json.JsonValue;
import org.eclipse.ditto.policies.model.PolicyId;
import org.eclipse.ditto.things.model.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.concurrent.*;

public class ThingHandler {

    public Feature fuelTankFeature;
    public Feature tirePressureFeature;
    public Feature velocityFeature;
    public Feature progressFeature;

    private String status;
    private int weight;

    public ThingId thingId = ThingId.of("mytest:LKW-1");
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public ThingHandler() {


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

    public CompletableFuture<String> getPolicyFromURL(String policyURL) {
        return CompletableFuture.supplyAsync(() ->{
            URL url = null;
            try {
                url = new URL(policyURL);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            try (InputStream input = url.openStream()) {
            InputStreamReader isr = new InputStreamReader(input);
            BufferedReader reader = new BufferedReader(isr);
            StringBuilder json = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                json.append((char) c);
            }
            String search = "\"policyId\"";
            int startIndex = json.indexOf(search);
            if(startIndex == -1){
                System.out.println("keine Policy gefunden");
            }
            int cIndex = json.indexOf(":", startIndex);
            int start = json.indexOf("\"", cIndex + 1);
            int end = json.indexOf("\"", start + 1);

            return json.substring(start + 1, end);
        } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Boolean> policyExists(DittoClient dittoClient, String policy){
        return dittoClient
                .policies()
                .retrieve(PolicyId.of(policy))
                .thenApply(pol -> true)
                .exceptionally(ex -> false).toCompletableFuture();
    }


    public CompletableFuture<Boolean> createTwinWithWOT(DittoClient client, String wotTDDefinitionURL, String policyURL) throws ExecutionException, InterruptedException {


        String policy = getPolicyFromURL(policyURL).get();
        var future = new CompletableFuture<Boolean>();



        //String json =
      //  var o = JsonObject.newBuilder()
       //         .set("thingId", thingId.toString()) //ThingId, need to adhere to rules (entityID in Ditto)
       //         .set("definition", wotTDDefinitionURL) //WoT definition (in GUI WoT TD)
       //         .set("policyId", policy)
       //         .build();

        ThingDefinition thingDefinition = ThingsModelFactory.newDefinition(wotTDDefinitionURL);
        Thing thing = ThingsModelFactory.newThingBuilder()
                .setId(thingId).setDefinition(thingDefinition).setPolicyId(PolicyId.of(policy)).build();

        client.twin().create(thing).handle((createdThing, throwable) -> {
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

    public CompletableFuture<Boolean> createTwinAndPolicy(DittoClient dittoClient, String thingURL, String policyURL){
        return createPolicyFromURL(dittoClient, policyURL).thenCompose(x -> {
            try {
                return createTwinWithWOT(dittoClient, thingURL, policyURL);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Void> deleteThing(DittoClient dittoClient, String thingID){
        return  dittoClient.twin().delete(ThingId.of(thingID)).thenAccept(thing -> {
            System.out.println("Deleted thing: " + thingID);
        }).exceptionally(ex -> {
            System.out.println("Error deleting thing" + ex.getMessage());
            return null;
        }).toCompletableFuture();
    }
    public CompletableFuture<Void> deletePolicy(DittoClient dittoClient, String policyID){
        return  dittoClient.policies().delete(PolicyId.of(policyID)).thenAccept(thing -> {
            System.out.println("Deleted policy: " + policyID);
        }).exceptionally(ex -> {
            System.out.println("Error deleting policy " + ex.getMessage());
            return null;
        }).toCompletableFuture();
    }

    public CompletableFuture<Void> deleteThingandPolicy(DittoClient dittoClient, String thingID, String policyID){
        deleteThing(dittoClient, thingID);
        deletePolicy(dittoClient,policyID);
        return null;
    }

 /*   public CompletableFuture<Boolean> updateTwinDefinition(DittoClient dittoClient, String thingWOTURL, String policy){
        var o = JsonObject.newBuilder()
                .set("thingId", thingId.toString())
                .set("definition", thingWOTURL)
                .set("policyId", policy)
                .build();
        JsonObject feature = JsonObject.newBuilder().set("definition", "https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/LKW/FuelTank").build();
        ThingDefinition thingDefinition = ThingsModelFactory.newDefinition(thingWOTURL);
        Thing thing = ThingsModelFactory.newThingBuilder()
                .setId(thingId).setFeature("FuelTank").setDefinition(thingDefinition).setPolicyId(PolicyId.of(policy)).build();
        var future = new CompletableFuture<Boolean>();
        var newDef = JsonObject.newBuilder().set("definition", thingWOTURL).build();



        dittoClient.twin().put(o).whenComplete((createdThing, throwable) -> {
                    if (createdThing.isPresent()) {
                        System.out.println("Created new thing: " + createdThing);
                    } else {
                        System.out.println("Thing could not be created due to: " + throwable.getMessage());
                    }

        }).toCompletableFuture()
                .thenRun(() -> future.complete(true))
                .exceptionally((t) -> {
                    future.completeExceptionally(t);
                    return null;
                });


        dittoClient.twin().merge(thingId, thing).whenComplete((adaptable, throwable) -> {
                    if (throwable != null) {
                        System.out.println("Received error while sending MergeThing: '{}' " +  throwable);
                    } else {
                        System.out.println("Received response for MergeThing: '{}' " + adaptable);
                    }
                }).toCompletableFuture()
                .thenRun(() -> future.complete(true))
                .exceptionally((t) -> {
                    future.completeExceptionally(t);
                    return null;
                });

        return future;

    }
*/
    public CompletableFuture<Void> getThingPayload(DittoClient dittoClient){
        return  dittoClient.twin().retrieve(thingId).thenAccept(thing -> {
            System.out.println("Payload: " + thing.toString());
        }).exceptionally(ex -> {
            System.out.println("Error retrieving payload" + ex.getMessage());
            return null;
        }).toCompletableFuture();

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


