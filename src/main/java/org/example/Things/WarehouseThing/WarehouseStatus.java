package org.example.Things.WarehouseThing;

import org.example.Things.DigitalTwinStatus;

public enum WarehouseStatus implements DigitalTwinStatus {
    WAITING,
    LOADING;

    @Override
    public String status() {
        return name();
    }
}
