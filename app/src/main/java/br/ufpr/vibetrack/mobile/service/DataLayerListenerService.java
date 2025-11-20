package br.ufpr.vibetrack.mobile.service;

import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import br.ufpr.vibetrack.mobile.MainActivity;
import br.ufpr.vibetrack.mobile.data.model.ExperimentResult;
import br.ufpr.vibetrack.mobile.data.model.HealthData;
import br.ufpr.vibetrack.mobile.data.remote.ApiClient;
import br.ufpr.vibetrack.mobile.data.remote.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DataLayerListenerService extends WearableListenerService {

    private static final String TAG = "DataLayerListener";
    private static final String EXPERIMENT_DATA_PATH = "/experiment-data";

    public static final String ACTION_SYNC_STATUS = "br.ufpr.vibetrack.mobile.SYNC_STATUS";
    public static final String EXTRA_STATUS_MESSAGE = "status_message";

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        if (messageEvent.getPath().equals("/experiment-data")) {
            String json = new String(messageEvent.getData(), StandardCharsets.UTF_8);
            Log.d(TAG, "Mensagem JSON recebida do smartwatch: " + json);
            sendBroadcastMessage("Recebido do relógio: " + json);

            // 1. Pegar o User ID salvo no celular
            SharedPreferences prefs = getSharedPreferences("VibeTrackPrefs", MODE_PRIVATE);
            String userId = prefs.getString("user_id", null);

            if (userId == null) {
                Log.e(TAG, "Recebidos dados do relógio, mas o celular não está pareado! (userId é nulo)");
                sendBroadcastMessage("Falha: Dados recebidos, mas o celular não está pareado. Abra o app e insira o código.");
                return;
            }

            try {
                Gson gson = new Gson();
                HealthData healthData = gson.fromJson(json, HealthData.class);

                // 2. Agora isso funciona (graças à Correção 1)
                ExperimentResult result = new ExperimentResult();
                result.setUserId(userId);
                result.setDate(new Date()); // Agora isso funciona (graças à Correção 1)
                result.setDevice("Smartwatch (Wear OS)");
                result.setHealthData(healthData);

                sendDataToBackend(result);

            } catch (JsonSyntaxException e) {
                Log.e(TAG, "Erro ao desserializar JSON: " + json, e);
                sendBroadcastMessage("Erro: JSON inválido recebido do relógio.");
            }
        } else {
            super.onMessageReceived(messageEvent);
        }
    }

    private void sendDataToBackend(ExperimentResult result) {
        // 3. CORRIGIDO: Usando getApiService()
        ApiService apiService = ApiClient.getApiService();
        // 4. CORRIGIDO: Usando submitExperimentResult()
        Call<Void> call = apiService.submitExperimentResult(result);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Sucesso! Dados enviados ao servidor.");
                    sendBroadcastMessage("Sucesso: Dados enviados ao servidor!");
                } else {
                    Log.e(TAG, "Falha: Servidor rejeitou os dados. Código: " + response.code());
                    sendBroadcastMessage("Falha: Servidor rejeitou os dados. Código: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "Erro de rede ao enviar dados: ", t);
                sendBroadcastMessage("Erro de Rede: " + t.getMessage());
            }
        });
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "DataLayerListenerService criado");
    }


    private void sendBroadcastMessage(String message) {
        Intent intent = new Intent(ACTION_SYNC_STATUS);
        intent.putExtra(EXTRA_STATUS_MESSAGE, message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}