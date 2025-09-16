package br.ufpr.vibetrack.mobile.service;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class DataLayerListenerService extends WearableListenerService {

    private static final String TAG = "DataLayerListener";
    private static final String EXPERIMENT_DATA_PATH = "/experiment-data";

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);

        // Verifica se a mensagem recebida é a que esperamos (dados do experimento)
        if (messageEvent.getPath().equals(EXPERIMENT_DATA_PATH)) {
            // A mensagem chega como um array de bytes, convertemos para String (JSON)
            final String message = new String(messageEvent.getData());

            // Usamos o Logcat para depurar e ver a mensagem recebida
            Log.d(TAG, "Mensagem recebida do smartwatch: " + message);

            // TODO: Nas próximas fases, aqui vamos processar o JSON e enviar para o backend.
        }
    }
}