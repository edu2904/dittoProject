package org.example.Things;

public interface DigitalTwinData<ThingStatus extends Enum<ThingStatus>> {

    void setThingID(String thingID);
    String getThingID();

    void setStatus(ThingStatus status);

    ThingStatus getStatus();

    void setUtilization(double utilization);
    Double getUtilization();

}
