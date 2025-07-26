package org.example.Factory;

import org.eclipse.ditto.client.DittoClient;
import org.example.Factory.ConcreteFactories.GasStationFactory;
import org.example.Factory.ConcreteFactories.TruckFactory;
import org.example.Factory.ConcreteFactories.WarehouseFactory;
import org.example.ThingHandler;
import org.example.Things.GasStationThing.GasStation;
import org.example.Things.TruckThing.Truck;
import org.example.Things.WarehouseThing.Warehouse;

public class DigitalTwinFactoryMain {
    private final DigitalTwinFactory<Truck> truckFactory;
    private final DigitalTwinFactory<GasStation> gasStationFactory;

    private final DigitalTwinFactory<Warehouse> warehouseFactory;


    public DigitalTwinFactoryMain(DittoClient dittoClient){
        ThingHandler thingHandler = new ThingHandler();
        gasStationFactory = new GasStationFactory(dittoClient, thingHandler);
        truckFactory = new TruckFactory(dittoClient, thingHandler);
        warehouseFactory = new WarehouseFactory(dittoClient, thingHandler);
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
