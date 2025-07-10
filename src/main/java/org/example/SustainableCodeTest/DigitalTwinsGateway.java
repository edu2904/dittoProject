package org.example.SustainableCodeTest;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface DigitalTwinsGateway<T> {



    //Responsible for the communication between things and their digital twin
    void startGateway() throws ExecutionException, InterruptedException;

    String getWOTURL();
    void updateAttributes(T thing);

    void startUpdating(T thing) throws ExecutionException, InterruptedException;
    void updateFeatures(T thing) throws ExecutionException, InterruptedException;

    void logToInfluxDB(T thing, String measurementType);
    void handleEvents(T thing);
    void handelActions(T thing);
    void subscribeForEventsAndActions();
}
