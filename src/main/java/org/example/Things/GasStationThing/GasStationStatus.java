package org.example.Things.GasStationThing;

import org.example.Things.DigitalTwinStatus;

public enum GasStationStatus implements DigitalTwinStatus {
    WAITING,
    REFUELING,

    ADJUSTINGTIREPRESSURE;

    @Override
    public String status() {
        return name();
    }
}
