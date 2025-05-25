package com.example.synczone.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.synczone.R;
import com.example.synczone.models.DirectChat;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class DirectChatAdapter extends RecyclerView.Adapter<DirectChatAdapter.ViewHolder> {

    private List<DirectChat> chatList;
    private Context context;
    private OnChatClickListener listener;

    // Interface for click listener
    public interface OnChatClickListener {
        void onChatClick(String userId);
    }

    // Constructor
    public DirectChatAdapter(List<DirectChat> chatList, Context context, OnChatClickListener listener) {
        this.chatList = chatList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.ic_direct_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DirectChat chat = chatList.get(position);

        // Fetch username and profile image from Firebase
        fetchUserData(chat.getUserId(), holder);

        // Set last message
        holder.textLastMessage.setText(chat.getLastMessage());

        // Handle click event
        holder.itemView.setOnClickListener(v -> listener.onChatClick(chat.getUserId()));
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    // Method to fetch user data from Firebase
    private void fetchUserData(String userId, ViewHolder holder) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String username = snapshot.child("username").getValue(String.class);
                    String profileImage = snapshot.child("profileImage").getValue(String.class);

                    holder.textUsername.setText(username != null ? username : "Unknown User");

                    // Decode Base64 profile image or set default
                    if (profileImage != null && !profileImage.isEmpty()) {
                        Bitmap decodedImage = decodeBase64Image(profileImage);
                        if (decodedImage != null) {
                            holder.imageProfile.setImageBitmap(decodedImage);
                        } else {
                            holder.imageProfile.setImageResource(R.drawable.default_profile);
                        }
                    } else {
                        holder.imageProfile.setImageResource(R.drawable.default_profile);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle database error
            }
        });
    }

    // Method to decode Base64 string into Bitmap
    private Bitmap decodeBase64Image(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ViewHolder class
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textUsername, textLastMessage;
        CircleImageView imageProfile;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textUsername = itemView.findViewById(R.id.textUsername);
            textLastMessage = itemView.findViewById(R.id.textLastMessage);
            imageProfile = itemView.findViewById(R.id.imageProfile);
        }
    }
}
