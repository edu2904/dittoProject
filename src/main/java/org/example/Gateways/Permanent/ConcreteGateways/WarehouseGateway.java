package org.example.Gateways.Permanent.ConcreteGateways;

import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.example.Gateways.AbstractGateway;
import org.example.Things.WarehouseThing.Warehouse;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class WarehouseGateway extends AbstractGateway<Warehouse> {

    List<Warehouse> warehouseList;

    public WarehouseGateway(DittoClient dittoClient, DittoClient listenerClient, InfluxDBClient influxDBClient, List<Warehouse> warehouseList){
        super(dittoClient, listenerClient, influxDBClient);
        this.warehouseList = warehouseList;

    }
    @Override
    public void startGateway() throws ExecutionException, InterruptedException {
        warehouseList.forEach(this::upDateThing);

    }
    @Override
    public void updateAttributes(Warehouse warehouse) {
        var attributes = new HashMap<>(Map.<String, Object>of(
                "capacity", warehouse.getCapacity(),
                "status", warehouse.getStatus().toString(),
                "location/geo:lat", warehouse.getLocation().getLat(),
                "location/geo:long", warehouse.getLocation().getLon(),
                "utilization", warehouse.getUtilization()
        ));
        attributes.forEach((attributeName, attributeValue) ->
                updateAttributeValue(attributeName, attributeValue, warehouse.getThingId())
        );
    }

    @Override
    public void updateFeatures(Warehouse warehouse)  {
        var features = new HashMap<String, Map<String, Object>>(Map.of(
                "Inventory", Map.of("amount", warehouse.getInventory()),
                "Workers", Map.of("amount", warehouse.getWorkers())
              ));
        features.forEach((featureName, prop) ->
                prop.forEach((propName, value) ->
                        updateFeatureValue(featureName, propName, value, warehouse.getThingId())
                )
        );
    }

    @Override
    public void logToInfluxDB(Warehouse thing, String measurementType) {

    }


    public double getUtilizationFromDitto(Warehouse warehouse) throws ExecutionException, InterruptedException {
        return (double) getAttributeValueFromDitto("utilization", warehouse.getThingId());
    }
}
