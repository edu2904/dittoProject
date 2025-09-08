package org.example.Gateways.ConcreteGateways.TaskGateway;

import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.things.model.Thing;
import org.example.util.ThingHandler;

import java.io.DataInput;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class ThingSelector<T> {

    private final ThingHandler thingHandler;
    private final DittoClient dittoClient;

    public ThingSelector(ThingHandler thingHandler, DittoClient dittoClient){
        this.thingHandler = thingHandler;
        this.dittoClient = dittoClient;
    }

    public Optional<T> findBestThing(String thing, Function<Thing, T> mapper, Function<T, Double> score){
        try {
            List<T> things = thingHandler.searchThings(dittoClient, mapper, thing);
            return things.stream().min(Comparator.comparingDouble(score::apply));
        }catch (Exception e){
            return Optional.empty();
        }
    }
}
