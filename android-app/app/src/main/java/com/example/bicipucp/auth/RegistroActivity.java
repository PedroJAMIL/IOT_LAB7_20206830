package com.example.bicipucp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bicipucp.R;
import com.example.bicipucp.main.MainActivity;
import com.example.bicipucp.model.UsuarioBici;
import com.example.bicipucp.service.AuthService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

public class RegistroActivity extends AppCompatActivity {

    private TextInputEditText etNombre, etCorreo, etContrasena, etCodigo, etPin;
    private MaterialButton btnRegistrar;
    private LinearLayout llValidando;
    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        authService = new AuthService();

        etNombre = findViewById(R.id.etNombre);
        etCorreo = findViewById(R.id.etCorreo);
        etContrasena = findViewById(R.id.etContrasena);
        etCodigo = findViewById(R.id.etCodigo);
        etPin = findViewById(R.id.etPin);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        llValidando = findViewById(R.id.llValidando);

        findViewById(R.id.tvBarra).setOnClickListener(v -> finish());

        btnRegistrar.setOnClickListener(v -> intentarRegistro());
    }

    private void intentarRegistro() {
        String nombre = textOf(etNombre);
        String correo = textOf(etCorreo);
        String contrasena = textOf(etContrasena);
        String codigo = textOf(etCodigo);
        String pin = textOf(etPin);

        if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(correo) ||
            TextUtils.isEmpty(contrasena) || TextUtils.isEmpty(codigo) || TextUtils.isEmpty(pin)) {
            Snackbar.make(btnRegistrar, R.string.error_campos_vacios, Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (codigo.length() != 8) {
            Snackbar.make(btnRegistrar, R.string.error_codigo_formato, Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (pin.length() != 4) {
            Snackbar.make(btnRegistrar, R.string.error_pin_formato, Snackbar.LENGTH_SHORT).show();
            return;
        }

        cargando(true);

        authService.registrar(nombre, correo, contrasena, codigo, pin,
            new AuthService.RegistroCallback() {
                @Override
                public void onExito(UsuarioBici usuario) {
                    cargando(false);
                    Snackbar.make(btnRegistrar, "Registro exitoso. Bienvenido " + usuario.getNombre(),
                        Snackbar.LENGTH_SHORT).show();

                    btnRegistrar.postDelayed(() -> {
                        Intent intent = new Intent(RegistroActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }, 800);
                }

                @Override
                public void onErrorBackend(String mensaje) {
                    cargando(false);
                    Snackbar.make(btnRegistrar, mensaje, Snackbar.LENGTH_LONG).show();
                }

                @Override
                public void onErrorFirebase(String mensaje) {
                    cargando(false);
                    Snackbar.make(btnRegistrar, mensaje, Snackbar.LENGTH_LONG).show();
                }
            }
        );
    }

    private void cargando(boolean estado) {
        btnRegistrar.setEnabled(!estado);
        llValidando.setVisibility(estado ? View.VISIBLE : View.GONE);
    }

    private String textOf(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }
}
