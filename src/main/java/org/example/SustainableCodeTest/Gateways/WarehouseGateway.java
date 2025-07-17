package org.example.SustainableCodeTest.Gateways;

import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.example.SustainableCodeTest.AbstractGateway;
import org.example.SustainableCodeTest.DigitalTwinsGateway;
import org.example.Things.TruckThing.TruckEventsActions;
import org.example.Things.WarehouseThing.Warehouse;

import java.util.concurrent.ExecutionException;

public class WarehouseGateway extends AbstractGateway<Warehouse> {

    Warehouse warehouse;
    private final TruckEventsActions truckEventsActions = new TruckEventsActions();

    public WarehouseGateway(DittoClient dittoClient, InfluxDBClient influxDBClient, Warehouse warehouse){
        super(dittoClient, influxDBClient);
        this.warehouse = warehouse;

    }
    @Override
    public void startGateway() throws ExecutionException, InterruptedException {
        startUpdating(warehouse);

    }
    @Override
    public String getWOTURL() {
        return "https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/Warehouse/WarehouseMain";
    }

    @Override
    public void updateAttributes(Warehouse thing) {
        updateAttributeValue("capacity", warehouse.getCapacity(), warehouse.getThingId());
        updateAttributeValue("status", warehouse.getStatus().toString(), warehouse.getThingId());
    }

    @Override
    public void startUpdating(Warehouse thing) throws ExecutionException, InterruptedException {
        updateAttributes(warehouse);
        updateFeatures(warehouse);
    }

    @Override
    public void updateFeatures(Warehouse thing) throws ExecutionException, InterruptedException {
        updateFeatureValue("Inventory", "amount", warehouse.getInventory(), warehouse.getThingId());
        updateFeatureValue("Workers", "amount", warehouse.getWorkers(), warehouse.getThingId());
    }

    @Override
    public void logToInfluxDB(Warehouse thing, String measurementType) {

    }

    @Override
    public void handleEvents(Warehouse thing) {

    }

    @Override
    public void handelActions(Warehouse thing) {

    }

    @Override
    public void subscribeForEventsAndActions() {

    }
}
