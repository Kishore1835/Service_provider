package com.example.snapjob;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerChat;
    private EditText etMessage;
    private ImageView btnSend, btnMic;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private List<ChatMessage> chatList;
    private ChatAdapter chatAdapter;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // ✅ Firebase setup
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userEmail = currentUser.getEmail();

        // ✅ UI bindings
        recyclerChat = findViewById(R.id.recyclerChat);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnMic = findViewById(R.id.btnMic);

        // ✅ Setup RecyclerView
        chatList = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatList, userEmail);
        recyclerChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerChat.setAdapter(chatAdapter);

        // ✅ Fetch chat messages
        loadChatMessages();

        // ✅ Send message
        btnSend.setOnClickListener(v -> sendMessage());

        // (Optional) 🎤 Mic click placeholder
        btnMic.setOnClickListener(v -> Toast.makeText(this, "Voice input coming soon!", Toast.LENGTH_SHORT).show());

        // ✅ Setup bottom navigation
        setupBottomMenu();
    }

    // 🔥 Real-time chat listener
    private void loadChatMessages() {
        db.collection("chat_messages")
                .whereEqualTo("userEmail", userEmail)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading messages.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (value != null) {
                        chatList.clear();
                        for (DocumentChange doc : value.getDocumentChanges()) {
                            ChatMessage message = doc.getDocument().toObject(ChatMessage.class);
                            chatList.add(message);
                        }
                        chatAdapter.notifyDataSetChanged();
                        recyclerChat.scrollToPosition(chatList.size() - 1);
                    }
                });
    }

    // 📨 Send message to Firebase
    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        Map<String, Object> message = new HashMap<>();
        message.put("sender", userEmail);
        message.put("message", text);
        message.put("timestamp", new Date());
        message.put("userEmail", userEmail);
        message.put("fromSupport", false);

        db.collection("chat_messages").add(message)
                .addOnSuccessListener(documentReference -> {
                    etMessage.setText("");
                    recyclerChat.scrollToPosition(chatList.size() - 1);
                    simulateSupportReply(text);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // 💬 Simulated Support Reply (demo)
    private void simulateSupportReply(String userText) {
        String reply;
        if (userText.toLowerCase().contains("payment")) {
            reply = "Our team has received your payment query. We’ll verify it soon!";
        } else if (userText.toLowerCase().contains("booking")) {
            reply = "Booking confirmed. Thank you for choosing SnapJob!";
        } else {
            reply = "Support: Thank you for your message. We’ll get back to you shortly.";
        }

        Map<String, Object> supportMsg = new HashMap<>();
        supportMsg.put("sender", "Support Team");
        supportMsg.put("message", reply);
        supportMsg.put("timestamp", new Date());
        supportMsg.put("userEmail", userEmail);
        supportMsg.put("fromSupport", true);

        db.collection("chat_messages").add(supportMsg);
    }

    // ⚙️ Bottom Menu Navigation
    private void setupBottomMenu() {
        findViewById(R.id.menu_profile).setOnClickListener(v ->
                navigateTo(ProfileActivity.class));

        findViewById(R.id.menu_requests).setOnClickListener(v ->
                navigateTo(MyRequestsActivity.class));

        findViewById(R.id.menu_dashboard).setOnClickListener(v ->
                navigateTo(HomeActivity.class));

        findViewById(R.id.menu_notifications).setOnClickListener(v ->
                navigateTo(NotificationsActivity.class));

        findViewById(R.id.menu_chat).setOnClickListener(v ->
                Toast.makeText(this, "Already on Support Chat", Toast.LENGTH_SHORT).show());
    }

    private void navigateTo(Class<?> cls) {
        startActivity(new android.content.Intent(this, cls));
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }
}
