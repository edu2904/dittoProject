package org.example;

import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.json.JsonFactory;
import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.policies.model.PolicyId;
import org.eclipse.ditto.things.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.*;

public class ThingHandler {
    private final Logger logger = LoggerFactory.getLogger(ThingHandler.class);


    public Feature fuelTankFeature;
    public Feature tirePressureFeature;
    public Feature velocityFeature;
    public Feature progressFeature;

    private String status;
    private int weight;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public ThingHandler() {


    }

    public CompletableFuture<Boolean> createPolicyFromURL(DittoClient dittoClient, String policyURL) {
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
        return CompletableFuture.supplyAsync(() -> {
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
                if (startIndex == -1) {
                    logger.error("No policy found");
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

    public CompletableFuture<Boolean> policyExists(DittoClient dittoClient, String policy) {
        return dittoClient
                .policies()
                .retrieve(PolicyId.of(policy))
                .thenApply(pol -> true)
                .exceptionally(ex -> false).toCompletableFuture();
    }
    public CompletableFuture<Void> deletePolicy(DittoClient dittoClient, String policyID) {
        return dittoClient.policies().delete(PolicyId.of(policyID)).thenAccept(thing -> {
            logger.info("Deleted policy: {}", policyID);
        }).exceptionally(ex -> {
            logger.error("Error deleting policy {}", ex.getMessage());
            return null;
        }).toCompletableFuture();
    }


    public CompletableFuture<Boolean> createTwinWithWOT(DittoClient client, String wotTDDefinitionURL, String policyURL, String thingId) throws ExecutionException, InterruptedException {


        String policy = getPolicyFromURL(policyURL).get();
        var future = new CompletableFuture<Boolean>();

        ThingDefinition thingDefinition = ThingsModelFactory.newDefinition(wotTDDefinitionURL);
        Thing thing = ThingsModelFactory.newThingBuilder()
                .setId(ThingId.of(thingId)).setDefinition(thingDefinition).setPolicyId(PolicyId.of(policy)).build();

        client.twin().create(thing).handle((createdThing, throwable) -> {
                    if (createdThing != null) {
                        logger.info("Created new thing: {}", createdThing);
                    } else {
                        logger.error("Thing could not be created due to: {}" , throwable.getMessage());
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

    public CompletableFuture<Boolean> createTwinAndPolicy(DittoClient dittoClient, String thingURL, String policyURL, String thingId) throws ExecutionException, InterruptedException {
        if (!policyExists(dittoClient, getPolicyFromURL(policyURL).get()).get()) {
           CompletableFuture<Boolean> future = createPolicyFromURL(dittoClient, policyURL);
           future.get();
        }
        if(thingExists(dittoClient, thingId).get()){
            deleteThing(dittoClient, thingId);
        }

        return createTwinWithWOT(dittoClient, thingURL, policyURL, thingId);
    }


    public CompletableFuture<Boolean> thingExists(DittoClient dittoClient, String thingID) {
        return dittoClient
                .twin()
                .retrieve(ThingId.of(thingID))
                .thenApply(pol -> {
                    if(pol.isEmpty()) {
                        return false;
                    }
                    else {
                        return true;
                    }
                })
                .exceptionally(ex -> false).toCompletableFuture();
    }


    public CompletableFuture<Void> deleteThing(DittoClient dittoClient, String thingID) {
        return dittoClient.twin().delete(ThingId.of(thingID)).thenAccept(thing -> {
            logger.info("Deleted thing: {}" , thingID);
        }).exceptionally(ex -> {
            logger.error("Error deleting thing: {}", ex.getMessage());
            return null;
        }).toCompletableFuture();
    }


    public CompletableFuture<Void> deleteThingandPolicy(DittoClient dittoClient, String policyID, String thingId) {
        deleteThing(dittoClient, thingId);
        deletePolicy(dittoClient, policyID);
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
    public CompletableFuture<Void> getThingPayload(DittoClient dittoClient, String thingId) {
        return dittoClient.twin().retrieve(ThingId.of(thingId)).thenAccept(thing -> {
            logger.info("Payload: {}", thing.toString());
        }).exceptionally(ex -> {
            logger.error("Error retrieving payload {}", ex.getMessage());
            return null;
        }).toCompletableFuture();

    }


}