package org.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.example.Factory.ConcreteFactories.TruckFactory;
import org.example.Gateways.Permanent.GatewayManager;
import org.example.Things.Location;
import org.example.Things.TruckThing.Truck;
import org.example.Things.TruckThing.TruckSimulation;
import org.example.process.TruckProcess;
import org.example.util.ThingHandler;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class TruckControllerHTTPServer {
    GatewayManager gatewayManager;
    TruckProcess truckProcess;


    public TruckControllerHTTPServer(GatewayManager gatewayManager, TruckProcess truckProcess){
        this.gatewayManager = gatewayManager;
        this.truckProcess = truckProcess;
    }
    public void control(){

    HttpServer server = null;
        try {
        server = HttpServer.create(new InetSocketAddress(8000), 0);
    } catch (IOException e) {
        throw new RuntimeException(e);
    }

        createNewRoute(server);
        //addNewTruck(server);

        server.setExecutor(null); // creates a default executor
        server.start();

}

public void createNewRoute(HttpServer server){
    server.createContext("/createNewRoute", (HttpExchange t) -> {
        truckProcess.startRandomProcess();
        String response = "New Route Created";
        t.sendResponseHeaders(200, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
    });
}

public void addNewTruck(HttpServer server){
    server.createContext("/addTruck", (HttpExchange t) -> {
        String requestQuery = t.getRequestURI().getQuery();
        Map<String, String> parameters = new HashMap<>();
        if (requestQuery != null) {
            for (String param : requestQuery.split("&")) {
                String[] paramValue = param.split("=");
                if (paramValue.length == 2) {
                    parameters.put(paramValue[0], paramValue[1]);
                }
            }
        }
        TruckFactory truckFactory = new TruckFactory(gatewayManager.getThingClient(), new ThingHandler());
        Truck truck = truckFactory.createDefaultTruck(gatewayManager.getTruckList().size() + 1);

        if (parameters.containsKey("FuelTank")) {
            truck.setFuel(Integer.parseInt(parameters.get("FuelTank")));
        }
        if (parameters.containsKey("capacity")) {
            truck.setCapacity(Integer.parseInt(parameters.get("capacity")));
        }
        if (parameters.containsKey("weight")) {
            truck.setWeight(Integer.parseInt(parameters.get("weight")));
        }
        if (parameters.containsKey("lat") && parameters.containsKey("lon")) {
            double lat = Double.parseDouble(parameters.get("lat"));
            double lon = Double.parseDouble(parameters.get("lon"));
            truck.setLocation(new Location(lat, lon));
        }

        gatewayManager.getTruckList().add(truck);
        truckFactory.getThings().add(truck);

        try {
            truckFactory.createTwinsForDitto();
            truck.setGasStationList(new ArrayList<>(gatewayManager.getGasStationList()));
            truck.setWarehouseList(new ArrayList<>(gatewayManager.getWarehouseList()));
            TruckSimulation truckSimulation = new TruckSimulation(gatewayManager.getThingClient(), truck);
            truckSimulation.runSimulation(gatewayManager);
            System.out.println("TRUCK CREATED");
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        String response = "Truck " + truck.getThingId() + " created";
        t.sendResponseHeaders(200, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();

    });
}
}
