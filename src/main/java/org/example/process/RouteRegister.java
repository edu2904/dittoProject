package org.example.process;

import java.util.HashMap;
import java.util.Map;

public class RouteRegister {
    private Map<Object, RouteExecutor> register = new HashMap<>();

    public void registerExecutor(String routeId, RouteExecutor routeExecutor){
        register.put(routeId, routeExecutor);
    }

    public RouteExecutor getRegister(String routeId) {
        return register.get(routeId);
    }
}
