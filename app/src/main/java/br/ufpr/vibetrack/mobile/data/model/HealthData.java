package br.ufpr.vibetrack.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class HealthData {

    @SerializedName("steps")
    private int steps;

    @SerializedName("heartRate")
    private HeartRate heartRate;

    // Construtor, Getters e Setters
    public HealthData(int steps, HeartRate heartRate) {
        this.steps = steps;
        this.heartRate = heartRate;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public HeartRate getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(HeartRate heartRate) {
        this.heartRate = heartRate;
    }
}