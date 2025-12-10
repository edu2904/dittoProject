package org.example.Gateways.Temporary.TaskGateway;

import com.eclipsesource.json.Json;
import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.client.live.messages.RepliableMessage;
import org.eclipse.ditto.json.JsonArray;
import org.eclipse.ditto.json.JsonArrayBuilder;
import org.eclipse.ditto.json.JsonFactory;
import org.example.Mapper.TruckMapper;
import org.example.Gateways.AbstractGateway;
import org.example.Things.EventActionHandler;
import org.example.Things.TaskThings.*;
import org.example.Things.TruckThing.TruckEventsActions;
import org.example.Things.TruckThing.TruckStatus;
import org.example.Things.WarehouseThing.Warehouse;
import org.example.util.Config;
import org.example.util.ThingHandler;
import org.example.Things.TruckThing.Truck;
import org.json.JSONArray;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.ToDoubleFunction;

public class TaskGateway extends AbstractGateway<Task> {

    private static final Set<String> RESERVED_TRUCKS = ConcurrentHashMap.newKeySet();
    TasksEvents tasksEvents = new TasksEvents();
    TaskActions taskActions = new TaskActions();
    ThingHandler thingHandler = new ThingHandler();
    private final Set<String> openThings = ConcurrentHashMap.newKeySet();
    private final Random random = new Random();
    private static final ScheduledExecutorService REPLACEMENT_SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService taskExecutor = Executors.newSingleThreadScheduledExecutor();


    Task task;

    public TaskGateway(DittoClient dittoClient, DittoClient listenerClient, InfluxDBClient influxDBClient, Task task) {
        super(dittoClient, listenerClient, influxDBClient);
        this.task = task;
        assignThingToTask();


    }

    @Override
    public void startGateway() {
        updateAttributes(task);
    }
    @Override
    public void logToInfluxDB(Task thing) {

    }
    // updates attributes of the core values and the specific ones if needed
    @Override
    public void updateAttributes(Task task) {
        updateAttributeValue("status", task.getStatus().toString(), task.getThingId());
        updateAttributeValue("creationDate", task.getCreationTime(), task.getThingId());
        updateAttributeValue("type", task.getTaskType().toString(), task.getThingId());

        switch (task.getTaskType()){
            case LOAD, UNLOAD:
                String warehouse = (String) task.getData("to");
                String warehouse1 = (String) task.getData("from");
                double quantity = (double) task.getData("quantity");

                updateAttributeIfPresent("to", warehouse, task.getThingId());
                updateAttributeIfPresent("from", warehouse1, task.getThingId());
                updateAttributeIfPresent("quantity", quantity, task.getThingId());
        }
    }


    public <T> T selectBestThing(List<T> things, ToDoubleFunction<T> score) {
        return things.stream().min(Comparator.comparingDouble(score)).orElse(null);
    }

    // assignes things to tasks
    public void assignThingToTask() {
        List<String> selectedTrucks = findBestIdleTruck((Double) task.getData("quantity"));

        logger.debug("Things found: {}" , selectedTrucks.size());

        // if no suitable things are found, the task will try again after some time
        if (selectedTrucks.isEmpty()) {

            noSuitableThingFound();
            return;
        }

        // if suitable things are found the task will store them
        task.setTargetTrucks(selectedTrucks);


        // the target things will be stored in Eclipse Ditto and visualized
        JsonArrayBuilder builder = JsonArray.newBuilder();
        for(String thing : task.getTargetTrucks()){
            builder.add(JsonFactory.newValue(thing));
        }
        JsonArray jsonArray = builder.build();
        updateAttributeValue("targetThings", jsonArray, task.getThingId());


        // stores them into open things. This is necessary to know if all the trucks assigned to the tasks fulfilled their job.
        // a truck will be removed from the openThings set if it finished its task
        // if the openThings Set remains not empty, the task will never send a finished event.
        openThings.clear();
        openThings.addAll(selectedTrucks);

        sendEventForTask(task);


    }

    // If all the trucks are assigned the task can start, therefore it sends a startEvent and an action to the trucks with all the necessary information
    public void sendEventForTask(Task task) {
        tasksEvents.sendStartEvent(dittoClient, task);
        switch (task.getTaskType()) {
            case LOAD -> {
                taskActions.sendLoadAction(dittoClient, task);
                registerForThingMessagesFromThing();
                logger.info("LOAD EVENT SENT FOR {}", task.getThingId());
            }
            case UNLOAD -> {
                taskActions.sendUnloadAction(dittoClient, task);
                registerForThingMessagesFromThing();
                logger.info("UNLOAD EVENT SENT FOR {}", task.getThingId());
            }
            default -> logger.warn("Unknown Task Type: {}", task.getTaskType());
        }
    }


