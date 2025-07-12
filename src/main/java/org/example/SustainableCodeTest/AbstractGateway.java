package org.example.SustainableCodeTest;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.exceptions.InfluxException;
import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.json.JsonValue;
import org.eclipse.ditto.things.model.Feature;
import org.eclipse.ditto.things.model.ThingId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public abstract class AbstractGateway<T> implements DigitalTwinsGateway<T> {

    protected final DittoClient dittoClient;
    protected InfluxDBClient influxDBClient;

    protected final Logger logger = LoggerFactory.getLogger(AbstractGateway.class);


    public AbstractGateway(DittoClient dittoClient, InfluxDBClient influxDBClient){
        this.dittoClient = dittoClient;
        this.influxDBClient = influxDBClient;

    }


    public void updateAttributeValue(String attributeName, Object attributeAmount, String thingId){
        dittoClient.twin()
                .forId(ThingId.of(thingId))
                .mergeAttribute(attributeName, JsonValue.of(attributeAmount))
                .whenComplete(((adaptable, throwable) -> {
                    if (throwable != null) {
                        logger.error("Received error while sending Attribute MergeThing for: {} {}",  attributeName, throwable.getMessage());
                    } else {
                        logger.debug("Attribute Merge operation completed successfully for: {}",  thingId);
                    }
                }));

    }

    public void updateFeatureValue(String featureID, String featurePropertyName, Object featureAmount, String thingId) throws ExecutionException, InterruptedException {
        dittoClient.twin().startConsumption().toCompletableFuture();
        dittoClient.twin()
                .forFeature(ThingId.of(thingId), featureID)
                .mergeProperty(featurePropertyName, JsonValue.of(featureAmount))
                .whenComplete(((adaptable, throwable) -> {
                    if (throwable != null) {
                        logger.error("Received error while sending Feature MergeThing for: {} {}",  featureID, throwable.getMessage());
                    } else {
                        logger.debug("Feature Merge operation completed successfully for: {}",  thingId);
                    }
                }));

    }

    public Object getAttributeValueFromDitto(String attributeProperty, String thingId) throws InterruptedException, ExecutionException {
        CompletableFuture<Double> attributeAmount = new CompletableFuture<>();

        dittoClient.twin().forId(ThingId.of(thingId))
                .retrieve()
                .thenCompose(thing -> {
                    JsonValue feature = thing.getAttributes().
                            flatMap(attributes -> attributes.getValue(attributeProperty))
                            .orElse(JsonValue.nullLiteral());

                    attributeAmount.complete(feature.asDouble());
                    return CompletableFuture.completedFuture(null);
                });
        return attributeAmount.get();
    }
    public Object getFeatureValueFromDitto(String featureProperty, String thingId) throws InterruptedException, ExecutionException {
        CompletableFuture<Double> featureAmount = new CompletableFuture<>();

        dittoClient.twin().forId(ThingId.of(thingId))
                .retrieve()
                .thenCompose(thing -> {
                    JsonValue feature = thing.getFeatures().
                            flatMap(features -> features.getFeature(featureProperty)).
                            flatMap(Feature::getProperties).
                            flatMap(featureProperties -> featureProperties.getValue("amount"))
                            .orElse(JsonValue.nullLiteral());

                    featureAmount.complete(feature.asDouble());
                    return CompletableFuture.completedFuture(null);
                });
        return featureAmount.get();
    }



    public void startLoggingToInfluxDB(String measurement, String thingID, String subject, double amount){
        WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
        try {
            Point point = Point.measurement(measurement)
                    .addTag("thingID", thingID)
                    .addField(subject, amount)
                    .time(Instant.now().toEpochMilli(), WritePrecision.MS);

            writeApi.writePoint(point);
        }catch (InfluxException e){
            logger.error(e.getMessage());
        }
    }

    @Override
    public void updateFeatures(T thing) throws ExecutionException, InterruptedException {

    }


}
