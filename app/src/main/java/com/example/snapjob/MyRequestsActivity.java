package com.example.snapjob;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MyRequestsActivity extends AppCompatActivity {

    private RecyclerView recyclerBookings;
    private ProgressBar progressBar;
    private TextView tvSelectedDate, tvTotalTasks, tvCompletedTasks, tvPendingTasks, tvTotalEarnings;
    private ImageView btnCalendar, btnRefresh;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private List<TaskModel> taskList;
    private TaskAdapter adapter;

    private String selectedDate = "";
    private ProgressDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_requests);

        // 🔥 Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 🔗 UI Components
        recyclerBookings = findViewById(R.id.recyclerBookings);
        progressBar = findViewById(R.id.progressBar);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        btnCalendar = findViewById(R.id.btnCalendar);
        btnRefresh = findViewById(R.id.btnRefresh);
        tvTotalTasks = findViewById(R.id.tvTotalTasks);
        tvCompletedTasks = findViewById(R.id.tvCompletedTasks);
        tvPendingTasks = findViewById(R.id.tvPendingTasks);
        tvTotalEarnings = findViewById(R.id.tvTotalEarnings);

        recyclerBookings.setLayoutManager(new LinearLayoutManager(this));
        taskList = new ArrayList<>();
        adapter = new TaskAdapter(taskList);
        recyclerBookings.setAdapter(adapter);

        loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage("Loading your requests...");
        loadingDialog.setCancelable(false);

        // 📅 Calendar Picker
        btnCalendar.setOnClickListener(v -> showDatePicker());

        // 🔁 Refresh Button
        btnRefresh.setOnClickListener(v -> {
            selectedDate = "";
            tvSelectedDate.setText("All Dates");
            fetchBookingsFromFirebase();
        });

        // 🚀 Load Bookings initially
        fetchBookingsFromFirebase();

        // ✅ Setup Bottom Navigation
        setupBottomMenu();
    }

    // 📅 Show Date Picker
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    selectedDate = sdf.format(calendar.getTime());
                    tvSelectedDate.setText(selectedDate);
                    fetchBookingsFromFirebase();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    // 🔥 Fetch user bookings from Firebase
    private void fetchBookingsFromFirebase() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadingDialog.show();
        progressBar.setVisibility(ProgressBar.VISIBLE);

        db.collection("tasks")
                .whereEqualTo("postedBy", user.getEmail())
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    loadingDialog.dismiss();
                    if (task.isSuccessful()) {
                        taskList.clear();

                        int total = 0, completed = 0, pending = 0;
                        double totalEarnings = 0;

                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            TaskModel model = doc.toObject(TaskModel.class);
                            model.setId(doc.getId());

                            // Filter by selected date if applied
                            if (!selectedDate.isEmpty()) {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                String docDate = sdf.format(doc.getDate("timestamp"));
                                if (!selectedDate.equals(docDate)) continue;
                            }

                            taskList.add(model);
                            total++;

                            if ("Completed".equalsIgnoreCase(model.getStatus())) {
                                completed++;
                                try {
                                    totalEarnings += Double.parseDouble(model.getAmount());
                                } catch (Exception ignored) {}
                            } else {
                                pending++;
                            }
                        }

                        adapter.notifyDataSetChanged();

                        // Update summary
                        tvTotalTasks.setText("Total Tasks: " + total);
                        tvCompletedTasks.setText("Completed: " + completed);
                        tvPendingTasks.setText("Pending: " + pending);
                        tvTotalEarnings.setText("Total Earnings: ₹" + totalEarnings);

                        if (taskList.isEmpty()) {
                            Toast.makeText(this, "No requests found for this date.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Error loading data.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ⚙️ Bottom Navigation
    private void setupBottomMenu() {
        findViewById(R.id.menu_profile).setOnClickListener(v ->
                navigateTo(ProfileActivity.class));

        findViewById(R.id.menu_requests).setOnClickListener(v ->
                Toast.makeText(this, "Already on My Requests", Toast.LENGTH_SHORT).show());

        findViewById(R.id.menu_dashboard).setOnClickListener(v ->
                navigateTo(HomeActivity.class));

        findViewById(R.id.menu_notifications).setOnClickListener(v ->
               navigateTo(NotificationsActivity.class));

        findViewById(R.id.menu_chat).setOnClickListener(v ->
               navigateTo(ChatActivity.class));
    }

    private void navigateTo(Class<?> cls) {
        startActivity(new android.content.Intent(this, cls));
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }
}
