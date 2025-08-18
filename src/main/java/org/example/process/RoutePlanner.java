package org.example.process;

import org.example.Gateways.GatewayManager;
import org.example.TaskManager;
import org.example.Things.TaskThings.TaskType;
import org.example.Things.WarehouseThing.Warehouse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RoutePlanner {

    public TaskManager taskManager;
    private GatewayManager gatewayManager;



    private final ExecutorService executorService = Executors.newSingleThreadScheduledExecutor();


    public RoutePlanner(TaskManager taskManager, GatewayManager gatewayManager){
        this.taskManager = taskManager;
        this.gatewayManager = gatewayManager;
    }






    public static class Segment{
        private final Warehouse from;
        private final Warehouse to;
        private TaskType taskType;
        private double quantity;
        public Segment(Warehouse from, Warehouse to, TaskType taskType, double quantity){
            this.from = from;
            this.to = to;
            this.taskType = taskType;
            this.quantity = quantity;
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

        @Override
        public String toString(){
            return from.getThingId() + " to " + to.getThingId() + " which task: " + taskType.toString();
        }
    }
    public static class Route{
        private final List<Segment> segments;
        public Route(List<Segment> segments){
            this.segments = segments;
        }
        public List<Segment> getSegments() {
            return segments;
        }
    }
    public Route createRoute(){
        List<Segment> segments = new ArrayList<>();
        List<Warehouse> warehouses = gatewayManager.getWarehouseList();
        double quantitiy = 100;
        for(int i = 0; i < warehouses.size()-1; i++){
            Warehouse from = warehouses.get(i);
            Warehouse to = warehouses.get(i+1);

            TaskType taskType = (i % 2 == 0) ? TaskType.LOAD : TaskType.UNLOAD;
            segments.add(new Segment(from, to, taskType, quantitiy));
        }
        return new Route(segments);
    }

}
