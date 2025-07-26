package org.example.Client;
import org.eclipse.ditto.client.DisconnectedDittoClient;
import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.client.configuration.BasicAuthenticationConfiguration;
import org.eclipse.ditto.client.DittoClients;
import org.eclipse.ditto.client.configuration.WebSocketMessagingConfiguration;
import org.eclipse.ditto.client.messaging.AuthenticationProviders;
import org.eclipse.ditto.client.messaging.MessagingProvider;
import org.eclipse.ditto.client.messaging.MessagingProviders;
import org.example.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class DittoClientBuilder {
    private final Logger logger = LoggerFactory.getLogger(DittoClientBuilder.class);


    public DittoClient dittoClient;

    public DittoClientBuilder() throws ExecutionException, InterruptedException {
        var authentication = AuthenticationProviders.basic(BasicAuthenticationConfiguration
                .newBuilder()
                .username(Config.DITTO_USERNAME)
                .password(Config.DITTO_PASSWORD)
                .build());

        MessagingProvider messagingProvider = MessagingProviders.webSocket(
                WebSocketMessagingConfiguration
                        .newBuilder()
                        .endpoint(Config.WEBSOCKET_ENDPOINT)
                        .build(), authentication);

        DisconnectedDittoClient disconnectedDittoClient = DittoClients.newInstance(messagingProvider);

        CompletableFuture<DittoClient> dittoClientCompletableFuture = new CompletableFuture<>();

        disconnectedDittoClient.connect()
                .thenAccept(dittoClient -> {
                    dittoClientCompletableFuture.complete(dittoClient);
                    logger.info("DittoClient connected");
                })
                .exceptionally(error -> {
                    dittoClientCompletableFuture.completeExceptionally(error);
                    logger.error("Error connecting to DittoClient");
                    return null;
                }).toCompletableFuture().get();

        this.dittoClient = dittoClientCompletableFuture.get();
    }

    public DittoClient getDittoClient() {
        return dittoClient;
    }

}

