package org.example.process;

import org.example.Gateways.GatewayManager;
import org.example.TaskManager;
import org.example.Things.TaskThings.TaskType;
import org.example.Things.WarehouseThing.Warehouse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RoutePlanner {

    public TaskManager taskManager;
    private GatewayManager gatewayManager;

    private final Logger logger = LoggerFactory.getLogger(RoutePlanner.class);


    private final ExecutorService executorService = Executors.newSingleThreadScheduledExecutor();


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
    public static class Route{
        private final List<Segment> segments;
        public String executor;

        public Route(List<Segment> segments, String executor){
            this.segments = segments;
            this.executor = executor;
        }
        public List<Segment> getSegments() {
            return segments;
        }
        public void setExecutor(String executor) {
            this.executor = executor;
        }

        public String getExecutor() {
            return executor;
        }
    }
    public Route createRoute(){
        String routeId = "route-" + UUID.randomUUID().toString().substring(0, 6);
        List<Segment> segments = new ArrayList<>();
        List<Warehouse> warehousesThingIds = new ArrayList<>(gatewayManager.getWarehouseList());
        double quantity = 100;

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
        return new Route(segments, null);
    }

    public int generateRouteSize(List<Warehouse> warehouseList){
        int max = warehouseList.size();
        int min = 3;
        return min + 2 * new Random().nextInt(((max - min) + 1 ) / 2);

    }

}
