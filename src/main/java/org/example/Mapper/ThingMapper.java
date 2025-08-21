package org.example.Mapper;

import org.eclipse.ditto.things.model.Thing;

public interface ThingMapper<T> {
    T fromThing(Thing thing);
}
