package br.ufpr.vibetrack.mobile;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
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

    private static final int SENSOR_PERMISSION_REQUEST_CODE = 101;
    private TextView permissionStatusTextView;
    private Button requestPermissionButton;
    private TextView syncStatusTextView;
    private BroadcastReceiver syncResultReceiver;
    private Button sendMockDataButton;
    private Button resetButton;

    // Variáveis para a funcionalidade de pareamento
    private EditText pairingCodeEditText;
    private Button pairButton;
    private String currentUserId = null;

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

        // --- Inicialização dos componentes da UI ---
        permissionStatusTextView = findViewById(R.id.permissionStatusTextView);
        requestPermissionButton = findViewById(R.id.requestPermissionButton);
        syncStatusTextView = findViewById(R.id.syncStatusTextView);
        sendMockDataButton = findViewById(R.id.sendMockDataButton);

        // --- Componentes do Pareamento ---
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
                this.currentUserId = pairingCode;
                Toast.makeText(this, "Dispositivo pareado com o código: " + this.currentUserId, Toast.LENGTH_LONG).show();
                pairingCodeEditText.setEnabled(false);
                pairButton.setText("Pareado");
                pairButton.setEnabled(false);
                resetButton.setVisibility(View.VISIBLE);
            }
        });

        // Listener para o botão de reset
        resetButton.setOnClickListener(v -> {
            // Limpa o ID e reativa os campos
            currentUserId = null;
            pairingCodeEditText.setText("");
            pairingCodeEditText.setEnabled(true);
            pairButton.setEnabled(true);
            pairButton.setText("Parear com o Sistema");
            resetButton.setVisibility(View.GONE); // <-- 4. ESCONDER O BOTÃO DE RESET
            Toast.makeText(this, "Pareamento resetado. Pode inserir um novo código.", Toast.LENGTH_LONG).show();
        });



        checkSensorPermission();

        // Configura o "receptor" para a mensagem do serviço
        syncResultReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null && DataLayerListenerService.ACTION_SYNC_RESULT.equals(intent.getAction())) {
                    boolean success = intent.getBooleanExtra(DataLayerListenerService.EXTRA_SYNC_SUCCESS, false);
                    String message = intent.getStringExtra(DataLayerListenerService.EXTRA_SYNC_MESSAGE);
                    syncStatusTextView.setText(message);
                    syncStatusTextView.setTextColor(ContextCompat.getColor(context, success ? android.R.color.darker_gray : android.R.color.holo_red_dark));
                }
            }
        };
    }

    private void sendMockDataToServer() {
        // Verifica se o dispositivo foi pareado antes de enviar
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "É necessário parear o dispositivo primeiro!", Toast.LENGTH_LONG).show();
            return;
        }

        HeartRate mockHeartRate = new HeartRate(75, 85, 120);
        HealthData mockHealthData = new HealthData(500, mockHeartRate);

        // Usa o ID do usuário/código inserido
        ExperimentResult mockResult = new ExperimentResult(
                this.currentUserId,
                "Teste direto do App Mobile",
                "2025-10-17T10:00:00Z",
                mockHealthData
        );

        ApiService apiService = ApiClient.getApiService();
        Toast.makeText(this, "Enviando dados de teste para o usuário " + this.currentUserId + "...", Toast.LENGTH_SHORT).show();

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
        registerReceiver(syncResultReceiver, new IntentFilter(DataLayerListenerService.ACTION_SYNC_RESULT), RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(syncResultReceiver);
    }

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