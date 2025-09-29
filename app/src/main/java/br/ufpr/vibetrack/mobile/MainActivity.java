package br.ufpr.vibetrack.mobile;

import android.Manifest;
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

public class MainActivity extends AppCompatActivity {

    private static final int SENSOR_PERMISSION_REQUEST_CODE = 101;
    private TextView permissionStatusTextView;
    private Button requestPermissionButton;

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

        // Configura o listener do botão
        requestPermissionButton.setOnClickListener(v -> requestSensorPermission());

        // Verifica o status da permissão ao iniciar a tela
        checkSensorPermission();
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