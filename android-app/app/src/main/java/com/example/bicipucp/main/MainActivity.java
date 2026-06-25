package com.example.bicipucp.main;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bicipucp.R;
import com.example.bicipucp.auth.LoginActivity;
import com.example.bicipucp.carne.CarneActivity;
import com.example.bicipucp.model.UsuarioBici;
import com.example.bicipucp.network.ApiClient;
import com.example.bicipucp.network.DesbloqueoRequest;
import com.example.bicipucp.network.DesbloqueoResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BiciPUCP";
    private static final int SEGUNDOS_DE_VIDA = 120;

    private LinearLayout cardEstado;
    private TextView tvTituloEstado, tvCountdown, tvMensajePrincipal, tvMensajeSecundario, tvInfoUsuario;
    private MaterialButton btnSolicitarDesbloqueo;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private FirebaseUser usuarioActual;
    private UsuarioBici usuarioBici;

    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setTitle("BiciPUCP");

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        usuarioActual = firebaseAuth.getCurrentUser();

        if (usuarioActual == null) {
            irALogin();
            return;
        }

        cardEstado = findViewById(R.id.cardEstado);
        tvTituloEstado = findViewById(R.id.tvTituloEstado);
        tvCountdown = findViewById(R.id.tvCountdown);
        tvMensajePrincipal = findViewById(R.id.tvMensajePrincipal);
        tvMensajeSecundario = findViewById(R.id.tvMensajeSecundario);
        tvInfoUsuario = findViewById(R.id.tvInfoUsuario);
        btnSolicitarDesbloqueo = findViewById(R.id.btnSolicitarDesbloqueo);

        btnSolicitarDesbloqueo.setOnClickListener(v -> pedirPinYSolicitarDesbloqueo());

        cargarUsuarioDesdeFirestore();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_mi_carne) {
            startActivity(new Intent(this, CarneActivity.class));
            return true;
        } else if (id == R.id.action_cerrar_sesion) {
            firebaseAuth.signOut();
            irALogin();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void cargarUsuarioDesdeFirestore() {
        String uid = usuarioActual.getUid().trim();
        Log.d(TAG, "Cargando perfil de Firestore para uid=" + uid);

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener(snapshot -> {
                if (!snapshot.exists()) {
                    Snackbar.make(cardEstado, "No se encontró el perfil del usuario", Snackbar.LENGTH_LONG).show();
                    return;
                }
                usuarioBici = snapshot.toObject(UsuarioBici.class);
                if (usuarioBici == null) {
                    Snackbar.make(cardEstado, "Error al leer el perfil", Snackbar.LENGTH_LONG).show();
                    return;
                }
                Log.d(TAG, "Perfil cargado: " + usuarioBici.getNombre() + ", timestamp=" + usuarioBici.getTimestampAprobacion());
                tvInfoUsuario.setText("Token IoT: " + usuarioBici.getIotAuthToken());
                iniciarMaquinaDeEstados();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error al cargar perfil de Firestore", e);
                Snackbar.make(cardEstado, "Error de Firestore: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
            });
    }

    private void iniciarMaquinaDeEstados() {
        long segundosRestantes = calcularSegundosRestantes(usuarioBici.getTimestampAprobacion());
        Log.d(TAG, "Segundos restantes calculados: " + segundosRestantes);

        if (segundosRestantes <= 0) {
            mostrarEstadoExpirado();
        } else {
            mostrarEstadoActivo(segundosRestantes);
        }
    }

    private long calcularSegundosRestantes(String timestampIso) {
        try {
            LocalDateTime aprobacion = LocalDateTime.parse(timestampIso, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDateTime expiracion = aprobacion.plusSeconds(SEGUNDOS_DE_VIDA);
            LocalDateTime ahora = LocalDateTime.now();
            long restantes = ChronoUnit.SECONDS.between(ahora, expiracion);
            return Math.max(0, restantes);
        } catch (Exception e) {
            Log.e(TAG, "No se pudo parsear timestamp: " + timestampIso, e);
            return 0;
        }
    }

    private void mostrarEstadoActivo(long segundosIniciales) {
        cardEstado.setBackgroundResource(R.drawable.bg_card_green);
        tvTituloEstado.setText(R.string.estado_activo);
        tvTituloEstado.setTextColor(getResources().getColor(R.color.bici_green));
        tvCountdown.setTextColor(getResources().getColor(R.color.bici_green));
        tvMensajePrincipal.setText(R.string.texto_candado_energizado);
        tvMensajePrincipal.setTextColor(getResources().getColor(R.color.bici_green));
        tvMensajeSecundario.setText(R.string.texto_retire_bicicleta);
        tvMensajeSecundario.setVisibility(View.VISIBLE);
        btnSolicitarDesbloqueo.setVisibility(View.GONE);

        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(segundosIniciales * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                long segundos = millisUntilFinished / 1000;
                tvCountdown.setText(String.format("%ds", segundos));
            }

            @Override
            public void onFinish() {
                tvCountdown.setText("00s");
                mostrarEstadoExpirado();
            }
        };
        countDownTimer.start();
    }

    private void mostrarEstadoExpirado() {
        cardEstado.setBackgroundResource(R.drawable.bg_card_red);
        tvTituloEstado.setText(R.string.estado_expirado);
        tvTituloEstado.setTextColor(getResources().getColor(R.color.bici_red));
        tvCountdown.setText("00s");
        tvCountdown.setTextColor(getResources().getColor(R.color.bici_red));
        tvMensajePrincipal.setText(R.string.texto_candado_trabado);
        tvMensajePrincipal.setTextColor(getResources().getColor(R.color.bici_red));
        tvMensajeSecundario.setVisibility(View.GONE);
        btnSolicitarDesbloqueo.setVisibility(View.VISIBLE);
    }

    private void pedirPinYSolicitarDesbloqueo() {
        if (usuarioBici == null) {
            Snackbar.make(cardEstado, "Perfil no cargado todavía", Snackbar.LENGTH_SHORT).show();
            return;
        }

        TextInputEditText etPin = new TextInputEditText(this);
        etPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        etPin.setHint(getString(R.string.dialog_pin_hint));
        int padding = (int) (getResources().getDisplayMetrics().density * 24);
        etPin.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_titulo_pin)
            .setView(etPin)
            .setPositiveButton(R.string.dialog_btn_confirmar, (d, w) -> {
                String pin = etPin.getText() == null ? "" : etPin.getText().toString().trim();
                if (pin.length() != 4) {
                    Snackbar.make(cardEstado, "PIN inválido", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                reenergizarCandado(pin);
            })
            .setNegativeButton(R.string.dialog_btn_cancelar, null)
            .show();
    }

    private void reenergizarCandado(String pin) {
        Log.d(TAG, "Re-energizando con codigo=" + usuarioBici.getCodigo() + ", pin=" + pin);
        btnSolicitarDesbloqueo.setEnabled(false);
        btnSolicitarDesbloqueo.setText("Validando…");

        DesbloqueoRequest request = new DesbloqueoRequest(usuarioBici.getCodigo(), pin);
        ApiClient.getOrquestadorApi().solicitarDesbloqueo(request).enqueue(new Callback<DesbloqueoResponse>() {
            @Override
            public void onResponse(@NonNull Call<DesbloqueoResponse> call, @NonNull Response<DesbloqueoResponse> response) {
                btnSolicitarDesbloqueo.setEnabled(true);
                btnSolicitarDesbloqueo.setText(R.string.btn_solicitar_desbloqueo);

                if (response.isSuccessful() && response.body() != null) {
                    DesbloqueoResponse body = response.body();
                    Log.d(TAG, "Re-energizado OK, nuevo token=" + body.getIotAuthToken());
                    actualizarFirestoreYReiniciar(body);
                } else {
                    Snackbar.make(cardEstado, "Rechazado por el servidor", Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<DesbloqueoResponse> call, @NonNull Throwable t) {
                btnSolicitarDesbloqueo.setEnabled(true);
                btnSolicitarDesbloqueo.setText(R.string.btn_solicitar_desbloqueo);
                Log.e(TAG, "Fallo de red en re-energizar", t);
                Snackbar.make(cardEstado, "No se pudo conectar al servidor", Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void actualizarFirestoreYReiniciar(DesbloqueoResponse body) {
        usuarioBici.setIotAuthToken(body.getIotAuthToken());
        usuarioBici.setDesbloqueoExpiraEn(body.getDesbloqueoExpiraEn());
        usuarioBici.setTimestampAprobacion(body.getTimestampAprobacion());

        firestore.collection("users").document(usuarioBici.getUid().trim())
            .update(
                "iotAuthToken", body.getIotAuthToken(),
                "desbloqueoExpiraEn", body.getDesbloqueoExpiraEn(),
                "timestampAprobacion", body.getTimestampAprobacion()
            )
            .addOnSuccessListener(unused -> {
                Log.d(TAG, "Firestore actualizado, reiniciando contador a 120s");
                tvInfoUsuario.setText("Token IoT: " + usuarioBici.getIotAuthToken());
                mostrarEstadoActivo(SEGUNDOS_DE_VIDA);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error al actualizar Firestore", e);
                Snackbar.make(cardEstado, "Error al guardar en Firestore", Snackbar.LENGTH_LONG).show();
            });
    }

    private void irALogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
