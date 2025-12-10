package org.example.util;

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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;

public class ThingHandler {
    private final Logger logger = LoggerFactory.getLogger(ThingHandler.class);
    public ThingHandler() {


    }

    //create a policy file.
    //This function creates a policy file from a defined policy JSON
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

    // retrieve the policy to check whether it is created. This prevents multiple policy creation, as is would cause an error.
    // Eclipse Ditto does not allow two of the same policies to be present
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

    //check if the policy was already created in Eclipse Ditto
    public CompletableFuture<Boolean> policyExists(DittoClient dittoClient, String policy) {
        return dittoClient
                .policies()
                .retrieve(PolicyId.of(policy))
                .thenApply(pol -> true)
                .exceptionally(ex -> false).toCompletableFuture();
    }

    // This code deletes a created policy
    public void deletePolicy(DittoClient dittoClient, String policyID) {
        dittoClient.policies().delete(PolicyId.of(policyID)).thenAccept(thing -> {
            logger.info("Deleted policy: {}", policyID);
        }).exceptionally(ex -> {
            logger.error("Error deleting policy {}", ex.getMessage());
            return null;
        }).toCompletableFuture();
    }


    // This code created a Thing with its specified policy by providing of its WoT description
    public CompletableFuture<Boolean> createTwinWithWOTAndPolicy(DittoClient client, String wotTDDefinitionURL, String policyURL, String thingId) throws ExecutionException, InterruptedException {
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
    // This code created a thing without a predefined policy file.
    // In this case Eclipse Ditto will automatically generate a policy for the twin.
    public CompletableFuture<Boolean> createTwinWithWOT(DittoClient client, String wotTDDefinitionURL, String thingId) throws ExecutionException, InterruptedException {
        var future = new CompletableFuture<Boolean>();

        ThingDefinition thingDefinition = ThingsModelFactory.newDefinition(wotTDDefinitionURL);
        Thing thing = ThingsModelFactory.newThingBuilder()
                .setId(ThingId.of(thingId)).setDefinition(thingDefinition).build();

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

    public CompletableFuture<Boolean> createTwin(DittoClient dittoClient, String thingURL, String thingId) throws ExecutionException, InterruptedException {
        if(thingExists(dittoClient, thingId).get()){
            deleteThing(dittoClient, thingId);
        }
        return createTwinWithWOT(dittoClient, thingURL, thingId);
    }

    public CompletableFuture<Boolean> createTwinAndPolicy(DittoClient dittoClient, String thingURL, String policyURL, String thingId) throws ExecutionException, InterruptedException {

        // check if policy already exists
        if (!policyExists(dittoClient, getPolicyFromURL(policyURL).get()).get()) {
            CompletableFuture<Boolean> future = createPolicyFromURL(dittoClient, policyURL);
            future.get();
        }
        //don't create two of the same things
        if(thingExists(dittoClient, thingId).get()){
            deleteThing(dittoClient, thingId);
        }
        return createTwinWithWOTAndPolicy(dittoClient, thingURL, policyURL, thingId);
    }


    //check if a thing was already created and exists in ditto
    public CompletableFuture<Boolean> thingExists(DittoClient dittoClient, String thingID) {
        return dittoClient
                .twin()
                .retrieve(ThingId.of(thingID))
                .thenApply(pol -> !pol.isEmpty())
                .exceptionally(ex -> false).toCompletableFuture();
    }

    // deletes a thing from ditto
    public void deleteThing(DittoClient dittoClient, String thingID) {
        dittoClient.twin().delete(ThingId.of(thingID)).thenAccept(thing -> {
            logger.info("Deleted thing: {}", thingID);
        }).exceptionally(ex -> {
            logger.error("Error deleting thing: {}", ex.getMessage());
            return null;
        }).toCompletableFuture();
    }


    // searches for all the things that are detected from the filter string and returns them as a List.
    // However, it does not contain the specific information of the thing. It only returns the twins JSON file.
    // A specified mapper has to be implemented to assign all the needed value inside the JSON. These mappers are defined in the "Mapper" file.
    public  <T> List<T> searchThings(DittoClient dittoClient, Function<Thing, T> mapper, String filter){
        List<T> foundThings = new ArrayList<>();
        dittoClient.twin().search()
                .stream(queryBuilder -> queryBuilder.filter("like(thingId,\"" + filter + ":*\" )")
                        .options(o -> o.sort(s -> s.desc("thingId")).size(200))
                        .fields("attributes"))
                .sequential()
                .map(mapper).toList()
                .forEach(foundThing -> {
                    logger.info("Found thing: {}", foundThing);
                    foundThings.add(foundThing);
                });
        return foundThings;
    }

    // deletes the thing with its corresponding policy
    public void deleteThingAndPolicy(DittoClient dittoClient, String policyID, String thingId) {
        deleteThing(dittoClient, thingId);
        deletePolicy(dittoClient, policyID);
    }

    //returns the Json of a specific thing-
    public CompletableFuture<Void> getThingPayload(DittoClient dittoClient, String thingId) {
        return dittoClient.twin().retrieve(ThingId.of(thingId)).thenAccept(thing -> {
            logger.info("Payload: {}", thing.toString());
        }).exceptionally(ex -> {
            logger.error("Error retrieving payload {}", ex.getMessage());
            return null;
        }).toCompletableFuture();
    }
}