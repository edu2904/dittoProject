package org.example.process;

import java.util.HashMap;
import java.util.Map;

// stored routes by their ID
public class RouteRegister {
    private Map<Object, RouteExecutor> register = new HashMap<>();

    public void registerExecutor(String routeId, RouteExecutor routeExecutor){
        register.put(routeId, routeExecutor);
    }

    public RouteExecutor getRegister(String routeId) {
        return register.get(routeId);
    }
}
