package br.ufpr.vibetrack.mobile.service;

import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

    // AÇÕES DE BROADCAST PARA A MAINACTIVITY
    public static final String ACTION_SYNC_RESULT = "br.ufpr.vibetrack.mobile.SYNC_RESULT";
    public static final String EXTRA_SYNC_SUCCESS = "EXTRA_SYNC_SUCCESS";
    public static final String EXTRA_SYNC_MESSAGE = "EXTRA_SYNC_MESSAGE";

    // --- INÍCIO DA ATUALIZAÇÃO ---
    // Nova ação para enviar o JSON bruto para a UI
    public static final String ACTION_DATA_RECEIVED = "br.ufpr.vibetrack.mobile.DATA_RECEIVED";
    public static final String EXTRA_JSON_DATA = "EXTRA_JSON_DATA";
    // --- FIM DA ATUALIZAÇÃO ---


    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);

        if (messageEvent.getPath().equals(EXPERIMENT_DATA_PATH)) {
            final String jsonMessage = new String(messageEvent.getData());
            Log.d(TAG, "Mensagem JSON recebida do smartwatch: " + jsonMessage);

            // --- INÍCIO DA ATUALIZAÇÃO ---
            // Envia o JSON bruto para a MainActivity (para visualização imediata)
            Intent dataIntent = new Intent(ACTION_DATA_RECEIVED);
            dataIntent.putExtra(EXTRA_JSON_DATA, jsonMessage);
            sendBroadcast(dataIntent);
            // --- FIM DA ATUALIZAÇÃO ---


            // --- LÓGICA DE STATUS ATUALIZADA ---

            // 1. Buscar o User ID salvo no celular
            SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
            String userId = prefs.getString(MainActivity.KEY_USER_ID, null);

            if (userId == null || userId.isEmpty()) {
                Log.e(TAG, "Recebidos dados do relógio, mas o celular não está pareado! (userId é nulo)");
                // MENSAGEM ATUALIZADA
                sendSyncResultBroadcast(false, "Falha: Dados recebidos do relógio, mas o celular não está pareado. Abra o app e insira o código.");
                return;
            }

            // 2. Deserializar a MENSAGEM (que agora é HealthData)
            Gson gson = new Gson();
            HealthData healthData;
            try {
                healthData = gson.fromJson(jsonMessage, HealthData.class);
                if (healthData == null) {
                    throw new Exception("Objeto HealthData nulo após deserialização.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Falha ao deserializar JSON do relógio: " + jsonMessage, e);
                // MENSAGEM ATUALIZADA
                sendSyncResultBroadcast(false, "Falha: Recebidos dados inválidos do relógio.");
                return;
            }


            // 3. Montar o ExperimentResult COMPLETO aqui
            String currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date());

            ExperimentResult result = new ExperimentResult(
                    userId,
                    "VibeTrack Smartwatch", // Nome do dispositivo
                    currentDate,
                    healthData
            );

            // 4. Enviar para o Backend
            sendDataToBackend(result);
            // --- FIM DA ATUALIZAÇÃO ---
        }
    }

    private void sendDataToBackend(ExperimentResult result) {
        ApiService apiService = ApiClient.getApiService();
        Call<Void> call = apiService.submitExperimentResult(result);

        Log.d(TAG, "Enviando para o Backend (Usuário: " + result.getUserId() + "): " + new Gson().toJson(result));

        // Envia uma mensagem IMEDIATA para a UI, mostrando que os dados foram recebidos.
        String timeNow = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        sendSyncResultBroadcast(true, "Dados recebidos do relógio às " + timeNow + ". Enviando ao servidor...");


        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                String message;
                String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

                if (response.isSuccessful()) {
                    // MENSAGEM ATUALIZADA
                    message = "Sucesso! Dados enviados ao servidor às " + timestamp + ".";
                    sendSyncResultBroadcast(true, message);
                } else {
                    // MENSAGEM ATUALIZADA
                    message = "Falha: Servidor rejeitou os dados (Código: " + response.code() + ") às " + timestamp + ". (Verifique a segurança/autenticação do backend)";
                    sendSyncResultBroadcast(false, message);
                }
                Log.d(TAG, message);
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                // MENSAGEM ATUALIZADA
                String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                String error = t.getMessage() != null ? t.getMessage() : "Erro desconhecido";
                String message = "Falha: Erro de rede às " + timestamp + ". O servidor está offline ou o IP/porta está errado.\nDetalhe: " + error;
                sendSyncResultBroadcast(false, message);
                Log.e(TAG, message, t);
            }
        });
    }

    /**
     * Envia o RESULTADO (Sucesso/Falha) da sincronia com o backend para a MainActivity.
     */
    private void sendSyncResultBroadcast(boolean success, String message) {
        Intent intent = new Intent(ACTION_SYNC_RESULT);
        intent.putExtra(EXTRA_SYNC_SUCCESS, success);
        intent.putExtra(EXTRA_SYNC_MESSAGE, message);
        sendBroadcast(intent);
    }
}