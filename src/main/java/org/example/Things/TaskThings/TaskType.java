package org.example.Things.TaskThings;

public enum TaskType {
    REFUEL("https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/instructionThings/refuelTruck"),

    TIREPRESSUREADJUSTMENT("https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/instructionThings/tirePressureTask"),

    LOAD("https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/instructionThings/loadTruck");

    private final  String wot;

    TaskType(String wot){
        this.wot = wot;
    }

    public String getWot() {
        return wot;
    }
}
