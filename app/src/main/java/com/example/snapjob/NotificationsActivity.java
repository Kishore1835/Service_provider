package com.example.snapjob;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView recyclerNotifications;
    private ProgressBar progressBar;
    private LinearLayout layoutEmptyNotifications;
    private ImageView btnRefresh;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private List<NotificationModel> notificationList;
    private NotificationsAdapter adapter;

    private ProgressDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // 🔥 Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 🔗 Bind Views
        recyclerNotifications = findViewById(R.id.recyclerNotifications);
        progressBar = findViewById(R.id.notificationsProgressBar);
        layoutEmptyNotifications = findViewById(R.id.layoutEmptyNotifications);
        btnRefresh = findViewById(R.id.btnRefreshNotifications);

        recyclerNotifications.setLayoutManager(new LinearLayoutManager(this));
        notificationList = new ArrayList<>();
        adapter = new NotificationsAdapter(notificationList);
        recyclerNotifications.setAdapter(adapter);

        loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage("Loading notifications...");
        loadingDialog.setCancelable(false);

        // 🔄 Refresh Button
        btnRefresh.setOnClickListener(v -> fetchNotifications());

        // 🚀 Load notifications on start
        fetchNotifications();

        // ✅ Setup bottom menu
        setupBottomMenu();
    }

    // 🔥 Fetch notifications from Firestore
    private void fetchNotifications() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        layoutEmptyNotifications.setVisibility(View.GONE);
        recyclerNotifications.setVisibility(View.GONE);

        db.collection("notifications")
                .whereEqualTo("userEmail", user.getEmail())
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    notificationList.clear();

                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            NotificationModel model = doc.toObject(NotificationModel.class);
                            notificationList.add(model);
                        }
                        adapter.notifyDataSetChanged();
                        recyclerNotifications.setVisibility(View.VISIBLE);
                    } else {
                        layoutEmptyNotifications.setVisibility(View.VISIBLE);
                        insertDemoNotifications(user.getEmail()); // 👈 Insert demo if empty
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    layoutEmptyNotifications.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Error fetching data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // 🧩 Insert demo notifications for testing
    private void insertDemoNotifications(String email) {
        Map<String, Object> sample1 = new HashMap<>();
        sample1.put("title", "Booking Confirmed!");
        sample1.put("message", "Your booking for Electrician service has been confirmed successfully.");
        sample1.put("type", "booking");
        sample1.put("timestamp", new Date());
        sample1.put("userEmail", email);

        Map<String, Object> sample2 = new HashMap<>();
        sample2.put("title", "Payment Received");
        sample2.put("message", "You received ₹500 for your completed delivery task.");
        sample2.put("type", "payment");
        sample2.put("timestamp", new Date());
        sample2.put("userEmail", email);

        Map<String, Object> sample3 = new HashMap<>();
        sample3.put("title", "Task Update");
        sample3.put("message", "Your posted task 'Home Cleaning' has been accepted by a service provider.");
        sample3.put("type", "task");
        sample3.put("timestamp", new Date());
        sample3.put("userEmail", email);

        db.collection("notifications").add(sample1);
        db.collection("notifications").add(sample2);
        db.collection("notifications").add(sample3);

        Toast.makeText(this, "Sample notifications added!", Toast.LENGTH_SHORT).show();

        // Reload after demo insert
        fetchNotifications();
    }

    // ⚙️ Bottom Navigation setup
    private void setupBottomMenu() {
        findViewById(R.id.menu_profile).setOnClickListener(v ->
                navigateTo(ProfileActivity.class));

        findViewById(R.id.menu_requests).setOnClickListener(v ->
                navigateTo(MyRequestsActivity.class));

        findViewById(R.id.menu_dashboard).setOnClickListener(v ->
                navigateTo(HomeActivity.class));

        findViewById(R.id.menu_notifications).setOnClickListener(v ->
                Toast.makeText(this, "Already on Notifications", Toast.LENGTH_SHORT).show());

        findViewById(R.id.menu_chat).setOnClickListener(v ->
                navigateTo(ChatActivity.class));
    }

    private void navigateTo(Class<?> cls) {
        startActivity(new Intent(this, cls));
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }
}
