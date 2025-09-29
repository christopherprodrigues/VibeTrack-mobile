package br.ufpr.vibetrack.mobile.service;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.gson.Gson;

import br.ufpr.vibetrack.mobile.data.model.ExperimentResult;
import br.ufpr.vibetrack.mobile.data.remote.ApiClient;
import br.ufpr.vibetrack.mobile.data.remote.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DataLayerListenerService extends WearableListenerService {

    private static final String TAG = "DataLayerListener";
    private static final String EXPERIMENT_DATA_PATH = "/experiment-data";

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);

        if (messageEvent.getPath().equals(EXPERIMENT_DATA_PATH)) {
            final String jsonMessage = new String(messageEvent.getData());
            Log.d(TAG, "Mensagem JSON recebida do smartwatch: " + jsonMessage);

            // 1. Converter o JSON para nosso objeto Java
            Gson gson = new Gson();
            ExperimentResult result = gson.fromJson(jsonMessage, ExperimentResult.class);

            // 2. Enviar o objeto para o backend
            sendDataToBackend(result);
        }
    }

    private void sendDataToBackend(ExperimentResult result) {
        ApiService apiService = ApiClient.getApiService();
        Call<Void> call = apiService.submitExperimentResult(result);

        // A chamada de rede é assíncrona. Usamos enqueue para executá-la
        // em uma thread de fundo e receber o resultado em um callback.
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Dados enviados com sucesso para o backend! Código: " + response.code());
                } else {
                    Log.e(TAG, "Erro ao enviar dados. Código: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "Falha crítica na chamada de rede: ", t);
            }
        });
    }
}