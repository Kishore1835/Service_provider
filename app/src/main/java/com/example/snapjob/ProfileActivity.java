package com.example.snapjob;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.*;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ImageView imgProfile, btnEditPhoto;
    private EditText etName, etEmail, etVerificationType, etAadhaar;
    private Button btnSave;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageReference;

    private Uri imageUri = null;
    private ProgressDialog progressDialog;

    private static final int PICK_IMAGE_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // ✅ Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference("user_profiles");

        // ✅ Session check
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // ✅ Bind UI
        imgProfile = findViewById(R.id.imgProfile);
        btnEditPhoto = findViewById(R.id.btnEditPhoto);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etVerificationType = findViewById(R.id.etVerificationType);
        etAadhaar = findViewById(R.id.etAadhaar);
        btnSave = findViewById(R.id.btnSave);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        // Load user data
        loadUserProfile();

        // Select new photo
        btnEditPhoto.setOnClickListener(v -> openImagePicker());

        // Save profile updates
        btnSave.setOnClickListener(v -> saveProfile());

        // ✅ Setup Bottom Navigation
        setupBottomMenu();
    }

    // 🔹 Pick image from gallery
    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Photo"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                imgProfile.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 🔹 Load data from Firebase
    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        etEmail.setText(currentUser.getEmail());

        // Load user basic info
        DocumentReference userRef = db.collection("users").document(currentUser.getUid());
        userRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                if (snapshot.contains("name"))
                    etName.setText(snapshot.getString("name"));
                if (snapshot.contains("profileImage")) {
                    String photoUrl = snapshot.getString("profileImage");
                    // TODO: Use Glide/Picasso for loading photo
                    // Glide.with(this).load(photoUrl).into(imgProfile);
                }
            }
        });

        // Load verification info
        DocumentReference verifyRef = db.collection("verifications").document(currentUser.getUid());
        verifyRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                String idType = snapshot.getString("idType");
                String idNumber = snapshot.getString("idNumber");

                if (idType != null)
                    etVerificationType.setText(idType);

                if (idNumber != null && idNumber.length() > 2) {
                    String masked = idNumber.charAt(0) + "*******" + idNumber.charAt(idNumber.length() - 1);
                    etAadhaar.setText(masked);
                    etAadhaar.setEnabled(false);
                }
            }
        });
    }

    // 🔹 Save updates
    private void saveProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
            return;
        }

        String name = etName.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Name required");
            etName.requestFocus();
            return;
        }

        progressDialog.setMessage("Saving changes...");
        progressDialog.show();

        if (imageUri != null) {
            // Upload image to Firebase Storage
            StorageReference fileRef = storageReference.child(currentUser.getUid() + "_profile.jpg");
            fileRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        saveToFirestore(name, uri.toString());
                    }))
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } else {
            saveToFirestore(name, null);
        }
    }

    // 🔹 Save info in Firestore
    private void saveToFirestore(String name, String imageUrl) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        if (imageUrl != null) updates.put("profileImage", imageUrl);

        db.collection("users").document(currentUser.getUid())
                .set(updates)
                .addOnSuccessListener(unused -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // ✅ Bottom Menu Handler
    private void setupBottomMenu() {
        LinearLayout menuProfile = findViewById(R.id.menu_profile);
        LinearLayout menuRequests = findViewById(R.id.menu_requests);
        LinearLayout menuDashboard = findViewById(R.id.menu_dashboard);
        LinearLayout menuNotifications = findViewById(R.id.menu_notifications);
        LinearLayout menuChat = findViewById(R.id.menu_chat);

        // Profile (current activity) — no navigation
        menuProfile.setOnClickListener(v ->
                Toast.makeText(ProfileActivity.this, "Already on Profile", Toast.LENGTH_SHORT).show()
        );

        // My Requests
        menuRequests.setOnClickListener(v -> navigateTo(MyRequestsActivity.class));

        // Dashboard
        menuDashboard.setOnClickListener(v -> navigateTo(HomeActivity.class));

        // Notifications
        menuNotifications.setOnClickListener(v -> navigateTo(NotificationsActivity.class));

        // Chat
        menuChat.setOnClickListener(v -> navigateTo(ChatActivity.class));
    }

    // 🎬 Reusable navigation with animation
    private void navigateTo(Class<?> targetActivity) {
        Intent intent = new Intent(ProfileActivity.this, targetActivity);
        startActivity(intent);
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }
}
