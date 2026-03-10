package com.example.snapjob;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private com.google.android.material.textfield.TextInputEditText etName, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private ImageView btnGoogleSignUp;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog;

    // Google Sign-in
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Views
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        btnGoogleSignUp = findViewById(R.id.btnGoogleSignUp);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Progress
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        // Google Sign-In setup
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                // this id comes from Firebase console -> Project settings -> Web client (OAuth 2.0)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Clicks
        btnRegister.setOnClickListener(v -> registerUserWithEmail());
        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
        btnGoogleSignUp.setOnClickListener(v -> startGoogleSignIn());
    }

    /**
     * Email/Password registration
     * Note: We DO NOT store password in Firestore. Firebase Auth handles password securely.
     */
    private void registerUserWithEmail() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirm = etConfirmPassword.getText().toString();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email)
                || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirm)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Registering...");
        progressDialog.show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Create user doc in Firestore under document ID = email
                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("uid", firebaseUser.getUid());
                            userMap.put("name", name);
                            userMap.put("email", email);
                            userMap.put("authProvider", "password");
                            userMap.put("isVerified", false);
                            userMap.put("createdAt", System.currentTimeMillis());

                            DocumentReference ref = db.collection("users").document(email);
                            ref.set(userMap)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(RegisterActivity.this, "Registration successful. Please login.", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(RegisterActivity.this, "Firestore error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Start Google Sign-In flow for new user registration (or login if account exists)
     */
    private void startGoogleSignIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    /**
     * Handle Google Sign-In result and authenticate with Firebase
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount acct = task.getResult(ApiException.class);
                if (acct != null) {
                    String idToken = acct.getIdToken();
                    firebaseAuthWithGoogleToken(idToken, acct);
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign-in failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Use the Google ID token to authenticate with Firebase
     */
    private void firebaseAuthWithGoogleToken(String idToken, GoogleSignInAccount acct) {
        progressDialog.setMessage("Signing in with Google...");
        progressDialog.show();

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    progressDialog.dismiss();
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        // If user doc not exists, create it (use email as doc id)
                        String email = user.getEmail();
                        if (email == null) email = "unknown_" + user.getUid();
                        final String finalEmail = email;
                        DocumentReference ref = db.collection("users").document(finalEmail);
                        ref.get().addOnSuccessListener(snapshot -> {
                            if (!snapshot.exists()) {
                                Map<String, Object> userMap = new HashMap<>();
                                userMap.put("uid", user.getUid());
                                userMap.put("name", acct.getDisplayName() != null ? acct.getDisplayName() : "");
                                userMap.put("email", finalEmail);
                                userMap.put("authProvider", "google");
                                userMap.put("isVerified", false);
                                userMap.put("createdAt", System.currentTimeMillis());

                                ref.set(userMap)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(RegisterActivity.this, "Account created with Google", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(RegisterActivity.this, "Firestore error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        });
                            } else {
                                // Already exists, just proceed to home
                                startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
                                finish();
                            }
                        }).addOnFailureListener(e -> {
                            Toast.makeText(RegisterActivity.this, "Error checking user: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(RegisterActivity.this, "Firebase auth failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Optionally: Sign-out Google client when activity destroyed to keep clean state
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // mGoogleSignInClient.signOut(); // uncomment if needed
    }
}
