package com.example.synczone.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.synczone.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Random;

public class CreateServerDialog extends DialogFragment {

    private EditText serverNameInput, serverDescriptionInput;
    private ProgressBar progressBarCreate;
    private ImageView serverProfileImage;
    private String encodedImage = null;
    private static final int IMAGE_PICK_REQUEST = 100;
    private DatabaseReference serversRef;
    private FirebaseAuth auth;
    private Context context;

    public CreateServerDialog(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_create_server, null);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        serversRef = FirebaseDatabase.getInstance().getReference("servers");

        // Bind Views
        serverNameInput = view.findViewById(R.id.etServerName);
        serverDescriptionInput = view.findViewById(R.id.etServerDescription);
        progressBarCreate = view.findViewById(R.id.progressBarCreate);
        serverProfileImage = view.findViewById(R.id.serverProfileImage);

        // Image Selection
        serverProfileImage.setOnClickListener(v -> openGallery());

        builder.setView(view)
                .setTitle("Create Server")
                .setPositiveButton("Create", null)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        return builder.create();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_PICK_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                serverProfileImage.setImageBitmap(bitmap);
                encodedImage = encodeImage(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String encodeImage(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
        byte[] imageBytes = outputStream.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> createServer(dialog));
        }
    }

    private void createServer(AlertDialog dialog) {
        String serverName = serverNameInput.getText().toString().trim();
        String serverDescription = serverDescriptionInput.getText().toString().trim();

        if (serverName.isEmpty()) {
            Toast.makeText(context, "Please enter a server name", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBarCreate.setVisibility(View.VISIBLE);

        // Set default image if none selected
        if (encodedImage == null) {
            Bitmap defaultBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_server);
            encodedImage = encodeImage(defaultBitmap);
        }

        // Generate Unique Server ID & Invite Code
        String serverId = serversRef.push().getKey();
        String inviteCode = generateInviteCode();
        String adminId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (serverId == null || adminId == null) {
            Toast.makeText(context, "Error generating server ID or user authentication!", Toast.LENGTH_SHORT).show();
            progressBarCreate.setVisibility(View.GONE);
            return;
        }

        // Store server data in HashMap
        HashMap<String, Object> serverData = new HashMap<>();
        serverData.put("serverId", serverId);
        serverData.put("serverName", serverName);
        serverData.put("serverDescription", serverDescription);
        serverData.put("serverIcon", encodedImage);
        serverData.put("inviteCode", inviteCode);
        serverData.put("createdBy", adminId);

        // Firebase Database References
        DatabaseReference serverRef = serversRef.child(serverId);
        DatabaseReference membersRef = serverRef.child("members").child(adminId);

        // Create server in Firebase
        serverRef.setValue(serverData).addOnSuccessListener(aVoid -> {
            membersRef.child("role").setValue("Admin").addOnSuccessListener(roleAdded -> {
                Toast.makeText(context, "Server Created Successfully!", Toast.LENGTH_SHORT).show();
                progressBarCreate.setVisibility(View.GONE);
                dialog.dismiss();
            }).addOnFailureListener(e -> {
                Toast.makeText(context, "Failed to assign admin role: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                progressBarCreate.setVisibility(View.GONE);
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(context, "Failed to create server: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            progressBarCreate.setVisibility(View.GONE);
        });
    }

    private String generateInviteCode() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder inviteCode = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            inviteCode.append(characters.charAt(random.nextInt(characters.length())));
        }
        return inviteCode.toString();
    }
}