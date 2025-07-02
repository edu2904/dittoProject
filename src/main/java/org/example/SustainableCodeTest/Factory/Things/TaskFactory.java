package org.example.SustainableCodeTest.Factory.Things;

import org.example.SustainableCodeTest.Factory.DigitalTwinFactory;
import org.example.Things.TaskThings.TaskType;
import org.example.Things.TaskThings.Tasks;

import java.util.concurrent.ExecutionException;

public class TaskFactory implements DigitalTwinFactory<Tasks>
{

    @Override
    public void createTwinsForDitto() throws ExecutionException, InterruptedException {

    }

    @Override
    public String getWOTURL() {
        TaskType taskType = getTaskType();
        switch (taskType)
    }

    @Override
    public String getPolicyURL() {
        return "https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/lkwpolicy";
    }

    @Override
    public void initializeThings() {


    }
    public TaskType getTaskType(Tasks tasks){
        return tasks.getTaskType();
    }
}
