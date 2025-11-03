package org.example.Things.TruckThing;

import org.example.Things.DigitalTwinStatus;

public enum TruckStatus implements DigitalTwinStatus {
    IDLE,
    WAITING,
    DRIVING,
    ADJUSTINGTIREPRESSURE,
    LOADING,
    UNLOADING,
    REFUELING,
    DISABLED;

    @Override
    public String status() {
        return name();
    }
}
