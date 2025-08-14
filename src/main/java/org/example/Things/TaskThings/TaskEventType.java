package org.example.Things.TaskThings;

public enum TaskEventType {
    TASK_BEGIN("tastBegins"),
    TASK_FINISHED("taskFinished");


    private final String eventName;
    TaskEventType(String eventName){
        this.eventName = eventName;
    }

    public String getEventName(){
        return eventName;
    }

}
