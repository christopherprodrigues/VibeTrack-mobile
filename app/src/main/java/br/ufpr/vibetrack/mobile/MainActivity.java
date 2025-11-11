package br.ufpr.vibetrack.mobile;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences; // Import necessário
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log; // Import necessário
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
    private static final int SENSOR_PERMISSION_REQUEST_CODE = 101;

    // Constantes para SharedPreferences (usadas pelo Service)
    public static final String PREFS_NAME = "VibeTrackPrefs";
    public static final String KEY_USER_ID = "UserID";

    // --- Componentes da UI ---
    private TextView permissionStatusTextView;
    private Button requestPermissionButton;
    private TextView syncStatusTextView;
    private Button sendMockDataButton;
    private Button resetButton;
    private EditText pairingCodeEditText;
    private Button pairButton;

    // --- Variáveis de Lógica ---
    private SharedPreferences sharedPreferences;
    private BroadcastReceiver syncResultReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializa o SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // --- Inicialização dos componentes da UI ---
        permissionStatusTextView = findViewById(R.id.permissionStatusTextView);
        requestPermissionButton = findViewById(R.id.requestPermissionButton);
        syncStatusTextView = findViewById(R.id.syncStatusTextView);
        sendMockDataButton = findViewById(R.id.sendMockDataButton);
        pairingCodeEditText = findViewById(R.id.pairingCodeEditText);
        pairButton = findViewById(R.id.pairButton);
        resetButton = findViewById(R.id.resetButton);

        // --- Configuração dos Listeners dos Botões ---
        requestPermissionButton.setOnClickListener(v -> requestSensorPermission());
        sendMockDataButton.setOnClickListener(v -> sendMockDataToServer());

        // Listener para o botão de parear
        pairButton.setOnClickListener(v -> {
            String pairingCode = pairingCodeEditText.getText().toString().trim();
            if (pairingCode.isEmpty()) {
                Toast.makeText(this, "Por favor, digite um código.", Toast.LENGTH_SHORT).show();
            } else {
                // Salva o ID no SharedPreferences para o Service usar
                saveUserId(pairingCode);

                // Atualiza a UI
                Toast.makeText(this, "Dispositivo pareado com o código: " + pairingCode, Toast.LENGTH_LONG).show();
                pairingCodeEditText.setEnabled(false);
                pairButton.setText("Pareado");
                pairButton.setEnabled(false);
                resetButton.setVisibility(View.VISIBLE);
            }
        });

        // Listener para o botão de reset
        resetButton.setOnClickListener(v -> {
            // Limpa o ID do SharedPreferences
            saveUserId(null);

            // Reseta a UI
            pairingCodeEditText.setText("");
            pairingCodeEditText.setEnabled(true);
            pairButton.setEnabled(true);
            pairButton.setText("Parear com o Sistema");
            resetButton.setVisibility(View.GONE);
            syncStatusTextView.setText("Aguardando dados do relógio...");
            syncStatusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            Toast.makeText(this, "Pareamento resetado. Pode inserir um novo código.", Toast.LENGTH_LONG).show();
        });

        // --- Lógica de Inicialização ---
        loadAndDisplayUserId(); // Carrega o ID salvo
        checkSensorPermission(); // Verifica permissões

        // Configura o "receptor" para as mensagens do serviço
        // Este receiver agora trata DUAS ações
        syncResultReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || intent.getAction() == null) return;

                String action = intent.getAction();

                if (DataLayerListenerService.ACTION_DATA_RECEIVED.equals(action)) {
                    // Ação 1: Mostrar o JSON bruto recebido
                    String json = intent.getStringExtra(DataLayerListenerService.EXTRA_JSON_DATA);
                    if (json != null) {
                        Log.d(TAG, "Broadcast de DADOS recebido: " + json);
                        // Exibe o JSON na tela
                        syncStatusTextView.setText("Dados Brutos Recebidos:\n" + json);
                        syncStatusTextView.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
                    }
                } else if (DataLayerListenerService.ACTION_SYNC_RESULT.equals(action)) {
                    // Ação 2: Mostrar a mensagem de STATUS (sucesso/falha)
                    boolean success = intent.getBooleanExtra(DataLayerListenerService.EXTRA_SYNC_SUCCESS, false);
                    String message = intent.getStringExtra(DataLayerListenerService.EXTRA_SYNC_MESSAGE);
                    if (message != null) {
                        Log.d(TAG, "Broadcast de STATUS recebido: " + message);
                        // Exibe o status na tela
                        syncStatusTextView.setText(message);
                        syncStatusTextView.setTextColor(ContextCompat.getColor(context, success ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
                    }
                }
            }
        };
    }

    /**
     * Salva o ID do Usuário no SharedPreferences.
     * O Service (DataLayerListenerService) lerá este valor.
     */
    private void saveUserId(String userId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (userId != null && !userId.isEmpty()) {
            editor.putString(KEY_USER_ID, userId);
            Log.i(TAG, "ID do Usuário salvo: " + userId);
        } else {
            editor.remove(KEY_USER_ID); // Remove o ID
            Log.i(TAG, "ID do Usuário removido.");
        }
        editor.apply();
    }

    /**
     * Carrega o ID do SharedPreferences e atualiza a UI no início.
     */
    private void loadAndDisplayUserId() {
        String userId = sharedPreferences.getString(KEY_USER_ID, null);
        if (userId != null && !userId.isEmpty()) {
            pairingCodeEditText.setText(userId);
            pairingCodeEditText.setEnabled(false);
            pairButton.setText("Pareado");
            pairButton.setEnabled(false);
            resetButton.setVisibility(View.VISIBLE);
        } else {
            // Garante que o estado inicial esteja limpo
            pairingCodeEditText.setText("");
            pairingCodeEditText.setEnabled(true);
            pairButton.setText("Parear com o Sistema");
            pairButton.setEnabled(true);
            resetButton.setVisibility(View.GONE);
            syncStatusTextView.setText("Dispositivo não pareado. Insira o código.");
            syncStatusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        }
    }


    /**
     * Envia dados FALSOS (mock) para o servidor, usando o ID salvo.
     */
    private void sendMockDataToServer() {
        // Lê o ID do SharedPreferences
        String currentUserId = sharedPreferences.getString(KEY_USER_ID, null);

        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "É necessário parear o dispositivo primeiro!", Toast.LENGTH_LONG).show();
            return;
        }

        // Cria dados mock
        HeartRate mockHeartRate = new HeartRate(75, 85, 120);
        HealthData mockHealthData = new HealthData(500, mockHeartRate);
        ExperimentResult mockResult = new ExperimentResult(
                currentUserId,
                "Teste direto do App Mobile",
                "2025-10-17T10:00:00Z",
                mockHealthData
        );

        ApiService apiService = ApiClient.getApiService();
        Toast.makeText(this, "Enviando dados de teste para o usuário " + currentUserId + "...", Toast.LENGTH_SHORT).show();

        apiService.submitExperimentResult(mockResult).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                String message;
                if (response.isSuccessful()) {
                    message = "Sucesso: Dados de teste enviados! (Código: " + response.code() + ")";
                    syncStatusTextView.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_green_dark));
                } else {
                    message = "Falha: O servidor respondeu com um erro. (Código: " + response.code() + ")";
                    syncStatusTextView.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_red_dark));
                }
                syncStatusTextView.setText(message);
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                String message = "Erro de Rede: Não foi possível conectar ao servidor.";
                syncStatusTextView.setText(message + "\nDetalhe: " + t.getMessage());
                syncStatusTextView.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_red_dark));
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // CORREÇÃO: Cria um filtro para AMBAS as ações
        IntentFilter filter = new IntentFilter();
        filter.addAction(DataLayerListenerService.ACTION_SYNC_RESULT);
        filter.addAction(DataLayerListenerService.ACTION_DATA_RECEIVED);

        // Registra o receiver com o filtro e a flag de segurança
        registerReceiver(syncResultReceiver, filter, RECEIVER_NOT_EXPORTED);
        Log.d(TAG, "BroadcastReceiver registrado para 2 ações.");
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Cancela o registro do receiver
        unregisterReceiver(syncResultReceiver);
        Log.d(TAG, "BroadcastReceiver cancelado.");
    }

    // --- Métodos de Permissão (sem alteração) ---

    private void checkSensorPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED) {
            permissionStatusTextView.setText("Status da Permissão de Sensores: CONCEDIDA");
            permissionStatusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            requestPermissionButton.setVisibility(View.GONE);
        } else {
            permissionStatusTextView.setText("Status da Permissão de Sensores: NEGADA\n(Necessária para a coleta de dados)");
            permissionStatusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            requestPermissionButton.setVisibility(View.VISIBLE);
        }
    }

    private void requestSensorPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BODY_SENSORS}, SENSOR_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SENSOR_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissão de sensores concedida!", Toast.LENGTH_SHORT).show();
                checkSensorPermission();
            } else {
                Toast.makeText(this, "Permissão de sensores negada. O aplicativo não pode coletar dados do smartwatch.", Toast.LENGTH_LONG).show();
            }
        }
    }
}