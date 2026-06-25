package com.example.bicipucp.carne;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.bicipucp.R;
import com.example.bicipucp.model.UsuarioBici;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class CarneActivity extends AppCompatActivity {

    private static final String TAG = "BiciPUCP";
    private static final String STORAGE_FOLDER = "credenciales_bicipucp";

    private ImageView ivAvatar;
    private TextView tvNombre, tvCodigo, tvUrl;
    private MaterialButton btnSubirFoto;
    private ProgressBar pbSubiendo;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private FirebaseUser usuarioActual;
    private UsuarioBici usuarioBici;

    private ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carne);

        getSupportActionBar().setTitle(R.string.carne_titulo);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        usuarioActual = firebaseAuth.getCurrentUser();

        if (usuarioActual == null) {
            finish();
            return;
        }

        ivAvatar = findViewById(R.id.ivAvatar);
        tvNombre = findViewById(R.id.tvNombre);
        tvCodigo = findViewById(R.id.tvCodigo);
        tvUrl = findViewById(R.id.tvUrl);
        btnSubirFoto = findViewById(R.id.btnSubirFoto);
        pbSubiendo = findViewById(R.id.pbSubiendo);

        // Lanzador moderno del Photo Picker (no requiere permisos en API 33+)
        pickMediaLauncher = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(),
            uri -> {
                if (uri != null) {
                    Log.d(TAG, "Imagen seleccionada: " + uri);
                    subirImagenAFirebaseStorage(uri);
                } else {
                    Log.d(TAG, "Selección cancelada por el usuario");
                }
            }
        );

        btnSubirFoto.setOnClickListener(v -> abrirGaleria());

        cargarPerfil();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void cargarPerfil() {
        String uid = usuarioActual.getUid().trim();
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener(snapshot -> {
                usuarioBici = snapshot.toObject(UsuarioBici.class);
                if (usuarioBici == null) return;

                tvNombre.setText(usuarioBici.getNombre());
                tvCodigo.setText(usuarioBici.getCodigo());

                if (usuarioBici.getFotoUrl() != null && !usuarioBici.getFotoUrl().isEmpty()) {
                    tvUrl.setText(usuarioBici.getFotoUrl());
                    Glide.with(this)
                        .load(usuarioBici.getFotoUrl())
                        .placeholder(R.drawable.ic_persona_placeholder)
                        .circleCrop()
                        .into(ivAvatar);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error al cargar perfil", e);
                Snackbar.make(btnSubirFoto, "Error: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
            });
    }

    private void abrirGaleria() {
        pickMediaLauncher.launch(new PickVisualMediaRequest.Builder()
            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
            .build());
    }

    private void subirImagenAFirebaseStorage(Uri uri) {
        if (usuarioActual == null) return;
        String uid = usuarioActual.getUid().trim();

        pbSubiendo.setVisibility(View.VISIBLE);
        btnSubirFoto.setEnabled(false);
        Toast.makeText(this, R.string.carne_subiendo, Toast.LENGTH_SHORT).show();

        // Ruta: /credenciales_bicipucp/<UID>.jpg
        StorageReference ref = storage.getReference()
            .child(STORAGE_FOLDER)
            .child(uid + ".jpg");

        ref.putFile(uri)
            .addOnSuccessListener(taskSnapshot -> {
                Log.d(TAG, "Upload OK, obteniendo downloadUrl…");
                ref.getDownloadUrl()
                    .addOnSuccessListener(downloadUri -> {
                        String url = downloadUri.toString();
                        Log.d(TAG, "downloadUrl=" + url);
                        Toast.makeText(this, "URL: " + url, Toast.LENGTH_LONG).show();
                        guardarUrlEnFirestore(uid, url, uri);
                    })
                    .addOnFailureListener(e -> {
                        pbSubiendo.setVisibility(View.GONE);
                        btnSubirFoto.setEnabled(true);
                        Log.e(TAG, "No se pudo obtener downloadUrl", e);
                        Snackbar.make(btnSubirFoto, "Error obteniendo URL: " + e.getMessage(),
                            Snackbar.LENGTH_LONG).show();
                    });
            })
            .addOnFailureListener(e -> {
                pbSubiendo.setVisibility(View.GONE);
                btnSubirFoto.setEnabled(true);
                Log.e(TAG, "Error al subir imagen", e);
                Snackbar.make(btnSubirFoto, getString(R.string.carne_subida_error) + ": " + e.getMessage(),
                    Snackbar.LENGTH_LONG).show();
            });
    }

    private void guardarUrlEnFirestore(String uid, String url, Uri uriLocal) {
        firestore.collection("users").document(uid)
            .update("fotoUrl", url)
            .addOnSuccessListener(unused -> {
                pbSubiendo.setVisibility(View.GONE);
                btnSubirFoto.setEnabled(true);
                tvUrl.setText(url);
                Snackbar.make(btnSubirFoto, R.string.carne_subida_ok, Snackbar.LENGTH_SHORT).show();

                Glide.with(this)
                    .load(uriLocal)
                    .circleCrop()
                    .into(ivAvatar);
            })
            .addOnFailureListener(e -> {
                pbSubiendo.setVisibility(View.GONE);
                btnSubirFoto.setEnabled(true);
                Log.e(TAG, "Error al actualizar fotoUrl en Firestore", e);
                Snackbar.make(btnSubirFoto, "Subió foto pero falló al guardar URL: " + e.getMessage(),
                    Snackbar.LENGTH_LONG).show();
            });
    }
}
