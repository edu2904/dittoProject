package org.example.SustainableCodeTest.Gateways;

import com.influxdb.client.InfluxDBClient;
import org.eclipse.ditto.client.DittoClient;
import org.example.SustainableCodeTest.AbstractGateway;
import org.example.Things.TaskThings.Tasks;
import org.example.Things.TaskThings.TasksEventsActions;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TaskGateway extends AbstractGateway<Tasks> {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    TasksEventsActions tasksEventsActions = new TasksEventsActions();

    public TaskGateway(DittoClient dittoClient, InfluxDBClient influxDBClient) {
        super(dittoClient, influxDBClient);
    }


    @Override
    public void startUpdating(Tasks tasks) {
        Runnable updateTask = () -> {

               updateAttributes(tasks);

        };
        scheduler.scheduleAtFixedRate(updateTask, 0, 3, TimeUnit.SECONDS);
    }

    @Override
    public void logToInfluxDB(Tasks thing) {

    }

    @Override
    public void handleEvents(Tasks thing) {

    }

    @Override
    public void handelActions(Tasks thing) {

    }

    @Override
    public void subscribeForEventsAndActions(List<Tasks> things) {
        for(Tasks tasks : things){

            tasksEventsActions.startTaskLogging(tasks.getThingId());
        }
    }

    @Override
    public void startGateway() {

    }

    @Override
    public String getWOTURL() {
        return null;
    }


    @Override
    public void updateAttributes(Tasks tasks) {
        updateAttributeValue("status", tasks.getStatus().toString(), tasks.getThingId());
        updateAttributeValue("targetTruck", tasks.getTargetTruck(), tasks.getThingId());
        updateAttributeValue("creationDate", tasks.getCreationTime(), tasks.getThingId());

    }
}
