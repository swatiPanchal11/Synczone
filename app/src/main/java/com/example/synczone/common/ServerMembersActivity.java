package com.example.synczone.common;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.synczone.R;
import com.example.synczone.adapters.MembersAdapter;
import com.example.synczone.models.MemberModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ServerMembersActivity extends AppCompatActivity {
    private Button leaveServerBtn, deleteServerBtn, editServerBtn;
    private RecyclerView membersRecyclerView;
    private MembersAdapter membersAdapter;
    private List<MemberModel> membersList;

    private DatabaseReference serverRef, userRoleRef, databaseReference;
    private String serverId, currentUserId, currentUserRole;
    private static final int PICK_IMAGE_REQUEST = 1;
    private String base64Icon = null;
    CircleImageView serverIconImageView;
    TextView serverNameTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_members);

        serverIconImageView =findViewById(R.id.serverIcon);
        serverNameTextView = findViewById(R.id.serverName);
        initializeUI();
        validateServerId();
        setupRecyclerView();
        server();
        fetchCurrentUserRole();
        setupCopyInviteButton();
    }

    private void server() {
        serverRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String serverNameValue = snapshot.child("serverName").getValue(String.class);
                    String serverIconBase64 = snapshot.child("serverIcon").getValue(String.class);

                    if (serverNameValue != null) {
                        serverNameTextView.setText(serverNameValue); // serverNameTextView should be a TextView
                    }
                    if (serverIconBase64 != null) {
                        byte[] decodedString = Base64.decode(serverIconBase64, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        serverIconImageView.setImageBitmap(decodedByte); // serverIconImageView should be an ImageView
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ServerMembersActivity.this, "Failed to load server details!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupCopyInviteButton() {
        FloatingActionButton copyInviteFab = findViewById(R.id.copyInviteFab);
        copyInviteFab.setOnClickListener(v -> fetchAndShowInviteCode());
    }

    private void showInviteCodeDialog(String inviteCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_invite_code, null);
        builder.setView(dialogView);

        TextView inviteCodeText = dialogView.findViewById(R.id.inviteCodeText);
        Button copyButton = dialogView.findViewById(R.id.copyButton);

        inviteCodeText.setText(inviteCode);

        AlertDialog dialog = builder.create();
        dialog.show();

        copyButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Invite Code", inviteCode);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Invite Code Copied!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    private void fetchAndShowInviteCode() {
        serverRef.child("inviteCode").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String inviteCode = snapshot.getValue(String.class);
                    if (inviteCode != null) {
                        showInviteCodeDialog(inviteCode);
                    }
                } else {
                    Toast.makeText(ServerMembersActivity.this, "Invite code not found!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ServerMembersActivity.this, "Failed to fetch invite code.", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void initializeUI() {
        editServerBtn = findViewById(R.id.editServerBtn);
        leaveServerBtn = findViewById(R.id.leaveServerBtn);
        deleteServerBtn = findViewById(R.id.deleteServerBtn);

        serverId = getIntent().getStringExtra("serverId");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        serverRef = FirebaseDatabase.getInstance().getReference("servers").child(serverId);
        userRoleRef = serverRef.child("members").child(currentUserId).child("role");
    }

    private void validateServerId() {
        if (serverId == null || serverId.isEmpty()) {
            Toast.makeText(this, "Server ID is missing!", Toast.LENGTH_SHORT).show();
            finish();
        }
        databaseReference = serverRef.child("members");
    }

    private void setupRecyclerView() {
        membersRecyclerView = findViewById(R.id.membersRecyclerView);
        membersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        membersList = new ArrayList<>();
        membersAdapter = new MembersAdapter(this, membersList, serverId, currentUserId, currentUserRole);
        membersRecyclerView.setAdapter(membersAdapter);
    }

    private void fetchCurrentUserRole() {
        userRoleRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUserRole = snapshot.exists() ? snapshot.getValue(String.class) : "Member";
                setPermissions();
                fetchMembersList();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ServerMembersActivity.this, "Failed to fetch user role", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchMembersList() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                membersList.clear();
                for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                    String userId = memberSnapshot.getKey();
                    String role = memberSnapshot.child("role").getValue(String.class);

                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                            String username = userSnapshot.child("username").getValue(String.class);
                            membersList.add(new MemberModel(userId, username, role));
                            membersAdapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("ServerDebug", "Failed to fetch username for " + userId);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ServerMembersActivity.this, "Failed to load members!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setPermissions() {
        boolean isAdmin = "Admin".equals(currentUserRole);
        editServerBtn.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        leaveServerBtn.setVisibility(isAdmin ? View.GONE : View.VISIBLE);
        deleteServerBtn.setVisibility(isAdmin ? View.VISIBLE : View.GONE);

        editServerBtn.setOnClickListener(v -> openEditServerDialog());
        leaveServerBtn.setOnClickListener(v -> confirmLeaveServer());
        deleteServerBtn.setOnClickListener(v -> confirmDeleteServer());
    }

    private void openEditServerDialog() {
        userRoleRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!"Admin".equals(snapshot.getValue(String.class))) {
                    Toast.makeText(ServerMembersActivity.this, "Only Admins can edit the server!", Toast.LENGTH_SHORT).show();
                    return;
                }
                showEditDialog();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ServerMembersActivity.this, "Failed to load server data!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_server, null);
        builder.setView(view);

        EditText editServerName = view.findViewById(R.id.editServerName);
        EditText editServerDesc = view.findViewById(R.id.editServerDesc);
        ImageView serverImageDialog = view.findViewById(R.id.serverImage);
        Button saveButton = view.findViewById(R.id.btnSave);
        Button cancelButton = view.findViewById(R.id.btnCancel);

        loadServerDetails(editServerName, editServerDesc, serverImageDialog);

        serverImageDialog.setOnClickListener(v -> openImagePicker());
        AlertDialog dialog = builder.create();
        dialog.show();

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        saveButton.setOnClickListener(v -> {
            updateServerDetails(editServerName, editServerDesc);
            dialog.dismiss();
        });
    }

    private void loadServerDetails(EditText name, EditText desc, ImageView image) {
        serverRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                name.setText(snapshot.child("serverName").getValue(String.class));
                desc.setText(snapshot.child("serverDescription").getValue(String.class));
                String iconBase64 = snapshot.child("serverIcon").getValue(String.class);
                if (iconBase64 != null) {
                    byte[] decodedString = Base64.decode(iconBase64, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    image.setImageBitmap(bitmap);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ServerMembersActivity.this, "Failed to load server details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Server Icon"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                InputStream imageStream = getContentResolver().openInputStream(data.getData());
                Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
                base64Icon = encodeImageToBase64(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
    }

    private void updateServerDetails(EditText name, EditText desc) {
        String newName = name.getText().toString().trim();
        String newDesc = desc.getText().toString().trim();
        if (!newName.isEmpty() && !newDesc.isEmpty()) {
            serverRef.child("serverName").setValue(newName);
            serverRef.child("serverDescription").setValue(newDesc);
            if (base64Icon != null) serverRef.child("serverIcon").setValue(base64Icon);
            Toast.makeText(this, "Server updated successfully!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Fields cannot be empty!", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmLeaveServer() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Server")
                .setMessage("Are you sure you want to leave this server?")
                .setPositiveButton("Yes", (dialog, which) -> leaveServer())
                .setNegativeButton("No", null)
                .show();
    }

    private void leaveServer() {
        serverRef.child("members").child(currentUserId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "You left the server", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void confirmDeleteServer() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Server")
                .setMessage("Are you sure you want to delete this server? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteServer())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteServer() {
        serverRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Server deleted", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
