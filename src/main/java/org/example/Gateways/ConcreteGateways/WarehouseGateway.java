package org.example.Gateways.ConcreteGateways;

import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.example.Gateways.AbstractGateway;
import org.example.Things.GasStationThing.GasStation;
import org.example.Things.TruckThing.TruckEventsActions;
import org.example.Things.WarehouseThing.Warehouse;
import org.example.util.GeoConst;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class WarehouseGateway extends AbstractGateway<Warehouse> {

    List<Warehouse> warehouseList;

    public WarehouseGateway(DittoClient dittoClient, InfluxDBClient influxDBClient, List<Warehouse> warehouseList){
        super(dittoClient, influxDBClient);
        this.warehouseList = warehouseList;

    }
    @Override
    public void startGateway() throws ExecutionException, InterruptedException {
        for(Warehouse warehouse : warehouseList) {
            startUpdating(warehouse);
        }

    }
    @Override
    public String getWOTURL() {
        return "https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/Warehouse/WarehouseMain";
    }

    @Override
    public void updateAttributes(Warehouse warehouse) {
        updateAttributeValue("capacity", warehouse.getCapacity(), warehouse.getThingId());
        updateAttributeValue("status", warehouse.getStatus().toString(), warehouse.getThingId());
        updateAttributeValue("location/geo:lat", warehouse.getLocation().getLat(), warehouse.getThingId());
        updateAttributeValue("location/geo:long", warehouse.getLocation().getLon(), warehouse.getThingId());
        updateAttributeValue("utilization", warehouse.getUtilization(), warehouse.getThingId());
    }

    @Override
    public void startUpdating(Warehouse warehouse) throws ExecutionException, InterruptedException {
        updateAttributes(warehouse);
        updateFeatures(warehouse);
    }

    @Override
    public void updateFeatures(Warehouse warehouse) throws ExecutionException, InterruptedException {
        updateFeatureValue("Inventory", "amount", warehouse.getInventory(), warehouse.getThingId());
        updateFeatureValue("Workers", "amount", warehouse.getWorkers(), warehouse.getThingId());
    }

    @Override
    public void logToInfluxDB(Warehouse thing, String measurementType) {

    }


    public double getUtilizationFromDitto(Warehouse warehouse) throws ExecutionException, InterruptedException {
        return (double) getAttributeValueFromDitto("utilization", warehouse.getThingId());
    }
}
