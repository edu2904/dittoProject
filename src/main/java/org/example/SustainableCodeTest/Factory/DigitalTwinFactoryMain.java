package org.example.SustainableCodeTest.Factory;

import org.eclipse.ditto.client.DittoClient;
import org.example.SustainableCodeTest.Factory.Things.GasStationFactory;
import org.example.SustainableCodeTest.Factory.Things.TaskFactory;
import org.example.SustainableCodeTest.Factory.Things.TruckFactory;
import org.example.Things.GasStationThing.GasStation;
import org.example.Things.TruckThing.Truck;

public class DigitalTwinFactoryMain {
    private final DigitalTwinFactory<Truck> truckFactory;
    private final DigitalTwinFactory<GasStation> gasStationFactory;


    public DigitalTwinFactoryMain(DittoClient dittoClient){
        gasStationFactory = new GasStationFactory(dittoClient);
        truckFactory = new TruckFactory(dittoClient);
    }

    public DigitalTwinFactory<GasStation> getGasStationFactory() {
        return gasStationFactory;
    }

    public DigitalTwinFactory<Truck> getTruckFactory() {
        return truckFactory;
    }
}
