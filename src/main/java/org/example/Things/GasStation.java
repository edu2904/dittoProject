package org.example.Things;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GasStation {
    private String thingId;
    private GasStationStatus gasStationStatus;
    private double fuelAmount;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public GasStation(){
        setStarterValues();

    }

    public String getThingId() {
        return thingId;
    }

    public void setThingId(String thingId) {
        this.thingId = thingId;
    }

    public GasStationStatus getGasStationStatus() {
        return gasStationStatus;
    }

    public void setGasStationStatus(GasStationStatus gasStationStatus) {
        this.gasStationStatus = gasStationStatus;
    }

    public double getFuelAmount() {
        return fuelAmount;
    }

    public void setFuelAmount(double fuelAmount) {
        this.fuelAmount = fuelAmount;
    }
    public void setStarterValues(){
        setThingId("mything:GasStation-1");
        setGasStationStatus(GasStationStatus.WAITING);
        setFuelAmount(3000);
    }

    public  void featureSimulation(){
        scheduler.scheduleAtFixedRate(() -> {
            double currentFuelAmount = getFuelAmount();
            GasStationStatus currentGasStationStatus = getGasStationStatus();

            if (currentGasStationStatus == GasStationStatus.WAITING) {
                setFuelAmount(currentFuelAmount);
            }
        }, 0, 3, TimeUnit.SECONDS);
    }
}
