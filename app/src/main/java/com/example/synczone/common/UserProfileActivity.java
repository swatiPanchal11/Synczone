package com.example.synczone.common;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.synczone.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserProfileActivity extends AppCompatActivity {

    private CircleImageView profileImage;
    private TextView textUsername, textFollowersCount, textFollowingCount;
    private Button buttonFollow,buttonMessage;
    private DatabaseReference userRef, followRef;
    private String userId, currentUserId;
    private boolean isFollowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        profileImage = findViewById(R.id.imageProfile);
        textUsername = findViewById(R.id.textUsername);
        textFollowersCount = findViewById(R.id.textFollowersCount);
        textFollowingCount = findViewById(R.id.textFollowingCount);
        buttonFollow = findViewById(R.id.buttonFollow);
        buttonMessage = findViewById(R.id.buttonMessage);

        userId = getIntent().getStringExtra("USER_ID");
        currentUserId = FirebaseAuth.getInstance().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        followRef = FirebaseDatabase.getInstance().getReference("Follow");

        loadUserProfileFromIntent();  // Intent se image aur username set karega
        loadUserProfileFromFirebase();  // Firebase se update karega
        loadFollowCounts();
        checkFollowingStatus();

        buttonFollow.setOnClickListener(v -> {
            if (isFollowing) {
                unfollowUser();
            } else {
                followUser();
            }
        });
    }

    // **Intent se profile image aur username load karna**
    private void loadUserProfileFromIntent() {
        String username = getIntent().getStringExtra("USERNAME");
        String profileImageBase64 = getIntent().getStringExtra("PROFILE_IMAGE");

        textUsername.setText(username);

        if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
            try {
                byte[] imageBytes = Base64.decode(profileImageBase64, Base64.DEFAULT);
                Bitmap decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                profileImage.setImageBitmap(decodedImage);
            } catch (Exception e) {
                e.printStackTrace();
                profileImage.setImageResource(R.drawable.default_profile);
            }
        } else {
            profileImage.setImageResource(R.drawable.default_profile);
        }
    }

    // **Firebase se profile image aur username update karna**
    private void loadUserProfileFromFirebase() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String username = snapshot.child("username").getValue(String.class);
                    String profileUrl = snapshot.child("profileImage").getValue(String.class);

                    if (username != null) {
                        textUsername.setText(username);
                    }

                    if (profileUrl != null && profileUrl.startsWith("http")) {
                        Glide.with(UserProfileActivity.this).load(profileUrl).into(profileImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // **Followers & Following Count Load Karna**
    private void loadFollowCounts() {
        followRef.child(userId).child("followers").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                textFollowersCount.setText("Followers: " + snapshot.getChildrenCount());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        followRef.child(userId).child("following").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                textFollowingCount.setText("Following: " + snapshot.getChildrenCount());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // **Check If User is Following**
    private void checkFollowingStatus() {
        followRef.child(currentUserId).child("following").child(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        isFollowing = snapshot.exists();
                        buttonFollow.setText(isFollowing ? "Unfollow" : "Follow");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // **Follow User**
    private void followUser() {
        followRef.child(currentUserId).child("following").child(userId).setValue(true);
        followRef.child(userId).child("followers").child(currentUserId).setValue(true);
    }

    // **Unfollow User**
    private void unfollowUser() {
        followRef.child(currentUserId).child("following").child(userId).removeValue();
        followRef.child(userId).child("followers").child(currentUserId).removeValue();
    }
}
