package org.example.Mapper;

import org.eclipse.ditto.things.model.Thing;
import org.example.Things.Location;
import org.example.Things.TaskThings.Task;
import org.example.Things.TaskThings.TaskStatus;
import org.example.Things.TaskThings.TaskType;
import org.example.Things.TruckThing.Truck;
import org.example.Things.TruckThing.TruckStatus;

import java.util.Objects;

public class TruckMapper implements ThingMapper<Truck>{

    @Override
    public Truck fromThing(Thing thing) {
        Truck truck = new Truck();
        truck.setThingId(thing
                .getEntityId()
                .map(Object::toString)
                .orElse(null));

        thing.getAttributes().ifPresent(attributes -> {
            String thingId = attributes.getValue("thingId")
                    .map(Object::toString)
                    .map(s -> s.replace("\"", ""))
                    .orElse("UNDEFINED");
            truck.setThingId(thingId);
            String thingStatus = attributes.getValue("status")
                    .map(Object::toString)
                    .map(s -> s.replace("\"", ""))
                    .orElse("UNDEFINED");
            truck.setStatus(TruckStatus.valueOf(thingStatus));
            double utilization = attributes.getValue("utilization").map(Objects::toString)
                    .map(Double::parseDouble).orElse(0.0);
            truck.setUtilization(utilization);
            double capacity = attributes.getValue("capacity").map(Objects::toString)
                    .map(Double::parseDouble).orElse(0.0);
            truck.setCapacity(capacity);
            double lat = attributes
                    .getValue("location/geo:lat")
                    .map(Objects::toString)
                    .map(Double::parseDouble).
                    orElse(0.0);
            double lon = attributes.getValue("location/geo:long")
                    .map(Objects::toString)
                    .map(Double::parseDouble)
                    .orElse(0.0);
            truck.setLocation(new Location(lat, lon));

        });
        return truck;
    }
}
