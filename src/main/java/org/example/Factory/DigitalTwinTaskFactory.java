package org.example.Factory;

import org.example.Things.TaskThings.TaskType;
import org.example.Things.TaskThings.Task;

import java.util.concurrent.ExecutionException;

public interface DigitalTwinTaskFactory {
    void createTwinsForDitto() throws ExecutionException, InterruptedException;

    String getWOTURL();

    String getPolicyURL();

    // Initialize Things by creating them and setting starter values

    void initializeTask() throws InterruptedException;

    Task getTask();
}