    // If no targetThings are found, the task status is set to PAUSED and the process is informed
    public void noSuitableThingFound() {
        logger.warn("NO BEST THING FOUND FOR {}", task.getThingId());
        logger.warn(RESERVED_TRUCKS.toString());
        task.setStatus(TaskStatus.PAUSED);
        tasksEvents.sendPausedEvent(dittoClient, task);
    }


    // This code tries to find the best suitable target things
    public List<String> findBestIdleTruck(double requiredCargo) {
        List<String> preAssigned = task.getTargetTrucks();
        try {
                // at first the task will check if it has preassigned target trucks. This applies to following tasks that belong to the same task List.
                // In the example of this implementation, the two or more task are stored in a route. The next task inside a route should get the same target things as the previous task
                if (preAssigned != null && !preAssigned.isEmpty()) {
                    List<Truck> candidates = thingHandler.searchThings(dittoClient, new TruckMapper()::fromThing, "truck")
                            .stream()
                            .filter(truck -> preAssigned.contains(truck.getThingId()))
                            .sorted(Comparator.comparingDouble(Truck::getCapacity).reversed())
                            .toList();

                    if (candidates.isEmpty()) {
                        logger.warn("Pre-assigned trucks {} for task {} not found as Things",
                                preAssigned, task.getThingId());
                        return List.of();
                    }

                    Map<String, Double> alloc =  assignCargo(requiredCargo, candidates, (Warehouse) task.getData("toWarehouse"));
                    if (alloc.isEmpty()) {
                        return List.of();
                    }

                    task.setThingAllocation(alloc);
                    return preAssigned;
                }

                // if the task does not have preassigned tasks, it will search for trucks that are idle and not occupied by other tasks
                List<Truck> idleTrucks = thingHandler.searchThings(dittoClient, new TruckMapper()::fromThing, "truck")
                        .stream()
                        .filter(truck -> truck.getStatus() == TruckStatus.IDLE)
                        .filter(truck -> !RESERVED_TRUCKS.contains(truck.getThingId()))
                        .toList();

                if(idleTrucks.isEmpty()){
                    logger.info("No trucks found for task {}", task.getThingId());
                    return List.of();
                }
               Map<String, Double> cargoAllocation;

                if(Config.RANDOM_DECISION_MAKING){
                    cargoAllocation = assignCargoRandom(requiredCargo, idleTrucks, (Warehouse) task.getData("toWarehouse"));
                }else {
                    cargoAllocation = assignCargo(requiredCargo, idleTrucks, (Warehouse) task.getData("toWarehouse"));
                }

                if(cargoAllocation.isEmpty()) {
                    logger.info("NOT ENOUGH SPACE FOUND FOR CARGO DELIVERY FOR TASK {}", task.getThingId());
                    return List.of();
                }
            logger.info("Final allocation for task {}: {})",
                    task.getThingId(), cargoAllocation);

            task.setThingAllocation(cargoAllocation);
            List<String> selectedTrucks = new ArrayList<>(cargoAllocation.keySet());
            if(!reserveTrucks(selectedTrucks)){
                logger.info("Selected trucks already in use");
                return List.of();
            };
            return selectedTrucks;

        } catch (Exception e) {
            logger.error("error while looking for truck: {}", e.getMessage(), e);
            return List.of();
        }

    }



//listener for messages from the truck. Sending to the process.
    public void registerForThingMessagesFromThing() {
        listenerClient.live().registerForMessage("thing_" + task.getThingId(), "*", message -> {
            taskExecutor.submit(() -> handleTruckMessage(message));
        });
    }
    public void handleTruckMessage(RepliableMessage<?, Object> message){
        Optional<?> optionalObject = message.getPayload();
        if (optionalObject.isEmpty()){
            return;
        }

        String rawPayload = optionalObject.get().toString();
        var parsePayload = Json.parse(rawPayload).asObject();

        String thingId = parsePayload.get("thingId").asString();

        if(!openThings.contains(thingId)){
            return;
        }

        switch (message.getSubject()) {
            case TruckEventsActions.TRUCK_SUCCESSFUL:


                openThings.remove(thingId);
                System.out.println("OPENTHINGS SIZEEEEE " + openThings.size());
                if(openThings.isEmpty()) {
                    long endTime = System.currentTimeMillis();
                    double minutes = (endTime - task.getStartTime()) / 60000.0;
                    task.setTime(minutes);
                    tasksEvents.sendTimeEvent(dittoClient, task);

                    tasksEvents.sendFinishedEvent(dittoClient, task);
                    logger.info("Task {} for Truck {} finished successful", task.getThingId(), task.getTargetTrucks());
                    listenerClient.live().deregister("thing_" + task.getThingId());

                }

                break;
            case TruckEventsActions.TRUCK_FAILED:

                if (task.getTargetTrucks().contains(thingId)) {
                    long endTime = System.currentTimeMillis();
                    double minutes = (endTime - task.getStartTime()) / 60000.0;
                    task.setTime(minutes);

                    task.setEventInformation("WAREHOUSE CAPACITY WITH " + thingId);
                    tasksEvents.sendFailEvent(dittoClient, task);
                    tasksEvents.sendTimeEvent(dittoClient, task);
                    tasksEvents.taskAbortedEvent(dittoClient, task);
                    logger.warn("Task {} of Truck {} failed", task.getThingId(), task.getTargetTrucks());
                    releaseTrucks(task.getTargetTrucks());
                    listenerClient.live().deregister("thing_" + task.getThingId());
                }

                break;
            case TruckEventsActions.TRUCK_TIRE_PRESSURE_LOW, TruckEventsActions.FUEL_TOO_LOW:
                task.setEventInformation(message.getSubject() + "for " + thingId);
                if (!task.getTargetTrucks().contains(thingId)) {
                    return;
                }
                if(task.getTaskType() == TaskType.LOAD){
                    releaseTrucks(List.of(thingId));
                    task.getTargetTrucks().remove(thingId);
                    tryToReplaceFailedTruck(thingId);
                    tasksEvents.sendEscalationEvent(dittoClient, task, thingId);
                    logger.warn("Task {} of Truck {} escalated", task.getThingId(), task.getTargetTrucks());

                }else if(task.getTaskType() == TaskType.UNLOAD){
                    releaseTrucks(task.getTargetTrucks());
                    task.getTargetTrucks().remove(thingId);
                    tasksEvents.taskAbortedEvent(dittoClient, task);
                    tasksEvents.sendFailEvent(dittoClient, task);
                    logger.warn("Task {} of Truck {} failed", task.getThingId(), task.getTargetTrucks());
                    listenerClient.live().deregister("thing_" + task.getThingId());
                }

                break;

        }
    }

