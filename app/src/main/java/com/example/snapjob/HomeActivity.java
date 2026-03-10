package com.example.snapjob;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private TextView tvAppName, tvUserEmail, tvVerified, tvTime, tvDate;
    private ImageView imgProfile;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Handler handler = new Handler();
    private FloatingActionButton fabAddTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // 🔹 Firebase initialization
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 🔹 Bind UI components
        tvAppName = findViewById(R.id.appTitle);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvVerified = findViewById(R.id.tvVerifiedStatus);
        tvTime = findViewById(R.id.tvTime);
        tvDate = findViewById(R.id.tvDate);
        imgProfile = findViewById(R.id.profileImage);
        fabAddTask = findViewById(R.id.btnAddTask);

        // 🔹 Animate App Title
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.bounce);
        tvAppName.startAnimation(fadeIn);

        // 🔹 Load user details
        loadUserData();

        // 🔹 Update live time and date
        updateTimeAndDate();

        // 🔹 Bottom navigation setup
        setupBottomMenu();

        // 🔹 Floating button → open AssignTaskActivity
        fabAddTask.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AssignTaskActivity.class);
            startActivity(intent);
        });
    }

    /**
     * 🔹 Load and listen for user verification data in real-time
     */
    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        tvUserEmail.setText(currentUser.getEmail());

        // ✅ Real-time listener for verification status
        db.collection("verifications")
                .whereEqualTo("userId", currentUser.getUid())
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error fetching data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        tvVerified.setText("Not Verified");
                        tvVerified.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        return;
                    }

                    // Extract first matching document
                    DocumentSnapshot snapshot = querySnapshot.getDocuments().get(0);
                    String status = snapshot.getString("status");

                    if ("Approved".equalsIgnoreCase(status) || "Verified".equalsIgnoreCase(status)) {
                        tvVerified.setText("✔ Verified User");
                        tvVerified.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    } else if ("Rejected".equalsIgnoreCase(status)) {
                        tvVerified.setText("❌ Access Terminated");
                        tvVerified.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    } else {
                        tvVerified.setText("Pending Verification");
                        tvVerified.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                    }
                });
    }

    /**
     * ⏰ Continuously update time and date every second
     */
    private void updateTimeAndDate() {
        handler.postDelayed(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                String currentDate = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(new Date());

                tvTime.setText(currentTime);
                tvDate.setText(currentDate);

                handler.postDelayed(this, 1000);
            }
        }, 0);
    }

    /**
     * ⚙️ Set up bottom navigation buttons
     */
    private void setupBottomMenu() {
        LinearLayout menuProfile = findViewById(R.id.menu_profile);
        LinearLayout menuRequests = findViewById(R.id.menu_requests);
        LinearLayout menuDashboard = findViewById(R.id.menu_dashboard);
        LinearLayout menuNotifications = findViewById(R.id.menu_notifications);
        LinearLayout menuChat = findViewById(R.id.menu_chat);

        if (menuProfile != null)
            menuProfile.setOnClickListener(v -> safeNavigate(ProfileActivity.class));
        if (menuRequests != null)
            menuRequests.setOnClickListener(v -> safeNavigate(MyRequestsActivity.class));
        if (menuDashboard != null)
            menuDashboard.setOnClickListener(v -> safeNavigate(HomeActivity.class));
        if (menuNotifications != null)
            menuNotifications.setOnClickListener(v -> safeNavigate(NotificationsActivity.class));
        if (menuChat != null)
            menuChat.setOnClickListener(v -> safeNavigate(ChatActivity.class));
    }

    /**
     * 🎬 Safe navigation handler
     */
    private void safeNavigate(Class<?> targetActivity) {
        try {
            Intent intent = new Intent(HomeActivity.this, targetActivity);
            startActivity(intent);
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        } catch (Exception e) {
            Toast.makeText(this, "Page not yet available!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 🔒 Logout
     */
    public void logout(android.view.View view) {
        mAuth.signOut();
        startActivity(new Intent(HomeActivity.this, LoginActivity.class));
        finish();
    }
}
