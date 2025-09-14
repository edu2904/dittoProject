package org.example.Gateways;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.exceptions.InfluxException;
import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.client.changes.ChangeAction;
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
    protected final DittoClient listenerClient;
    protected InfluxDBClient influxDBClient;

    protected final Logger logger = LoggerFactory.getLogger(AbstractGateway.class);
    WriteApiBlocking writeApi;


    public AbstractGateway(DittoClient dittoClient, DittoClient listenerClient, InfluxDBClient influxDBClient){
        this.dittoClient = dittoClient;
        this.listenerClient = listenerClient;
        this.influxDBClient = influxDBClient;
        this.writeApi = influxDBClient.getWriteApiBlocking();

    }

    public void upDateThing(T thing){
        try{
            updateAttributes(thing);
            updateFeatures(thing);
            logToInfluxDB(thing, thing.getClass().getName());
        }catch (Exception e){
            logger.error("ERROR UPDATING THING {}:", thing);
        }
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

    public void updateFeatureValue(String featureID, String featurePropertyName, Object featureAmount, String thingId) {
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
        CompletableFuture<Object> attributeAmount = new CompletableFuture<>();

        dittoClient.twin().forId(ThingId.of(thingId))
                .retrieve()
                .thenCompose(thing -> {
                    JsonValue attribute = thing.getAttributes().
                            flatMap(attributes -> attributes.getValue(attributeProperty))
                            .orElse(JsonValue.nullLiteral());

                    if(attribute.isDouble()) {
                        attributeAmount.complete(attribute.asDouble());
                    }else if(attribute.isString()){
                        attributeAmount.complete(attribute.asString());
                    }
                    return CompletableFuture.completedFuture(null);
                });
        return attributeAmount.get();
    }
    public Object getFeatureValueFromDitto(String featureName, String featureProperty, String thingId) throws InterruptedException, ExecutionException {
        CompletableFuture<Object> featureAmount = new CompletableFuture<>();

        dittoClient.twin().forId(ThingId.of(thingId))
                .retrieve()
                .thenCompose(thing -> {
                    JsonValue feature = thing.getFeatures().
                            flatMap(features -> features.getFeature(featureName)).
                            flatMap(Feature::getProperties).
                            flatMap(featureProperties -> featureProperties.getValue(featureProperty))
                            .orElse(JsonValue.nullLiteral());
                    if(feature.isDouble()) {
                        featureAmount.complete(feature.asDouble());
                    }else if(feature.isString()){
                        featureAmount.complete(feature.asString());
                    }
                    return CompletableFuture.completedFuture(null);
                });
        return featureAmount.get();
    }



    public void startLoggingToInfluxDB(String measurement, String thingID, String subject, double amount){try {
            Point point = Point.measurement(measurement)
                    .addTag("thingID", thingID)
                    .addField(subject, amount)
                    .time(Instant.now().toEpochMilli(), WritePrecision.MS);

            writeApi.writePoint(point);
        }catch (InfluxException e){
            logger.error(e.getMessage());
        }
    }

    public void subscribeToAttributeChanges(String group){
        listenerClient.twin().registerForAttributesChanges(group, change -> {
            if(change.isFull()){
                logger.info("Received full Attribute change for {}" , change);
            }else {
                logger.info("Received Attribute change for {}", change);
            }
        });
    }
    public void subscribeToSpecificAttributeChange(String group, String path){
        listenerClient.twin().registerForAttributeChanges(group, path, change -> {
            if(change.isFull()){
                logger.info("Received full Attribute change for {}" , change);
            }else {
                logger.info("Received Attribute change for {}", change);
            }
        });
    }
    public void subscribeForSpecificFeatureChanges(String group, String feature){
        listenerClient.twin().registerForFeaturePropertyChanges(group, feature, change -> {
            if(change.isFull()){
                logger.info("Received full feature change for {} with change {}", feature, change);
            }else {
                logger.info("Received feature change for {} with change {}", feature, change);
            }
        });
    }
    public void subscribeForSpecificFeaturePropertyChange(String group, String feature, String property){
        listenerClient.twin().registerForFeaturePropertyChanges(group, feature, property, change -> {
            if(change.isFull() && change.getAction() == ChangeAction.MERGED){
                logger.info("Received full feature change for {}, {} with change {}", feature, property, change);
            }else {
                logger.info("Received feature change for {}, {} with change {}", feature, property, change);
            }
        });
    }

    @Override
    public void logToInfluxDB(T thing, String measurementType){
        logger.info("influx logging not Implemented yet for {}", thing);
    }
    @Override
    public void updateAttributes(T thing){
        logger.info("attribute updating not Implemented yet for {}", thing);
    }
    @Override
    public void updateFeatures(T thing){
        logger.info("feature updating not Implemented yet for {}", thing);
    }
}
