package br.ufpr.vibetrack.mobile.data.model;

import com.google.gson.annotations.SerializedName;

public class ExperimentResult {

    @SerializedName("userId")
    private String userId;

    @SerializedName("device")
    private String device;

    @SerializedName("date")
    private String date;

    @SerializedName("healthData")
    private HealthData healthData;

    // Construtor, Getters e Setters
    public ExperimentResult(String userId, String device, String date, HealthData healthData) {
        this.userId = userId;
        this.device = device;
        this.date = date;
        this.healthData = healthData;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public HealthData getHealthData() {
        return healthData;
    }

    public void setHealthData(HealthData healthData) {
        this.healthData = healthData;
    }
}