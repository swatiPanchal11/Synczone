package com.example.synczone.common;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.synczone.R;
import com.example.synczone.adapters.ServerAdapter;
import com.example.synczone.dialogs.CreateServerDialog;
import com.example.synczone.models.ServerModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServersFragment extends Fragment {

    private RecyclerView serversRecyclerView;
    private FloatingActionButton fab;
    private List<ServerModel> serverList;
    private ServerAdapter serverAdapter;
    private DatabaseReference serversRef;
    private FirebaseUser currentUser;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_servers, container, false);

        serversRecyclerView = view.findViewById(R.id.serversRecyclerView);
        fab = view.findViewById(R.id.fabAddServer);
        progressBar = view.findViewById(R.id.progressBarLoading);

        serversRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        serverList = new ArrayList<>();
        serverAdapter = new ServerAdapter(getContext(), serverList, server -> {
            if (server != null) {
                Log.d("ServersFragment", "Server clicked: " + server.getServerName() + ", ID: " + server.getServerId());
                if (server.getServerId() != null) {
                    Intent intent = new Intent(getContext(), ServerActivity.class);
                    intent.putExtra("serverId", server.getServerId());
                    startActivity(intent);
                } else {
                    Log.e("ServersFragment", "Server ID is null for server: " + server.getServerName());
                }
            } else {
                Log.e("ServersFragment", "Server object is null!");
            }
        });

        serversRecyclerView.setAdapter(serverAdapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        serversRef = FirebaseDatabase.getInstance().getReference("servers");

        if (currentUser != null) {
            loadUserServers();
        } else {
            Log.e("ServersFragment", "User not authenticated!");
            Toast.makeText(getContext(), "User authentication failed!", Toast.LENGTH_SHORT).show();
        }

        fab.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Choose an Option")
                    .setItems(new CharSequence[]{"Create Server", "Join Server"}, (dialog, which) -> {
                        if (which == 0) {
                            CreateServerDialog createServerDialog = new CreateServerDialog(requireContext());
                            createServerDialog.show(getParentFragmentManager(), "CreateServerDialog");
                        } else {
                            showJoinServerDialog();
                        }
                    })
                    .show();
        });

        loadUserServers();
        return view;
    }

    private void showJoinServerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Join Server");

        final EditText input = new EditText(requireContext());
        input.setHint("Enter Invite Code");
        builder.setView(input);

        builder.setPositiveButton("Join", (dialog, which) -> {
            String inviteCode = input.getText().toString().trim();
            if (!inviteCode.isEmpty()) {
                joinServer(inviteCode);
            } else {
                Toast.makeText(requireContext(), "Please enter a valid code!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void joinServer(String inviteCode) {
        DatabaseReference serversRef = FirebaseDatabase.getInstance().getReference().child("servers");

        serversRef.orderByChild("inviteCode").equalTo(inviteCode).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot serverSnapshot : snapshot.getChildren()) {
                        String serverId = serverSnapshot.getKey();
                        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        DatabaseReference membersRef = serversRef.child(serverId).child("members").child(userId);

                        Map<String, Object> memberData = new HashMap<>();
                        memberData.put("role", "Member");

                        membersRef.setValue(memberData).addOnSuccessListener(aVoid -> {
                            Toast.makeText(requireContext(), "Joined server successfully!", Toast.LENGTH_SHORT).show();
                            loadUserServers();
                        }).addOnFailureListener(e -> {
                            Log.e("Firebase", "Error joining server: " + e.getMessage());
                            Toast.makeText(requireContext(), "Failed to join server", Toast.LENGTH_SHORT).show();
                        });

                        return;
                    }
                } else {
                    Toast.makeText(requireContext(), "Invalid invite code!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Database error!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserServers() {
        progressBar.setVisibility(View.VISIBLE);

        serversRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    serverList.clear();
                    String userId = currentUser.getUid();

                    for (DataSnapshot serverSnapshot : snapshot.getChildren()) {
                        ServerModel server = serverSnapshot.getValue(ServerModel.class);

                        if (server != null && server.getMembers() != null && server.getMembers().containsKey(userId)) {
                            String base64Image = serverSnapshot.child("serverImage").getValue(String.class);
                            if (base64Image != null && !base64Image.isEmpty()) {
                                try {
                                    byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                                    Bitmap decodedImage = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                    server.setDecodedImage(decodedImage);
                                } catch (Exception e) {
                                    Log.e("ServersFragment", "Image decoding failed: " + e.getMessage());
                                }
                            }
                            serverList.add(server);
                        }
                    }

                    serverAdapter.notifyDataSetChanged();

                    if (serverList.isEmpty()) {
                        Toast.makeText(getContext(), "No servers available", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d("ServersFragment", "No servers found in database!");
                }

                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ServersFragment", "Database error: " + error.getMessage());
                progressBar.setVisibility(View.GONE);
            }
        });
    }
}
