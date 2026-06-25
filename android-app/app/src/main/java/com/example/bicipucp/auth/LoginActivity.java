package com.example.bicipucp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bicipucp.R;
import com.example.bicipucp.main.MainActivity;
import com.example.bicipucp.service.AuthService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etCorreo, etContrasena;
    private MaterialButton btnIngresar;
    private ProgressBar pbLogin;
    private TextView tvIrRegistro;
    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authService = new AuthService();

        if (authService.getUsuarioActual() != null) {
            irAMain();
            return;
        }

        etCorreo = findViewById(R.id.etCorreo);
        etContrasena = findViewById(R.id.etContrasena);
        btnIngresar = findViewById(R.id.btnIngresar);
        pbLogin = findViewById(R.id.pbLogin);
        tvIrRegistro = findViewById(R.id.tvIrRegistro);

        btnIngresar.setOnClickListener(v -> intentarLogin());

        tvIrRegistro.setOnClickListener(v ->
            startActivity(new Intent(this, RegistroActivity.class))
        );
    }

    private void intentarLogin() {
        String correo = textOf(etCorreo);
        String contrasena = textOf(etContrasena);

        if (TextUtils.isEmpty(correo) || TextUtils.isEmpty(contrasena)) {
            Snackbar.make(btnIngresar, R.string.error_campos_vacios, Snackbar.LENGTH_SHORT).show();
            return;
        }

        cargando(true);

        authService.login(correo, contrasena, new AuthService.LoginCallback() {
            @Override
            public void onExito(com.google.firebase.auth.FirebaseUser user) {
                cargando(false);
                irAMain();
            }

            @Override
            public void onError(String mensaje) {
                cargando(false);
                Snackbar.make(btnIngresar, mensaje, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void cargando(boolean estado) {
        btnIngresar.setEnabled(!estado);
        pbLogin.setVisibility(estado ? View.VISIBLE : View.GONE);
    }

    private void irAMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String textOf(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }
}
