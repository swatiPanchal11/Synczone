package com.example.synczone.common;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.synczone.R;
import com.example.synczone.adapters.DirectChatAdapter;
import com.example.synczone.common.ChatActivity;
import com.example.synczone.models.DirectChat;
import com.example.synczone.models.ModelChat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class DirectMessagesFragment extends Fragment {

    private RecyclerView recyclerDirectChats;
    private DirectChatAdapter directChatAdapter;
    private List<com.example.synczone.models.DirectChat> chatList;
    private DatabaseReference chatRef, usersRef;
    private String currentUserId;

    public DirectMessagesFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_direct_messages, container, false);

        recyclerDirectChats = view.findViewById(R.id.recyclerDirectChats);
        recyclerDirectChats.setLayoutManager(new LinearLayoutManager(getContext()));

        chatList = new ArrayList<>();
        directChatAdapter = new DirectChatAdapter(chatList, getContext(), userId -> {
            Intent intent = new Intent(getContext(), ChatActivity.class);
            intent.putExtra("receiverId", userId);
            startActivity(intent);
        });

        recyclerDirectChats.setAdapter(directChatAdapter);

        currentUserId = FirebaseAuth.getInstance().getUid();
        chatRef = FirebaseDatabase.getInstance().getReference("Chats");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        loadDirectChats();

        return view;
    }

    private void loadDirectChats() {
        chatRef.child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatList.clear();

                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String receiverId = chatSnapshot.getKey(); // Receiver's ID
                    ModelChat lastMessage = null;

                    for (DataSnapshot messageSnapshot : chatSnapshot.getChildren()) {
                        ModelChat message = messageSnapshot.getValue(ModelChat.class);
                        if (lastMessage == null || message.getTimestamp() > lastMessage.getTimestamp()) {
                            lastMessage = message; // Get the latest message
                        }
                    }

                    if (lastMessage != null) {
                        fetchUserDetails(receiverId, lastMessage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private void fetchUserDetails(String userId, ModelChat lastMessage) {
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String username = snapshot.child("username").getValue(String.class);
                    String profileImage = snapshot.child("profileImage").getValue(String.class);

                    DirectChat directChat = new DirectChat(
                            userId, username, profileImage, lastMessage.getMessage(), lastMessage.getTimestamp());
                    chatList.add(directChat);
                    directChatAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }
}
