package br.ufpr.vibetrack.mobile.service;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import br.ufpr.vibetrack.mobile.data.model.ExperimentResult;
import br.ufpr.vibetrack.mobile.data.remote.ApiClient;
import br.ufpr.vibetrack.mobile.data.remote.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DataLayerListenerService extends WearableListenerService {

    private static final String TAG = "DataLayerListener";
    private static final String EXPERIMENT_DATA_PATH = "/experiment-data";

    public static final String ACTION_SYNC_RESULT = "br.ufpr.vibetrack.mobile.SYNC_RESULT";
    public static final String EXTRA_SYNC_SUCCESS = "EXTRA_SYNC_SUCCESS";
    public static final String EXTRA_SYNC_MESSAGE = "EXTRA_SYNC_MESSAGE";


    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);

        if (messageEvent.getPath().equals(EXPERIMENT_DATA_PATH)) {
            final String jsonMessage = new String(messageEvent.getData());
            Log.d(TAG, "Mensagem JSON recebida do smartwatch: " + jsonMessage);

            Gson gson = new Gson();
            ExperimentResult result = gson.fromJson(jsonMessage, ExperimentResult.class);

            sendDataToBackend(result);
        }
    }

    private void sendDataToBackend(ExperimentResult result) {
        ApiService apiService = ApiClient.getApiService();
        Call<Void> call = apiService.submitExperimentResult(result);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                String message;
                if (response.isSuccessful()) {
                    message = "Último envio: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
                    sendSyncResultBroadcast(true, message);
                } else {
                    message = "Falha no último envio (Código: " + response.code() + ")";
                    sendSyncResultBroadcast(false, message);
                }
                Log.d(TAG, message);
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                String message = "Falha no último envio (Erro de rede)";
                sendSyncResultBroadcast(false, message);
                Log.e(TAG, message, t);
            }
        });
    }

    private void sendSyncResultBroadcast(boolean success, String message) {
        Intent intent = new Intent(ACTION_SYNC_RESULT);
        intent.putExtra(EXTRA_SYNC_SUCCESS, success);
        intent.putExtra(EXTRA_SYNC_MESSAGE, message);
        sendBroadcast(intent);
    }
}