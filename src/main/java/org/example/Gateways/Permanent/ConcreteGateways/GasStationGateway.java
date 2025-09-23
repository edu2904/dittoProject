package org.example.Gateways.Permanent.ConcreteGateways;

import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.example.Gateways.AbstractGateway;
import org.example.Things.GasStationThing.GasStation;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class GasStationGateway extends AbstractGateway<GasStation> {

    List<GasStation> gasStations;

    public GasStationGateway(DittoClient dittoClient, DittoClient listenerClient, InfluxDBClient influxDBClient, List<GasStation> gasStations) {
        super(dittoClient, listenerClient, influxDBClient);
        this.gasStations = gasStations;
    }


    @Override
    public void startGateway() throws ExecutionException, InterruptedException {
        gasStations.forEach(this::upDateThing);
    }

    @Override
    public void updateAttributes(GasStation gasStation) {
        var attributes = new HashMap<>(Map.<String, Object>of(
                "status", gasStation.getGasStationStatus().toString(),
                "location/geo:lat", gasStation.getLocation().getLat(),
                "location/geo:long", gasStation.getLocation().getLon(),
                "utilization", gasStation.getUtilization()
        ));
        attributes.forEach((attributeName, attributeValue) ->
                updateAttributeValue(attributeName, attributeValue, gasStation.getThingId())
        );

    }

    @Override
    public void updateFeatures(GasStation gasStation) {
        var features = new HashMap<String, Map<String, Object>>(Map.of(
                "GasStationFuel", Map.of("amount", gasStation.getGasStationFuelAmount())
        ));

        features.forEach((featureName, prop) ->
                prop.forEach((propName, value) ->
                        updateFeatureValue(featureName, propName, value, gasStation.getThingId())
                )
        );


    }

    @Override
    public void logToInfluxDB(GasStation gasStation) {
        String measurementType = "GasStation";
        try {
            var loggingValues = new HashMap<>(Map.of(
                    "Utilization", getUtilizationFromDitto(gasStation),
                    "Inventory", getGasStationFuelFromDitto(gasStation)
            ));
            loggingValues.forEach((influxName, value) ->
                    startLoggingToInfluxDB(measurementType, gasStation.getThingId(), influxName, value)
            );

        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public double getUtilizationFromDitto(GasStation gasStation) throws ExecutionException, InterruptedException {
        return (double) getAttributeValueFromDitto("utilization", gasStation.getThingId());
    }

    public double getGasStationFuelFromDitto(GasStation gasStation) throws ExecutionException, InterruptedException {
        return (double) getFeatureValueFromDitto("GasStationFuel", "amount", gasStation.getThingId());
    }

}