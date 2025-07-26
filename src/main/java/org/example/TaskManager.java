package org.example;

import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.example.Factory.ConcreteFactories.TaskFactory;
import org.example.Gateways.ConcreteGateways.TaskGateway;
import org.example.Things.TaskThings.TaskType;
import org.example.Things.TaskThings.Tasks;
import org.example.Things.TruckThing.Truck;

import java.util.concurrent.ExecutionException;

public class TaskManager {
    private final DittoClient dittoClient;
    private final InfluxDBClient influxDBClient;

    public TaskManager(DittoClient dittoClient, InfluxDBClient influxDBClient){
        this.dittoClient = dittoClient;
        this.influxDBClient = influxDBClient;
    }
    public void startTask(TaskType taskType, Truck truck) throws ExecutionException, InterruptedException {
        TaskFactory taskFactory = new TaskFactory(dittoClient, taskType, truck);

        taskFactory.createTwinsForDitto();

        Tasks task = taskFactory.getTasks();

        TaskGateway taskGateway = new TaskGateway(this.dittoClient, this.influxDBClient, task, truck);
        taskGateway.startUpdating(task);

    }
}
