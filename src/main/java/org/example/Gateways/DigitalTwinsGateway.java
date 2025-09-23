package org.example.Gateways;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface DigitalTwinsGateway<T> {
    //Responsible for the communication between things and their digital twin
    void startGateway() throws ExecutionException, InterruptedException;
    void updateAttributes(T thing);
    void updateFeatures(T thing) throws ExecutionException, InterruptedException;

    void logToInfluxDB(T thing) throws ExecutionException, InterruptedException;
}
