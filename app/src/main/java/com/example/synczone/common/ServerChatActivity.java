package com.example.synczone.common;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.synczone.R;
import com.example.synczone.adapters.ChatAdapter;
import com.example.synczone.models.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ServerChatActivity extends AppCompatActivity {
    private CircleImageView serverImage;
    private TextView serverNameTextView;
    private String serverId;
    private EditText messageInput;
    private ImageButton sendButton, attachImageBtn;
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;
    private DatabaseReference chatRef;
    private static final int PICK_IMAGE_REQUEST = 1;
    private MediaRecorder mediaRecorder;
    private String audioFilePath;
    private boolean isRecording = false;
    private ImageButton recordButton; // Add a button for recording
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_chat);

        // UI Initialization
        serverImage = findViewById(R.id.serverImage);
        serverNameTextView = findViewById(R.id.serverName);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendMessageBtn);
        attachImageBtn = findViewById(R.id.attachImageBtn);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);

        recordButton = findViewById(R.id.recordButton);

        recordButton.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            } else {
                checkAudioPermission();
            }
        });

        // Get Server ID
        serverId = getIntent().getStringExtra("serverId");
        if (serverId == null || serverId.isEmpty()) {
            Toast.makeText(this, "Error: Server ID not found!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize Firebase Reference
        chatRef = FirebaseDatabase.getInstance().getReference("Chats").child(serverId);

        // Fetch Server Name
        fetchServerDetail();

        // RecyclerView Setup
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, messageList,"Admin");
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // Load Messages
        loadMessages();

        // Send Message Button
        sendButton.setOnClickListener(v -> {
            String messageText = messageInput.getText().toString().trim();
            if (!messageText.isEmpty()) {
                sendMessage(messageText);
                messageInput.setText("");
            }
        });

        // Attach Image Button
        attachImageBtn.setOnClickListener(v -> openGallery());

        // Open Server Members List
        serverNameTextView.setOnClickListener(v -> {
            Intent intent = new Intent(ServerChatActivity.this, ServerMembersActivity.class);
            intent.putExtra("serverId", serverId);
            startActivity(intent);
        });
    }

    // Start Recording Audio
    private void checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            startRecording();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                Toast.makeText(this, "Permission Denied! Cannot record audio.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startRecording() {
        audioFilePath = getExternalCacheDir().getAbsolutePath() + "/voice_message.3gp";
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFile(audioFilePath);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            recordButton.setImageResource(R.drawable.ic_stop);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;
        isRecording = false;
        recordButton.setImageResource(R.drawable.ic_mic);
        encodeAudioToBase64();
    }

    private void encodeAudioToBase64() {
        try {
            FileInputStream fileInputStream = new FileInputStream(audioFilePath);
            byte[] audioBytes = new byte[(int) new File(audioFilePath).length()];
            fileInputStream.read(audioBytes);
            fileInputStream.close();

            String encodedAudio = Base64.encodeToString(audioBytes, Base64.DEFAULT);
            sendVoiceMessage(encodedAudio);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Send Voice Message to Firebase
    private void sendVoiceMessage(String voiceBase64) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);

        userRef.child("username").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                String senderName = task.getResult().getValue(String.class);
                if (senderName == null || senderName.isEmpty()) senderName = "Unknown User";

                long timestamp = System.currentTimeMillis();
                ChatMessage chatMessage = new ChatMessage(userId, senderName, timestamp, voiceBase64, true);

                DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("Chats").child(serverId);
                chatRef.push().setValue(chatMessage).addOnSuccessListener(aVoid -> {
                    Toast.makeText(ServerChatActivity.this, "Voice Message Sent", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e -> {
                    Toast.makeText(ServerChatActivity.this, "Failed to send voice message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

        // Open Gallery to Select Image
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    encodeImageToBase64(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Convert Bitmap to Base64 String
    private void encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        String encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT);

        sendMessageWithImage(encodedImage);
    }

    private void sendMessageWithImage(String imageBase64) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);

        userRef.child("username").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                String senderName = task.getResult().getValue(String.class);
                if (senderName == null || senderName.isEmpty()) senderName = "Unknown User";

                long timestamp = System.currentTimeMillis();
                ChatMessage chatMessage = new ChatMessage(userId, senderName, "", timestamp, imageBase64, true);

                DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("Chats").child(serverId);
                chatRef.push().setValue(chatMessage).addOnSuccessListener(aVoid -> {
                    Toast.makeText(ServerChatActivity.this, "Image Sent", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e -> {
                    Toast.makeText(ServerChatActivity.this, "Failed to send image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void fetchServerDetail() {
        DatabaseReference serverRef = FirebaseDatabase.getInstance().getReference("servers").child(serverId);

        serverRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Fetch and set server name
                    String serverName = snapshot.child("serverName").getValue(String.class);
                    if (serverName != null && !serverName.isEmpty()) {
                        serverNameTextView.setText(serverName);
                    }

                    // Fetch and decode server icon (Base64)
                    String serverIconBase64 = snapshot.child("serverIcon").getValue(String.class);
                    if (serverIconBase64 != null && !serverIconBase64.isEmpty()) {
                        try {
                            byte[] decodedBytes = Base64.decode(serverIconBase64, Base64.DEFAULT);
                            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                            serverImage.setImageBitmap(decodedBitmap);
                        } catch (IllegalArgumentException e) {
                            Toast.makeText(ServerChatActivity.this, "Failed to decode server image.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ServerChatActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void sendMessage(String messageText) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);

        userRef.child("username").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                String senderName = task.getResult().getValue(String.class);
                if (senderName == null || senderName.isEmpty()) senderName = "Unknown User";

                long timestamp = System.currentTimeMillis();
                ChatMessage chatMessage = new ChatMessage(userId, senderName, messageText, timestamp);

                // 🔥 Server ID ke andar Message Save Karo
                DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("Chats").child(serverId);
                chatRef.push().setValue(chatMessage).addOnSuccessListener(aVoid -> {
                    Log.d("ServerChatActivity", "Message Sent Successfully");
                }).addOnFailureListener(e -> {
                    Log.e("ServerChatActivity", "Message Sending Failed: ", e);
                });
            }
        });
    }

    private void loadMessages() {
        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("Chats").child(serverId);

        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    ChatMessage chatMessage = dataSnapshot.getValue(ChatMessage.class);
                    if (chatMessage != null) {
                        messageList.add(chatMessage);
                    }
                }
                chatAdapter.notifyDataSetChanged();
                if (!messageList.isEmpty()) {
                    chatRecyclerView.smoothScrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ServerChatActivity", "Error loading messages: " + error.getMessage());
            }
        });
    }
}