    private boolean reserveTrucks(List<String> trucks){
        synchronized (RESERVED_TRUCKS){
            if(trucks.stream().anyMatch(RESERVED_TRUCKS::contains)){
                return false;
            }
            RESERVED_TRUCKS.addAll(trucks);
            return true;
        }
    }
    public static void releaseTrucks(List<String> trucks) {
        synchronized (RESERVED_TRUCKS) {
            trucks.forEach(RESERVED_TRUCKS::remove);
        }
    }
    public synchronized Map<String, Double> assignCargoRandom(double requiredCargo, List<Truck> trucks, Warehouse toWarehouse){
        Map<String, Double> cargoAllocation = new LinkedHashMap<>();
        double remainingCargo = requiredCargo;

        List<Truck> shuffledList = new ArrayList<>(trucks);
        Collections.shuffle(shuffledList);

        for(Truck truck : shuffledList){
            if(remainingCargo <= 0){
                break;
            }
            double capacity = truck.getCapacity();

            double assignedCargo = Math.min(capacity, remainingCargo);
            cargoAllocation.put(truck.getThingId(), assignedCargo);
            remainingCargo -= assignedCargo;
            logger.debug("Required Cargo: {} for {}", requiredCargo, task.getThingId());

            logger.debug("Remaining Cargo: {} for {}", remainingCargo, task.getThingId());
            logger.info("Allocating for task {}: truck={}, Capacity={}, availableCapacity={}, remainingBefore={}",
                    task.getThingId(), truck.getThingId(), truck.getCapacity(), capacity, remainingCargo);

        }
        if (remainingCargo > 0){
            logger.warn("NOT ENOUGH TRUCK STORAGE! MISSING : {}", remainingCargo);
            return Collections.emptyMap();
        }
        return cargoAllocation;
    }

