package org.example.Things.TaskThings;

import org.example.Things.DigitalTwinStatus;

public enum TaskStatus implements DigitalTwinStatus {
    STARTING,
    UNDERGOING,
    FINISHED,
    FAILED,
    PAUSED;

    @Override
    public String status() {
        return name();
    }
}
