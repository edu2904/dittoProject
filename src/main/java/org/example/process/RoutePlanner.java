package org.example.process;

import org.example.Gateways.Permanent.GatewayManager;
import org.example.Gateways.Temporary.TaskManager;
import org.example.Things.TaskThings.TaskType;
import org.example.Things.WarehouseThing.Warehouse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RoutePlanner {

    public TaskManager taskManager;
    private final GatewayManager gatewayManager;

    private final Logger logger = LoggerFactory.getLogger(RoutePlanner.class);


    private final ExecutorService executorService = Executors.newSingleThreadScheduledExecutor();


    // creates the routes
    public RoutePlanner(TaskManager taskManager, GatewayManager gatewayManager){
        this.taskManager = taskManager;
        this.gatewayManager = gatewayManager;
    }

    public static class Segment{
        private final Warehouse from;
        private final Warehouse to;
        private final TaskType taskType;
        private final double quantity;
        private final String setId;

        // Segment hold the information relevant for task creation.
        public Segment(Warehouse from, Warehouse to, TaskType taskType, double quantity, String routeId){
            this.from = from;
            this.to = to;
            this.taskType = taskType;
            this.quantity = quantity;
            this.setId = routeId;
        }

        public Warehouse getFrom() {
            return from;
        }
        public Warehouse getTo() {
            return to;
        }

        public TaskType getTaskType() {
            return taskType;
        }

        public double getQuantity() {
            return quantity;
        }

        public String getSetId() {
            return setId;
        }

        @Override
        public String toString(){
            return from + " to " + to + " which task: " + taskType.toString();
        }
    }

    // Routes are combined of 2 or more segments. They have an ID which helps to identify them and get their set of segments
    public static class Route{
        private final List<Segment> segments;
        private List<String> executor;
        private String routeId;
        private List<Double> totalTimeMinutes = new ArrayList<>();
        private List<String> routeEvents = new ArrayList<>();

        public Route(String routeId, List<Segment> segments, List<String> executor){
            this.segments = segments;
            this.executor = executor;
            this.routeId = routeId;
        }
        public List<Segment> getSegments() {
            return segments;
        }
        public void setExecutor(List<String> executor) {
            this.executor = executor;
        }

        public List<String> getExecutor() {
            return executor;
        }
        public void removeExecutor(String executor){
            this.executor.remove(executor);
        }

        public String getRouteId() {
            return routeId;
        }

        public void setRouteId(String routeId) {
            this.routeId = routeId;
        }

        public void setTotalTimeMinutes(List<Double> totalTimeMinutes) {
            this.totalTimeMinutes = totalTimeMinutes;
        }

        public void addTotalTimeMinutes(Double minutes){
            totalTimeMinutes.add(minutes);
        }
        public List<Double> getTotalTimeMinutes() {
            return totalTimeMinutes;
        }

        public List<String> getRouteEvents() {
            return routeEvents;
        }

        public void setRouteEvents(List<String> routeEvents) {
            this.routeEvents = routeEvents;
        }
        public void addRouteEVent(String routeEvent){
            this.routeEvents.add(routeEvent);
        }
    }


    // creates a fixes set of routes. The number of routes can be chosen arbitrarily. Their order is deterministic.
    public List<Route> createFixedTestRoutes(int numberOfRoutes) {
        List<Warehouse> warehouses = new ArrayList<>(gatewayManager.getWarehouseList());
        List<Route> routes = new ArrayList<>();

        if (warehouses.size() < 3) {
            logger.warn("Not enough warehouses to create routes");
            return routes;
        }

        warehouses.sort(Comparator.comparing(Warehouse::getThingId));


        int size = warehouses.size();

        for (int r = 0; r < numberOfRoutes; r++) {

            String routeId = "route-" + (r + 1);



           int warehousePerRoute;
           if(size >= 5){
               warehousePerRoute = (r % 2 == 0) ? 3 : 5;
           }else {
               warehousePerRoute = 3;
           }

            int startIndex = (r * 2) % size;
            int step = 1 + (r % (size - 1));

            //double baseQuantity = 100.0;
            //double quantity = baseQuantity * new double[]{0.5, 1.0, 2.0, 3.0}[r % 4];

            //double quantity = 150.0;
            //double quantity = baseQuantity * (2 + (r % 3));
           // double quantity = baseQuantity * (2 + 0.5 * r);
              double quantity = 50.0 + (r % 6) * 50.0;
            List<Warehouse> routeWarehouses = new ArrayList<>();
            for (int i = 0; i < warehousePerRoute; i++) {
                routeWarehouses.add(warehouses.get((startIndex + i * step) % warehouses.size()));
            }

            List<Segment> segments = new ArrayList<>();

            for (int i = 0; i < warehousePerRoute - 1; i++) {
                Warehouse from = routeWarehouses.get(i);
                Warehouse to = routeWarehouses.get(i + 1);

                TaskType taskType;
                if(i % 2 == 0){
                    taskType = TaskType.LOAD;
                }else {
                    taskType = TaskType.UNLOAD;
                }

                segments.add(new Segment(from, to, taskType, quantity, routeId));
            }

            routes.add(new Route(routeId, segments, null));
        }

        logger.info("Generated {} fixed test routes", routes.size());
        return routes;
    }

    // random route creation. Creates a random route consisting of 2-4 segments
    public Route createRandomRoute(){
        String routeId = "route-" + UUID.randomUUID().toString().substring(0, 6);
        List<Segment> segments = new ArrayList<>();
        List<Warehouse> warehousesThingIds = new ArrayList<>(gatewayManager.getWarehouseList());
        double quantity = 200;

        if(warehousesThingIds.size() < 2){
            logger.warn("Not enough Warehouses to create Route");
            return null;
        }
        Collections.shuffle(warehousesThingIds);
        int routeSize = generateRouteSize(warehousesThingIds);
        List<Warehouse> routeWarehouse = warehousesThingIds.subList(0, routeSize);

        for(int i = 0; i < routeWarehouse.size()-1; i++){
            Warehouse from = routeWarehouse.get(i);
            Warehouse to = routeWarehouse.get(i+1);
            TaskType taskType;
            if(i == 0){
                taskType = TaskType.LOAD;
            }else if(i == routeWarehouse.size() - 2){
                taskType = TaskType.UNLOAD;
            }else {
                taskType = (i % 2 != 0) ? TaskType.UNLOAD : TaskType.LOAD;
            }


            segments.add(new Segment(from, to, taskType, quantity, routeId));
            System.out.println("Segment: " + from.getThingId() + " -> " + to.getThingId() +
                    ", Task: " + taskType + ", Quantity: " + quantity);
        }
        return new Route(null, segments, null);
    }

    public int generateRouteSize(List<Warehouse> warehouseList){
        int max = warehouseList.size();
        int min = 3;
        return min + 2 * new Random().nextInt(((max - min) + 1 ) / 2);

    }
}
