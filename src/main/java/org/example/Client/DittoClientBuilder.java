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
import org.eclipse.ditto.client.options.Option;
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
    Feature fuelTankFeature;

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
                .thenAccept(dittoClient -> {
                    dittoClientCompletableFuture.complete(dittoClient);
                    System.out.println("Verbunden");
                })
                .exceptionally(error -> {
                    dittoClientCompletableFuture.completeExceptionally(error);
                    System.out.println("Nicht Verbunden");
                    return null;
                }).toCompletableFuture().get();

        this.dittoClient = dittoClientCompletableFuture.get();
    }

    public DittoClient getDittoClient() {
        return dittoClient;
    }

}

