package org.example.Mapper;

import org.eclipse.ditto.things.model.Thing;
import org.example.Things.TaskThings.Task;
import org.example.Things.TaskThings.TaskStatus;
import org.example.Things.TaskThings.TaskType;

public class TaskMapper {

    // builds truck from a json.
    // This is needed when whole things are requested from ditto. The information ditto sends is a JSON file.
    // The values inside the JSON file have to be extracted in this mapper
    public static Task fromThing(Thing thing){
        Task task = new Task();
        task.setThingId(thing.getEntityId().map(Object::toString).orElse(null));
        thing.getAttributes().ifPresent(attributes -> {
            String taskStatus = attributes.getValue("status")
                    .map(Object::toString)
                    .map(s -> s.replace("\"", ""))
                    .orElse("STARTING");
            task.setStatus(TaskStatus.valueOf(taskStatus));
            String taskType = attributes.getValue("type")
                    .map(Object::toString)
                    .map(s -> s.replace("\"", ""))
                    .orElse("UNDEFINED");
            task.setTaskType(TaskType.valueOf(taskType));
            String creationDate = attributes.getValue("creationDate")
                    .map(Object::toString).orElse("");
            task.setCreationTime(creationDate);
        });
        return task;
    }
}
