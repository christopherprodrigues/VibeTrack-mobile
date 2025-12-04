package br.ufpr.vibetrack.mobile.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.Date; // Importe a classe Date

public class ExperimentResult {

    @SerializedName("userId")
    private String userId;

    @SerializedName("device")
    private String device;

    @SerializedName("date")
    private String date; // Tinha um 's' antes, mas o setter é 'setDate(String date)'

    @SerializedName("healthData")
    private HealthData healthData;

    // Construtor original
    public ExperimentResult(String userId, String device, String s, HealthData healthData) {
        this.userId = userId;
        this.device = device;
        this.date = s; // Corrigido de 'date' para 's'
        this.healthData = healthData;
    }

    // ADICIONE ESTE CONSTRUTOR VAZIO:
    public ExperimentResult() {
    }

    // Getters e Setters
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

    // Crie um setter que aceita um objeto Date (facilita a vida)
    public void setDate(Date date) {
        if (date == null) {
            this.date = null;
        } else {
            // Formata a data para o padrão ISO 8601, que o backend espera
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            this.date = sdf.format(date);
        }
    }

    public HealthData getHealthData() {
        return healthData;
    }

    public void setHealthData(HealthData healthData) {
        this.healthData = healthData;
    }
}