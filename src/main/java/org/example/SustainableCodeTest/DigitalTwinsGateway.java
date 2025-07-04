package org.example.SustainableCodeTest;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface DigitalTwinsGateway<T> {

    void startGateway() throws ExecutionException, InterruptedException;

    String getWOTURL();
    void updateAttributes(T thing);

    void startUpdating(T thing) throws ExecutionException, InterruptedException;
    void updateFeatures(T thing) throws ExecutionException, InterruptedException;

    void logToInfluxDB(T thing);
    void handleEvents(T thing);
    void handelActions(T thing);
    void subscribeForEventsAndActions();
}
