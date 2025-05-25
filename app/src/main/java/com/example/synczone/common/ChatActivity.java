package com.example.synczone.common;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.synczone.R;
import com.example.synczone.adapters.Chat;
import com.example.synczone.models.ModelChat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private static final int IMAGE_PICK_CODE = 1000;

    private RecyclerView recyclerChat;
    private EditText editMessage;
    private Button buttonSend;
    private ImageButton buttonSendImage;
    private CircleImageView userProfileImage;
    private TextView userName;

    private String senderId, receiverId;
    private DatabaseReference chatRef;
    private List<ModelChat> chatList;
    private Chat chatAdapter;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        userProfileImage = findViewById(R.id.userProfileImage);
        userName = findViewById(R.id.userName);
        recyclerChat = findViewById(R.id.recyclerChat);
        editMessage = findViewById(R.id.editMessage);
        buttonSend = findViewById(R.id.buttonSend);
        buttonSendImage = findViewById(R.id.buttonSendImage); // Add this button in XML

        senderId = FirebaseAuth.getInstance().getUid();
        receiverId = getIntent().getStringExtra("receiverId");

        if (receiverId == null) {
            Toast.makeText(this, "Error: No user selected!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        chatRef = FirebaseDatabase.getInstance().getReference("Chats");
        chatList = new ArrayList<>();
        chatAdapter = new Chat(chatList, senderId);

        recyclerChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerChat.setAdapter(chatAdapter);

        loadReceiverDetails();
        loadMessages();

        buttonSend.setOnClickListener(v -> sendMessage());
        buttonSendImage.setOnClickListener(v -> pickImageFromGallery());
    }

    private void loadReceiverDetails() {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(receiverId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("username").getValue(String.class);
                    String profileImage = snapshot.child("profileImage").getValue(String.class);
                    userName.setText(name);

                    if (profileImage != null && !profileImage.isEmpty()) {
                        Bitmap decodedImage = decodeBase64Image(profileImage);
                        userProfileImage.setImageBitmap(decodedImage);
                    } else {
                        userProfileImage.setImageResource(R.drawable.default_profile);
                    }
                }
            }

            private Bitmap decodeBase64Image(String base64Str) {
                try {
                    byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
                    return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Failed to load user details!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMessages() {
        chatRef.child(senderId).child(receiverId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    ModelChat message = data.getValue(ModelChat.class);
                    chatList.add(message);
                }
                chatAdapter.notifyDataSetChanged();
                recyclerChat.scrollToPosition(chatList.size() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Failed to load messages!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String text = editMessage.getText().toString().trim();
        if (!text.isEmpty()) {
            String messageId = chatRef.push().getKey();
            ModelChat message = new ModelChat(senderId, receiverId, text, null, System.currentTimeMillis());
            saveMessageToDatabase(messageId, message);
            editMessage.setText("");
        }
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, IMAGE_PICK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_PICK_CODE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                sendImageMessage(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendImageMessage(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        String encodedImage = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

        String messageId = chatRef.push().getKey();
        ModelChat imageMessage = new ModelChat(senderId, receiverId, null, encodedImage, System.currentTimeMillis());
        saveMessageToDatabase(messageId, imageMessage);
    }

    private void saveMessageToDatabase(String messageId, ModelChat message) {
        chatRef.child(senderId).child(receiverId).child(messageId).setValue(message);
        chatRef.child(receiverId).child(senderId).child(messageId).setValue(message);
    }
}