    public synchronized Map<String, Double> assignCargo(double requiredCargo, List<Truck> trucks, Warehouse toWarehouse) {
        Map<String, Double> cargoAllocation = new LinkedHashMap<>();
        double remainingCargo = requiredCargo;

        List<Truck> idleTrucks = new ArrayList<>(trucks);

        while (remainingCargo > 0 && !idleTrucks.isEmpty()) {
            double finalRemainingCargo = remainingCargo;
            Truck chosenTruck = idleTrucks.stream()
                    .min(Comparator.comparingDouble
                            (t -> computeTruckScore(t, finalRemainingCargo, toWarehouse))).orElse(null);

            double capacity = chosenTruck.getCapacity();
            if (capacity <= 0) {
                idleTrucks.remove(chosenTruck);
                continue;
            }

            double assignedCargo = Math.min(capacity, remainingCargo);
            cargoAllocation.put(chosenTruck.getThingId(), assignedCargo);
            remainingCargo -= assignedCargo;

            idleTrucks.remove(chosenTruck);

            logger.debug("Required Cargo: {} for {}", requiredCargo, task.getThingId());

            logger.debug("Remaining Cargo: {} for {}", remainingCargo, task.getThingId());

            logger.info("Allocating for task {}: truck={}, Capacity={}, availableCapacity={}, remainingBefore={}",
                    task.getThingId(), chosenTruck.getThingId(), chosenTruck.getCapacity(), capacity, remainingCargo);

        }
        if (remainingCargo > 0){
            logger.warn("NOT ENOUGH TRUCK STORAGE! MISSING : {}", remainingCargo);
            return Map.of();
        }
        return cargoAllocation;
    }

    private double computeTruckScore(Truck truck, double requiredCargo, Warehouse toWarehouse){
        double util = truck.getUtilization();
        logger.debug("Fuel util: {}, for Truck {}", util, truck.getThingId());
        System.out.println("FUEL UTIL " + util);
        double locationUtil = truck.calculateLocationUtilization(toWarehouse);
        logger.debug("Location util: {}, for Truck {}", locationUtil, truck.getThingId());
        double capacityUtil = truck.calculateCapacityUtilization(requiredCargo);
        logger.debug("Capacity util: {}, for Truck {}", capacityUtil, truck.getThingId());


        double weightFuel = 0.1;
        double weightLocation = 0.45;
        double weightCapacity = 0.45;
        System.out.println(weightFuel * util + weightLocation * locationUtil + weightCapacity * capacityUtil + "         FOR" + truck.getThingId());
        return weightFuel * util + weightLocation * locationUtil + weightCapacity * capacityUtil;
    }

    public void tryToReplaceFailedTruck(String failedTruckId){
        Map<String, Double> allocations = task.getThingAllocation();

        double missingCargo = allocations.get(failedTruckId);

        allocations.remove(failedTruckId);
        task.setThingAllocation(allocations);
        task.getTargetTrucks().remove(failedTruckId);

        releaseTrucks(List.of(failedTruckId));

        Warehouse toWarehouse = (Warehouse) task.getData("toWarehouse");

        replaceIdleTruck(failedTruckId, missingCargo, toWarehouse);
    }
    public void replaceIdleTruck(String failedTruckId, double missingCargo, Warehouse toWarehouse){
        List<Truck> idleTrucks = thingHandler.searchThings(dittoClient, new TruckMapper()::fromThing, "truck")
                .stream()
                .filter(truck -> truck.getStatus() == TruckStatus.IDLE)
                .filter(truck -> !RESERVED_TRUCKS.contains(truck.getThingId()))
                .filter(truck -> !truck.getThingId().equals(failedTruckId))
                .toList();

        if(idleTrucks.isEmpty()) {
            logger.info("At the moment no replacement truck available for {} ", task.getThingId());

            REPLACEMENT_SCHEDULER.schedule(() -> taskExecutor.submit(() -> replaceIdleTruck(failedTruckId, missingCargo, toWarehouse)), 1, TimeUnit.MINUTES);
            return;
        }
        Map<String, Double> replacementAllocation = new LinkedHashMap<>();
        if(Config.RANDOM_DECISION_MAKING){
            replacementAllocation = assignCargoRandom(missingCargo, idleTrucks, toWarehouse);
        }else {
            replacementAllocation = assignCargo(missingCargo, idleTrucks, toWarehouse);
        }
        if(replacementAllocation.isEmpty()){
            logger.info("At the moment not enough replacement trucks available for {} ", task.getThingId());

            REPLACEMENT_SCHEDULER.schedule(() -> taskExecutor.submit(() -> replaceIdleTruck(failedTruckId, missingCargo, toWarehouse)), 1, TimeUnit.MINUTES);
            return;
        }
        Map<String, Double> allocations = task.getThingAllocation();
        allocations.putAll(replacementAllocation);
        task.setThingAllocation(allocations);

        List<String> newTrucks = new ArrayList<>(replacementAllocation.keySet());
        task.getTargetTrucks().addAll(newTrucks);
        openThings.addAll(newTrucks);
        openThings.remove(failedTruckId);
        reserveTrucks(newTrucks);
        taskActions.sendReplacementLoadAction(dittoClient, task, replacementAllocation);
    }
}

