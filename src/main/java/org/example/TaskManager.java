package org.example;

import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.example.SustainableCodeTest.Factory.Things.TaskFactory;
import org.example.SustainableCodeTest.Gateways.TaskGateway;
import org.example.Things.TaskThings.TaskType;
import org.example.Things.TaskThings.Tasks;
import org.example.Things.TruckThing.Truck;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
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

        Tasks refuelTask = taskFactory.getTasks();

        TaskGateway taskGateway = new TaskGateway(this.dittoClient, this.influxDBClient, refuelTask, truck);
        taskGateway.startUpdating(refuelTask);

    }
}
