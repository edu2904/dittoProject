package org.example.Client;
import org.eclipse.ditto.client.DisconnectedDittoClient;
import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.client.configuration.BasicAuthenticationConfiguration;
import org.eclipse.ditto.client.DittoClients;
import org.eclipse.ditto.client.configuration.WebSocketMessagingConfiguration;
import org.eclipse.ditto.client.messaging.AuthenticationProviders;
import org.eclipse.ditto.client.messaging.MessagingProvider;
import org.eclipse.ditto.client.messaging.MessagingProviders;

import java.util.List;
import java.util.concurrent.*;

public class DittoClientBuilder {

    public DittoClient dittoClient;

    public DittoClientBuilder() throws ExecutionException, InterruptedException {
        List<String> endpoints = List.of("ws://localhost:8080");
        var authentication = AuthenticationProviders.basic(BasicAuthenticationConfiguration
                .newBuilder()
                .username("ditto")
                .password("ditto")
                .build());

        MessagingProvider messagingProvider = MessagingProviders.webSocket(
                WebSocketMessagingConfiguration
                        .newBuilder()
                        .endpoint("ws://localhost:8080/ws/2")
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

