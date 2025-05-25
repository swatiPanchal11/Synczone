package com.example.synczone.common;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.synczone.R;
import com.example.synczone.adapters.ChannelAdapter;
import com.example.synczone.models.Channel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ServerActivity extends AppCompatActivity {

    private TextView tvServerName;
    private RecyclerView rvTextChannels, rvVoiceChannels;
    private ImageButton btnAddChannel;
    private String serverId;
    private String currentUserId;
    private String currentUserRole; // "Admin", "Mod", or "Member"

    private DatabaseReference serverRef;
    private DatabaseReference channelsRef;

    private List<Channel> textChannelList = new ArrayList<>();
    private List<Channel> voiceChannelList = new ArrayList<>();
    private ChannelAdapter textChannelAdapter, voiceChannelAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        tvServerName = findViewById(R.id.tvServerName);
        rvTextChannels = findViewById(R.id.rvTextChannels);
        rvVoiceChannels = findViewById(R.id.rvVoiceChannels);
        btnAddChannel = findViewById(R.id.btnAddChannel);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        serverId = getIntent().getStringExtra("serverId");
        if (serverId == null) {
            Toast.makeText(this, "Server not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        serverRef = FirebaseDatabase.getInstance().getReference("servers").child(serverId);
        channelsRef = serverRef.child("channels");

        // Fetch and display server name.
        serverRef.child("serverName").addListenerForSingleValueEvent(new ValueEventListener(){
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String serverName = snapshot.getValue(String.class);
                    tvServerName.setText(serverName);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        // Get the current user's role.
        serverRef.child("members").child(currentUserId).child("role")
                .addListenerForSingleValueEvent(new ValueEventListener(){
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()){
                            currentUserRole = snapshot.getValue(String.class);
                            if ("Admin".equals(currentUserRole) || "Mod".equals(currentUserRole)) {
                                btnAddChannel.setVisibility(ImageButton.VISIBLE);
                            } else {
                                btnAddChannel.setVisibility(ImageButton.GONE);
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });

        // Setup RecyclerViews.
        rvTextChannels.setLayoutManager(new LinearLayoutManager(this));
        rvVoiceChannels.setLayoutManager(new LinearLayoutManager(this));
        textChannelAdapter = new ChannelAdapter(textChannelList, channel -> {
            // Launch your existing ServerChatActivity when a text channel is tapped.
            Intent intent = new Intent(ServerActivity.this, ServerChatActivity.class);
            intent.putExtra("serverId", serverId);
            intent.putExtra("channelName", channel.getName());
            intent.putExtra("currentUserRole", currentUserRole);
            startActivity(intent);
        });
        rvTextChannels.setAdapter(textChannelAdapter);

        voiceChannelAdapter = new ChannelAdapter(voiceChannelList, channel -> {
            // Placeholder: when a voice channel is tapped.
            Toast.makeText(ServerActivity.this, "Joining voice channel: " + channel.getName(), Toast.LENGTH_SHORT).show();
            // Later, integrate your voice channel functionality.
        });
        rvVoiceChannels.setAdapter(voiceChannelAdapter);

        // Fetch channels from Firebase.
        channelsRef.addValueEventListener(new ValueEventListener(){
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                textChannelList.clear();
                voiceChannelList.clear();
                for(DataSnapshot ds : snapshot.getChildren()){
                    Channel channel = ds.getValue(Channel.class);
                    if(channel != null){
                        if("text".equalsIgnoreCase(channel.getType())){
                            textChannelList.add(channel);
                        } else if("voice".equalsIgnoreCase(channel.getType())){
                            voiceChannelList.add(channel);
                        }
                    }
                }
                // If no channels, create default ones.
                if(textChannelList.isEmpty()){
                    Channel general = new Channel("general", "text");
                    textChannelList.add(general);
                    channelsRef.child("general").setValue(general);
                }
                if(voiceChannelList.isEmpty()){
                    Channel voiceDefault = new Channel("Voice", "voice");
                    voiceChannelList.add(voiceDefault);
                    channelsRef.child("voice").setValue(voiceDefault);
                }
                textChannelAdapter.notifyDataSetChanged();
                voiceChannelAdapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        // Add channel button click (for Admin/Mod).
        btnAddChannel.setOnClickListener(v -> {
            showAddChannelDialog();
        });
    }

    private void showAddChannelDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Channel");
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_channel, null);
        final EditText etChannelName = view.findViewById(R.id.etChannelName);
        final Spinner spChannelType = view.findViewById(R.id.spChannelType);

        // Set spinner adapter using a string-array resource (define this in res/values/strings.xml)
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.channel_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spChannelType.setAdapter(adapter);

        builder.setView(view);
        builder.setPositiveButton("Create", (dialog, which) -> {
            String channelName = etChannelName.getText().toString().trim();
            // Now spChannelType.getSelectedItem() will not be null
            String channelType = spChannelType.getSelectedItem().toString(); // "Text" or "Voice"
            if (!channelName.isEmpty()) {
                createChannel(channelName, channelType);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
    private void createChannel(String channelName, String channelType) {
        HashMap<String, Object> channelData = new HashMap<>();
        channelData.put("name", channelName);
        channelData.put("type", channelType.toLowerCase());
        // Save channel data under "servers/{serverId}/channels/{channelName}"
        channelsRef.child(channelName).setValue(channelData)
                .addOnSuccessListener(aVoid -> {
                    // Explicitly initialize an empty "messages" node for this channel.
                    channelsRef.child(channelName).child("messages").setValue(new HashMap<String, Object>())
                            .addOnCompleteListener(task -> {
                                Toast.makeText(ServerActivity.this, "Channel created", Toast.LENGTH_SHORT).show();
                                // Launch ServerChatActivity using the newly created channel name.
                                Intent intent = new Intent(ServerActivity.this, ServerChatActivity.class);
                                intent.putExtra("serverId", serverId);
                                intent.putExtra("channelName", channelName);  // make sure this is not "general"
                                intent.putExtra("currentUserRole", currentUserRole);
                                startActivity(intent);
                            });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(ServerActivity.this, "Failed to create channel", Toast.LENGTH_SHORT).show());
    }

}
