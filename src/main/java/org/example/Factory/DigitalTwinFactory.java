package org.example.Factory;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface DigitalTwinFactory<T> {

    //Create things for Ditto
    void createTwinsForDitto() throws ExecutionException, InterruptedException;

    String getWOTURL();

    String getPolicyURL();

    // Initialize Things by creating them and setting starter values

    void initializeThings() throws InterruptedException;

    List<T> getThings();
}
