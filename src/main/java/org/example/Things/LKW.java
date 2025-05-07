package org.example.Things;

import org.eclipse.ditto.things.model.ThingId;

import java.util.concurrent.*;

public class LKW {

    private String thingId;
    private LKWStatus lkwStatus;
    private double weight;
    private double velocity;
    private double tirePressure;
    private double progress;
    private double fuel;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public LKW(){
        setStarterVaules();

    }

    public String getThingId() {
        return thingId;
    }

    public void setThingId(String thingId) {
        this.thingId = thingId;
    }

    public LKWStatus getStatus() {
        return lkwStatus;
    }

    public void setStatus(LKWStatus status) {
        this.lkwStatus = status;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getVelocity() {
        return velocity;
    }

    public void setVelocity(double velocity) {
        this.velocity = velocity;
    }

    public double getTirePressure() {
        return tirePressure;
    }

    public void setTirePressure(double tirePressure) {
        this.tirePressure = tirePressure;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public void setFuel(double fuel) {
        this.fuel = fuel;
    }

    public double getFuel() {
        return fuel;
    }

    public void setStarterVaules(){
        setThingId("mytest:LKW-1");
        setStatus(LKWStatus.IDLE);
        setWeight(7500);
        setVelocity(0);
        setTirePressure(7000);
        setProgress(0);
        setFuel(50);
    }

    public void featureSimulation(){
        double maxProgress = 100.0;

        scheduler.scheduleAtFixedRate(() -> {
            double currentFuelTank = getFuel();
            double currentVelocity = getVelocity();
            double currentProgress = getProgress();

            if(currentProgress > maxProgress || currentFuelTank <= 0){
                setVelocity(0);
                System.out.println("Fahrt Beendet");
                scheduler.shutdown();
            }

            System.out.println("fahrt läuft");
            System.out.println(currentProgress);
            System.out.println(currentFuelTank);
            setVelocity(75 + Math.random() * 10);
            setFuel(currentFuelTank - 0.5);
            setProgress(currentProgress + 5);


        }, 0, 3, TimeUnit.SECONDS);

    }
}