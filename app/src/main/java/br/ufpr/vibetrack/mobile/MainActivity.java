package br.ufpr.vibetrack.mobile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import br.ufpr.vibetrack.mobile.data.model.ExperimentResult;
import br.ufpr.vibetrack.mobile.data.model.HealthData;
import br.ufpr.vibetrack.mobile.data.model.HeartRate;
import br.ufpr.vibetrack.mobile.data.remote.ApiClient;
import br.ufpr.vibetrack.mobile.data.remote.ApiService;
import br.ufpr.vibetrack.mobile.service.DataLayerListenerService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private SharedPreferences prefs;
    private static final String PREF_USER_ID = "user_id";

    private TextView syncStatusTextView;
    private EditText pairingCodeEditText;
    private Button pairButton;
    private Button resetButton;
    private Button sendMockDataButton;
    private String currentUserId;
    private BroadcastReceiver statusReceiver;

    private TextView txtHeartRate, txtSteps, connectionStatusText;
    private View pairingLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        syncStatusTextView = findViewById(R.id.syncStatusTextView);
        pairingCodeEditText = findViewById(R.id.pairingCodeEditText);
        pairButton = findViewById(R.id.pairButton);
        resetButton = findViewById(R.id.resetButton);
        sendMockDataButton = findViewById(R.id.sendMockDataButton);
        txtHeartRate = findViewById(R.id.txtHeartRate);
        txtSteps = findViewById(R.id.txtSteps);
        connectionStatusText = findViewById(R.id.connectionStatusText);
        pairingLayout = findViewById(R.id.pairingLayout);

        prefs = getSharedPreferences("VibeTrackPrefs", MODE_PRIVATE);
        checkPairingStatus();

        pairButton.setOnClickListener(v -> pairWithSystem());
        resetButton.setOnClickListener(v -> resetPairing());
        sendMockDataButton.setOnClickListener(v -> sendMockData());

        statusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null && DataLayerListenerService.ACTION_SYNC_STATUS.equals(intent.getAction())) {
                    String message = intent.getStringExtra(DataLayerListenerService.EXTRA_STATUS_MESSAGE);

                    // Atualiza o Log (Terminal)
                    logMessage(message);

                    // Tenta atualizar o Dashboard se for uma mensagem JSON do relógio
                    if (message.contains("Recebido do relógio:")) {
                        try {
                            // Pega só a parte do JSON (depois dos dois pontos)
                            String jsonPart = message.substring(message.indexOf("{"));

                            // Usa o Gson (você já tem importado) para facilitar a leitura
                            com.google.gson.Gson gson = new com.google.gson.Gson();
                            HealthData data = gson.fromJson(jsonPart, HealthData.class);

                            if (data != null) {
                                txtSteps.setText(String.valueOf(data.getSteps()));
                                if (data.getHeartRate() != null) {
                                    txtHeartRate.setText(String.valueOf(data.getHeartRate().getAverage()));
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Erro ao atualizar dashboard: " + e.getMessage());
                        }
                    }
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(DataLayerListenerService.ACTION_SYNC_STATUS);
        LocalBroadcastManager.getInstance(this).registerReceiver(statusReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(statusReceiver);
    }

    private void checkPairingStatus() {
        currentUserId = prefs.getString(PREF_USER_ID, null);
        if (currentUserId != null) {
            // PAREADO
            pairingLayout.setVisibility(View.GONE);
            resetButton.setVisibility(View.VISIBLE);

            connectionStatusText.setText("● Conectado: " + currentUserId);
            // Usando a cor verde definida no colors.xml
            connectionStatusText.setTextColor(getResources().getColor(R.color.vibe_status_connected, getTheme()));
        } else {
            // NÃO PAREADO
            pairingLayout.setVisibility(View.VISIBLE);
            resetButton.setVisibility(View.GONE);

            connectionStatusText.setText("● Aguardando conexão");
            // Usando a cor vermelha definida no colors.xml
            connectionStatusText.setTextColor(getResources().getColor(R.color.vibe_status_disconnected, getTheme()));
        }
    }

    private void pairWithSystem() {
        String code = pairingCodeEditText.getText().toString();
        if (code.isEmpty()) {
            Toast.makeText(this, "Por favor, insira um código", Toast.LENGTH_SHORT).show();
            return;
        }
        currentUserId = code;
        prefs.edit().putString(PREF_USER_ID, currentUserId).apply();
        logMessage("Dispositivo pareado com ID: " + currentUserId);
        checkPairingStatus();
    }

    private void resetPairing() {
        prefs.edit().remove(PREF_USER_ID).apply();
        currentUserId = null;
        logMessage("Pareamento resetado.");
        checkPairingStatus();
    }

    private void sendMockData() {
        if (currentUserId == null) {
            logMessage("Erro: Dispositivo não pareado. Pareie antes de enviar dados.");
            return;
        }
        logMessage("Enviando dados de teste (mock)...");

        HeartRate heartRate = new HeartRate(60, 80, 120);
        HealthData healthData = new HealthData(150, heartRate);

        // CORRIGIDO: Usando o construtor vazio
        ExperimentResult result = new ExperimentResult();
        result.setUserId(currentUserId);
        result.setDate(new Date()); // CORRIGIDO: Usando o novo setter
        result.setDevice("Mobile (Mock Data)");
        result.setHealthData(healthData);

        // CORRIGIDO: Usando getApiService()
        ApiService apiService = ApiClient.getApiService();
        // CORRIGIDO: Usando submitExperimentResult()
        Call<Void> call = apiService.submitExperimentResult(result);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Sucesso! Dados mock enviados.");
                    logMessage("Sucesso: Dados mock enviados ao servidor!");
                } else {
                    Log.e(TAG, "Falha: Servidor rejeitou dados mock. Código: " + response.code());
                    logMessage("Falha: Servidor rejeitou dados mock. Código: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "Erro de rede ao enviar dados mock: ", t);
                logMessage("Erro de Rede (Mock): " + t.getMessage());
            }
        });
    }

    private void logMessage(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String oldText = syncStatusTextView.getText().toString();
        syncStatusTextView.setText(timestamp + ": " + message + "\n" + oldText);
    }
}