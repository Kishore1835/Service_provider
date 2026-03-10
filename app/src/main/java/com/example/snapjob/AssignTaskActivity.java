package com.example.snapjob;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AssignTaskActivity extends AppCompatActivity {

    private EditText etTaskName, etDescription, etAmount;
    private TextView tvFromLocation, tvToLocation;
    private Button btnPickFrom, btnPickTo, btnUploadPhoto, btnSubmitTask;
    private ImageView imgPreview;

    private Uri imageUri;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private ProgressDialog progressDialog;

    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int PICK_FROM_LOCATION = 200;
    private static final int PICK_TO_LOCATION = 201;

    private String fromLocation = "";
    private String toLocation = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assign_task);

        // ✅ Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("task_photos");
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        // ✅ Session check
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(AssignTaskActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // ✅ Bind UI elements
        etTaskName = findViewById(R.id.etTaskName);
        etDescription = findViewById(R.id.etDescription);
        etAmount = findViewById(R.id.etAmount);
        tvFromLocation = findViewById(R.id.layoutFromLocation);
        tvToLocation = findViewById(R.id.layoutToLocation);
        btnPickFrom = findViewById(R.id.btnChooseFrom);
        //btnPickTo = findViewById(R.id.btnPickTo);
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto);
        btnSubmitTask = findViewById(R.id.btnSubmitTask);
        imgPreview = findViewById(R.id.imgPreview);

        // --- Pick From Location ---
        btnPickFrom.setOnClickListener(v -> openMap(PICK_FROM_LOCATION));

        // --- Pick To Location ---
        btnPickTo.setOnClickListener(v -> openMap(PICK_TO_LOCATION));

        // --- Upload Photo ---
        btnUploadPhoto.setOnClickListener(v -> openImagePicker());

        // --- Submit Task ---
        btnSubmitTask.setOnClickListener(v -> submitTask());

        // ✅ Bottom menu setup
        setupBottomMenu();
    }

    // 🔹 Open Google Maps (mock for now)
    private void openMap(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="));
        startActivityForResult(intent, requestCode);
    }

    // 🔹 Choose Image
    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Task Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
                imageUri = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    imgPreview.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (requestCode == PICK_FROM_LOCATION) {
                fromLocation = "User selected start location";
                tvFromLocation.setText(fromLocation);
            } else if (requestCode == PICK_TO_LOCATION) {
                toLocation = "User selected destination";
                tvToLocation.setText(toLocation);
            }
        }
    }

    // 🔹 Validate and upload task
    private void submitTask() {
        String taskName = etTaskName.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        String amount = etAmount.getText().toString().trim();

        if (taskName.isEmpty() || desc.isEmpty() || amount.isEmpty()) {
            Toast.makeText(this, "All fields are required except photo", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Posting task...");
        progressDialog.show();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri != null) {
            StorageReference fileRef = storageRef.child(System.currentTimeMillis() + "_task.jpg");
            fileRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> saveTaskData(taskName, desc, amount, uri.toString(), user)))
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            saveTaskData(taskName, desc, amount, null, user);
        }
    }

    // 🔹 Save to Firestore
    private void saveTaskData(String taskName, String desc, String amount, String imageUrl, FirebaseUser user) {
        Map<String, Object> task = new HashMap<>();
        task.put("taskName", taskName);
        task.put("description", desc);
        task.put("amount", amount);
        task.put("fromLocation", fromLocation);
        task.put("toLocation", toLocation);
        task.put("imageUrl", imageUrl != null ? imageUrl : "");
        task.put("status", "Open");
        task.put("postedBy", user.getEmail());
        task.put("timestamp", System.currentTimeMillis());

        db.collection("tasks").add(task)
                .addOnSuccessListener(documentReference -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Task posted successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ✅ Bottom Menu Handler (same as HomeActivity)
    private void setupBottomMenu() {
        try {
            LinearLayout menuProfile = findViewById(R.id.menu_profile);
            LinearLayout menuRequests = findViewById(R.id.menu_requests);
            LinearLayout menuDashboard = findViewById(R.id.menu_dashboard);
            LinearLayout menuNotifications = findViewById(R.id.menu_notifications);
            LinearLayout menuChat = findViewById(R.id.menu_chat);

            // Profile
            menuProfile.setOnClickListener(v -> navigateTo(ProfileActivity.class));

            // My Requests
            menuRequests.setOnClickListener(v -> navigateTo(MyRequestsActivity.class));

            // Dashboard
            menuDashboard.setOnClickListener(v -> navigateTo(HomeActivity.class));

            // Notifications
            menuNotifications.setOnClickListener(v -> navigateTo(NotificationsActivity.class));

            // Chat
            menuChat.setOnClickListener(v -> navigateTo(ChatActivity.class));

        } catch (Exception e) {
            Toast.makeText(this, "Navigation setup failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 🎬 Reusable navigation with animation
    private void navigateTo(Class<?> targetActivity) {
        Intent intent = new Intent(AssignTaskActivity.this, targetActivity);
        startActivity(intent);
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }
}
