package org.example;

import org.eclipse.ditto.things.model.Thing;
import org.eclipse.ditto.things.model.ThingId;
import org.eclipse.ditto.things.model.ThingsModelFactory;
import org.example.Client.DittoClientBuilder;

import java.util.concurrent.ExecutionException;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // Press Alt+Eingabe with your caret at the highlighted text to see how
        // IntelliJ IDEA suggests fixing it.
        DittoClientBuilder dittoClientBuilder = new DittoClientBuilder();
        dittoClientBuilder.createFirstThing();
        dittoClientBuilder.getFuelTankValue();


    }
}