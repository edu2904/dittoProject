package org.example.SustainableCodeTest.Factory;

import org.eclipse.ditto.client.DittoClient;
import org.example.SustainableCodeTest.Factory.Things.GasStationFactory;
import org.example.SustainableCodeTest.Factory.Things.TaskFactory;
import org.example.SustainableCodeTest.Factory.Things.TruckFactory;
import org.example.SustainableCodeTest.Factory.Things.WarehouseFactory;
import org.example.Things.GasStationThing.GasStation;
import org.example.Things.TruckThing.Truck;
import org.example.Things.WarehouseThing.Warehouse;

public class DigitalTwinFactoryMain {
    private final DigitalTwinFactory<Truck> truckFactory;
    private final DigitalTwinFactory<GasStation> gasStationFactory;

    private final DigitalTwinFactory<Warehouse> warehouseFactory;


    public DigitalTwinFactoryMain(DittoClient dittoClient){
        gasStationFactory = new GasStationFactory(dittoClient);
        truckFactory = new TruckFactory(dittoClient);
        warehouseFactory = new WarehouseFactory(dittoClient);
    }

    public DigitalTwinFactory<GasStation> getGasStationFactory() {
        return gasStationFactory;
    }
    public DigitalTwinFactory<Truck> getTruckFactory() {
        return truckFactory;
    }

    public DigitalTwinFactory<Warehouse> getWarehouseFactory() {
        return warehouseFactory;
    }
}
