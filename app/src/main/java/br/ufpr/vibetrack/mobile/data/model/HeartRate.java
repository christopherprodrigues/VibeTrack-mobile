package br.ufpr.vibetrack.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class HeartRate {

    @SerializedName("resting")
    private int resting;

    @SerializedName("average")
    private int average;

    @SerializedName("max")
    private int max;

    // Construtor, Getters e Setters
    public HeartRate(int resting, int average, int max) {
        this.resting = resting;
        this.average = average;
        this.max = max;
    }

    public HeartRate(long l, int average) {
    }

    public int getResting() {
        return resting;
    }

    public void setResting(int resting) {
        this.resting = resting;
    }

    public int getAverage() {
        return average;
    }

    public void setAverage(int average) {
        this.average = average;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }
}