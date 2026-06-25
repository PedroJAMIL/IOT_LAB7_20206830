package com.example.bicipucp.service;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.bicipucp.model.UsuarioBici;
import com.example.bicipucp.network.ApiClient;
import com.example.bicipucp.network.DesbloqueoRequest;
import com.example.bicipucp.network.DesbloqueoResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthService {

    private static final String TAG = "BiciPUCP";

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;

    public interface RegistroCallback {
        void onExito(UsuarioBici usuario);
        void onErrorBackend(String mensaje);
        void onErrorFirebase(String mensaje);
    }

    public interface LoginCallback {
        void onExito(FirebaseUser user);
        void onError(String mensaje);
    }

    public AuthService() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Flujo de registro completo:
     * 1) POST al orquestador con codigo + pin.
     * 2) Si 200 OK -> Firebase Auth createUserWithEmailAndPassword.
     * 3) Guardar perfil en Firestore (collection "users", doc = UID).
     */
    public void registrar(String nombre, String correo, String contrasena,
                          String codigo, String pin, RegistroCallback callback) {

        Log.d(TAG, "registrar() -> POST a orquestador con codigo=" + codigo);

        DesbloqueoRequest request = new DesbloqueoRequest(codigo, pin);
        Call<DesbloqueoResponse> call = ApiClient.getOrquestadorApi().solicitarDesbloqueo(request);

        call.enqueue(new Callback<DesbloqueoResponse>() {
            @Override
            public void onResponse(@NonNull Call<DesbloqueoResponse> call,
                                   @NonNull Response<DesbloqueoResponse> response) {

                if (response.isSuccessful() && response.body() != null) {
                    DesbloqueoResponse body = response.body();
                    Log.d(TAG, "Orquestador OK: token=" + body.getIotAuthToken());
                    crearUsuarioFirebase(nombre, correo, contrasena, codigo, body, callback);
                } else {
                    String mensajeError = extraerMensajeError(response);
                    Log.e(TAG, "Orquestador rechazo: " + mensajeError);
                    callback.onErrorBackend(mensajeError);
                }
            }

            @Override
            public void onFailure(@NonNull Call<DesbloqueoResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Fallo de red contra orquestador", t);
                callback.onErrorBackend("No se pudo conectar al servidor. Verifique que el orquestador esté corriendo.");
            }
        });
    }

    private void crearUsuarioFirebase(String nombre, String correo, String contrasena, String codigo,
                                       DesbloqueoResponse backendData, RegistroCallback callback) {

        firebaseAuth.createUserWithEmailAndPassword(correo, contrasena)
            .addOnSuccessListener(authResult -> {
                FirebaseUser user = authResult.getUser();
                if (user == null) {
                    callback.onErrorFirebase("Firebase devolvio usuario nulo");
                    return;
                }

                String uid = user.getUid().trim();
                UsuarioBici usuario = new UsuarioBici(
                    uid,
                    nombre,
                    correo,
                    codigo,
                    backendData.getIotAuthToken(),
                    backendData.getDesbloqueoExpiraEn(),
                    backendData.getTimestampAprobacion()
                );

                firestore.collection("users").document(uid).set(usuario)
                    .addOnSuccessListener(unused -> {
                        Log.d(TAG, "Perfil guardado en Firestore -> users/" + uid);
                        callback.onExito(usuario);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error al guardar en Firestore", e);
                        callback.onErrorFirebase("No se pudo guardar el perfil: " + e.getMessage());
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error al crear usuario en Firebase Auth", e);
                callback.onErrorFirebase("Firebase Auth: " + e.getMessage());
            });
    }

    /**
     * Login simple con Firebase Auth, sin pasar por el orquestador.
     * El re-bloqueo / re-validacion se hace despues desde MainActivity.
     */
    public void login(String correo, String contrasena, LoginCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(correo, contrasena)
            .addOnSuccessListener(authResult -> {
                FirebaseUser user = authResult.getUser();
                if (user != null) {
                    Log.d(TAG, "Login OK -> uid=" + user.getUid());
                    callback.onExito(user);
                } else {
                    callback.onError("Usuario nulo despues del login");
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Login fallido", e);
                callback.onError("Credenciales invalidas: " + e.getMessage());
            });
    }

    private String extraerMensajeError(Response<DesbloqueoResponse> response) {
        try {
            if (response.errorBody() != null) {
                String json = response.errorBody().string();
                // Parseo manual mínimo para no requerir DTO extra
                int idx = json.indexOf("\"mensaje\"");
                if (idx >= 0) {
                    int start = json.indexOf("\"", idx + 9) + 1;
                    int end = json.indexOf("\"", start);
                    if (start > 0 && end > start) {
                        return json.substring(start, end);
                    }
                }
                return json;
            }
        } catch (IOException e) {
            Log.e(TAG, "No se pudo leer errorBody", e);
        }
        return "Error desconocido del servidor (HTTP " + response.code() + ")";
    }

    public FirebaseUser getUsuarioActual() {
        return firebaseAuth.getCurrentUser();
    }

    public void cerrarSesion() {
        firebaseAuth.signOut();
    }
}
