package com.example.synczone.common;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.synczone.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ProfileFragment extends Fragment {

    private static final int IMAGE_PICK_CODE = 100;
    private ImageView profileImage;
    private TextView usernameText, emailText, textFollowers, textFollowing;
    private Button editProfileButton, logoutButton;
    private DatabaseReference userRef;
    private FirebaseAuth auth;
    private String userId;
    private Bitmap selectedBitmap;
    private ValueEventListener profileListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        profileImage = view.findViewById(R.id.profileImage);
        usernameText = view.findViewById(R.id.etUsername);
        emailText = view.findViewById(R.id.tvEmail);
        textFollowers = view.findViewById(R.id.textFollowersCount);
        textFollowing = view.findViewById(R.id.textFollowingCount);
        editProfileButton = view.findViewById(R.id.btnChangeImage);
        logoutButton = view.findViewById(R.id.btnLogout);

        auth = FirebaseAuth.getInstance();
        userId = auth.getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);

        attachProfileListener();
        editProfileButton.setOnClickListener(v -> openEditProfileDialog());
        logoutButton.setOnClickListener(v -> logoutUser());

        // Open Followers List
        textFollowers.setOnClickListener(v -> openFollowersList());
        textFollowing.setOnClickListener(v -> openFollowingList());

        return view;
    }

    private void attachProfileListener() {
        profileListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String username = snapshot.child("username").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String profileBase64 = snapshot.child("profileImage").getValue(String.class);

                    long followersCount = snapshot.child("followers").getChildrenCount();
                    long followingCount = snapshot.child("following").getChildrenCount();

                    usernameText.setText(username);
                    emailText.setText(email);
                    textFollowers.setText(followersCount + " Followers");
                    textFollowing.setText(followingCount + " Following");

                    loadProfileImage(profileBase64, profileImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load profile!", Toast.LENGTH_SHORT).show();
            }
        };
        userRef.addValueEventListener(profileListener);
    }

    private void openFollowersList() {
        Intent intent = new Intent(getActivity(), FollowersListActivity.class);
        intent.putExtra("userId", userId);
        startActivity(intent);
    }

    private void openFollowingList() {
        Intent intent = new Intent(getActivity(), FollowingListActivity.class);
        intent.putExtra("userId", userId);
        startActivity(intent);
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();

        // Clear cached user data
        SharedPreferences.Editor editor = getActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void openEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);
        builder.setView(view);

        EditText editName = view.findViewById(R.id.editName);
        ImageView profileImgDialog = view.findViewById(R.id.profileImage);
        Button saveButton = view.findViewById(R.id.btnSave);
        Button cancelButton = view.findViewById(R.id.btnCancel);

        userRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                String currentName = snapshot.child("username").getValue(String.class);
                String profileBase64 = snapshot.child("profileImage").getValue(String.class);
                editName.setText(currentName);
                loadProfileImage(profileBase64, profileImgDialog);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        saveButton.setOnClickListener(v -> {
            String newName = editName.getText().toString().trim();
            if (!newName.isEmpty()) {
                userRef.child("username").setValue(newName)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Update failed!", Toast.LENGTH_SHORT).show());
            }
        });

        profileImgDialog.setOnClickListener(v -> selectImageFromGallery());
    }

    private void loadProfileImage(String base64Image, ImageView imageView) {
        if (base64Image != null && !base64Image.isEmpty()) {
            byte[] imageBytes = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            imageView.setImageBitmap(bitmap);
        }
    }

    private void selectImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, IMAGE_PICK_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                selectedBitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), imageUri);
                saveProfileImageToFirebase(selectedBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveProfileImageToFirebase(Bitmap bitmap) {
        if (bitmap != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            String encodedImage = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

            userRef.child("profileImage").setValue(encodedImage)
                    .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Profile image updated!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update image!", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (profileListener != null) {
            userRef.removeEventListener(profileListener);
        }
    }
}
