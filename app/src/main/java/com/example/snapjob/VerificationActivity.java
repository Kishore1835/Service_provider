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
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class VerificationActivity extends AppCompatActivity {

    private EditText etIdNumber;
    private RadioGroup idTypeGroup;
    private RadioButton rbAadhaar, rbPAN;
    private ImageView imgPreview;
    private Button btnUploadImage, btnSubmit;

    private Uri imageUri = null;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageReference;
    private ProgressDialog progressDialog;

    private static final int PICK_IMAGE_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference("verifications");

        // Link XML
        etIdNumber = findViewById(R.id.etIdNumber);
        idTypeGroup = findViewById(R.id.idTypeGroup);
        rbAadhaar = findViewById(R.id.rbAadhaar);
        rbPAN = findViewById(R.id.rbPAN);
        imgPreview = findViewById(R.id.imgPreview);
        btnUploadImage = findViewById(R.id.btnUploadImage);
        btnSubmit = findViewById(R.id.btnSubmit);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        btnUploadImage.setOnClickListener(v -> openImagePicker());
        btnSubmit.setOnClickListener(v -> validateAndSubmit());
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Document Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                imgPreview.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void validateAndSubmit() {
        String idNumber = etIdNumber.getText().toString().trim();
        int selectedId = idTypeGroup.getCheckedRadioButtonId();

        if (selectedId == -1) {
            Toast.makeText(this, "Please select ID type", Toast.LENGTH_SHORT).show();
            return;
        }

        if (idNumber.isEmpty()) {
            etIdNumber.setError("Please enter your ID number");
            etIdNumber.requestFocus();
            return;
        }

        String idType = (selectedId == R.id.rbAadhaar) ? "Aadhaar" : "PAN";

        // If image not uploaded, skip upload (temporary fix)
        if (imageUri == null) {
            saveVerificationData(idType, idNumber, null);
        } else {
            uploadImageAndSave(idType, idNumber);
        }
    }

    private void uploadImageAndSave(String idType, String idNumber) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        progressDialog.setMessage("Uploading verification details...");
        progressDialog.show();

        StorageReference fileRef = storageReference.child(currentUser.getUid() + "/" + idType + "_document.jpg");
        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    saveVerificationData(idType, idNumber, uri.toString());
                }))
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveVerificationData(String idType, String idNumber, @Nullable String imageUrl) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("userId", currentUser.getUid());
        data.put("email", currentUser.getEmail());
        data.put("idType", idType);
        data.put("idNumber", idNumber);
        if (imageUrl != null) data.put("documentUrl", imageUrl);
        data.put("status", "Pending");
        data.put("timestamp", System.currentTimeMillis());

        db.collection("verifications").document(currentUser.getUid())
                .set(data)
                .addOnSuccessListener(unused -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Verification submitted successfully!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error saving data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
