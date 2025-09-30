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

import br.ufpr.vibetrack.mobile.service.DataLayerListenerService;

public class MainActivity extends AppCompatActivity {

    private static final int SENSOR_PERMISSION_REQUEST_CODE = 101;
    private TextView permissionStatusTextView;
    private Button requestPermissionButton;
    private TextView syncStatusTextView;
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

        // Inicializa os componentes da UI
        permissionStatusTextView = findViewById(R.id.permissionStatusTextView);
        requestPermissionButton = findViewById(R.id.requestPermissionButton);
        syncStatusTextView = findViewById(R.id.syncStatusTextView);

        // Configura o listener do botão
        requestPermissionButton.setOnClickListener(v -> requestSensorPermission());

        // Verifica o status da permissão ao iniciar a tela
        checkSensorPermission();

        // Configura o "receptor" para a mensagem do serviço
        syncResultReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null && DataLayerListenerService.ACTION_SYNC_RESULT.equals(intent.getAction())) {
                    boolean success = intent.getBooleanExtra(DataLayerListenerService.EXTRA_SYNC_SUCCESS, false);
                    String message = intent.getStringExtra(DataLayerListenerService.EXTRA_SYNC_MESSAGE);

                    syncStatusTextView.setText(message);
                    if (success) {
                        syncStatusTextView.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
                    } else {
                        syncStatusTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
                    }
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Registra o receiver para ouvir as mensagens quando a tela está visível
        registerReceiver(syncResultReceiver, new IntentFilter(DataLayerListenerService.ACTION_SYNC_RESULT), RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Desregistra o receiver quando a tela não está mais visível para evitar vazamentos de memória
        unregisterReceiver(syncResultReceiver);
    }

    private void checkSensorPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                == PackageManager.PERMISSION_GRANTED) {
            // Permissão já concedida
            permissionStatusTextView.setText("Status da Permissão de Sensores: CONCEDIDA");
            permissionStatusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            requestPermissionButton.setVisibility(View.GONE);
        } else {
            // Permissão não concedida
            permissionStatusTextView.setText("Status da Permissão de Sensores: NEGADA\n(Necessária para a coleta de dados)");
            permissionStatusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            requestPermissionButton.setVisibility(View.VISIBLE);
        }
    }

    private void requestSensorPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.BODY_SENSORS},
                SENSOR_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == SENSOR_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão foi concedida pelo usuário
                Toast.makeText(this, "Permissão de sensores concedida!", Toast.LENGTH_SHORT).show();
                checkSensorPermission(); // Re-verifica para atualizar a UI
            } else {
                // Permissão foi negada pelo usuário
                Toast.makeText(this, "Permissão de sensores negada. O aplicativo não pode coletar dados do smartwatch.", Toast.LENGTH_LONG).show();
            }
        }
    }
}