package com.example.synczone.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.synczone.R;
import com.example.synczone.models.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private Context context;
    private List<ChatMessage> messageList;
    private String currentUserId;
    private String currentUserRole; // "Admin", "Mod", or "Member"
    private DatabaseReference userRef;
    private HashMap<String, Bitmap> profileCache = new HashMap<>();

    public ChatAdapter(Context context, List<ChatMessage> messageList, String currentUserRole) {
        this.context = context;
        this.messageList = messageList;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.currentUserRole = currentUserRole;
        this.userRef = FirebaseDatabase.getInstance().getReference("Users");
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        holder.senderName.setText(message.getSenderName());

        // Fetch and set profile image.
        fetchUserProfileImage(message.getUserId(), holder.profileImage);

        // --- Voice Message Handling ---
        if (message.isVoiceMessage()) {
            holder.messageText.setVisibility(View.GONE);
            holder.messageImage.setVisibility(View.GONE);
            holder.llVoiceMessage.setVisibility(View.VISIBLE);
            holder.tvVoiceDuration.setText("00:00 / 00:00");
            holder.seekBarVoice.setProgress(0);
            holder.voiceMessageButton.setVisibility(View.VISIBLE);
            holder.voiceMessageButton.setOnClickListener(v -> playVoiceMessage(message.getVoiceBase64(), holder));
        } else {
            holder.llVoiceMessage.setVisibility(View.GONE);
            if (message.isImageMessage()) {
                holder.messageText.setVisibility(View.GONE);
                holder.messageImage.setVisibility(View.VISIBLE);
                if (message.getImageBase64() != null && !message.getImageBase64().isEmpty()) {
                    Bitmap bitmap = decodeBase64ToBitmap(message.getImageBase64());
                    if (bitmap != null) {
                        holder.messageImage.setImageBitmap(bitmap);
                    } else {
                        holder.messageImage.setImageResource(R.drawable.default_profile);
                        Log.e("ChatAdapter", "Image Decoding Failed: Empty Bitmap");
                    }
                } else {
                    holder.messageImage.setImageResource(R.drawable.default_profile);
                    Log.e("ChatAdapter", "Image Base64 String is Empty");
                }
            } else {
                holder.messageText.setVisibility(View.VISIBLE);
                holder.messageText.setText(message.getMessage());
                holder.messageImage.setVisibility(View.GONE);
            }
            holder.voiceMessageButton.setVisibility(View.GONE);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String formattedTime = sdf.format(new Date(message.getTimestamp()));
        holder.timestamp.setText(formattedTime);

        // --- Admin/Mod Moderation Options ---
        if ((currentUserRole.equals("Admin") || currentUserRole.equals("Mod"))
                && !message.getUserId().equals(currentUserId)) {
            holder.optionsButton.setVisibility(View.VISIBLE);
            holder.optionsButton.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(context, holder.optionsButton);
                popupMenu.getMenuInflater().inflate(R.menu.chat_message_admin_menu, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.action_delete) {
                        deleteMessage(message);
                        return true;
                    } else if (item.getItemId() == R.id.action_pin) {
                        pinMessage(message);
                        return true;
                    } else if (item.getItemId() == R.id.action_edit) {
                        editMessage(message);
                        return true;
                    } else {
                        return false;
                    }
                });
                popupMenu.show();
            });
        } else {
            holder.optionsButton.setVisibility(View.GONE);
        }
    }

    private void playVoiceMessage(String base64Audio, ChatViewHolder holder) {
        try {
            byte[] audioBytes = Base64.decode(base64Audio, Base64.DEFAULT);
            File tempAudioFile = File.createTempFile("temp_audio", ".3gp", context.getCacheDir());
            try (FileOutputStream fos = new FileOutputStream(tempAudioFile)) {
                fos.write(audioBytes);
            }

            final MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(tempAudioFile.getAbsolutePath());
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                holder.isPrepared = true;
                int duration = mediaPlayer.getDuration();
                holder.seekBarVoice.setMax(duration);
                holder.tvVoiceDuration.setText(formatTime(0) + " / " + formatTime(duration));
                mediaPlayer.start();

                Handler handler = new Handler();
                Runnable updater = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Wrap isPlaying() in its own try-catch block.
                            boolean playing;
                            try {
                                playing = mediaPlayer.isPlaying();
                            } catch (IllegalStateException e) {
                                playing = false;
                            }
                            if (mediaPlayer != null && playing) {
                                int currentPos = mediaPlayer.getCurrentPosition();
                                holder.seekBarVoice.setProgress(currentPos);
                                holder.tvVoiceDuration.setText(formatTime(currentPos) + " / " + formatTime(duration));
                                handler.postDelayed(this, 500);
                            }
                        } catch (Exception e) {
                            Log.e("ChatAdapter", "Error updating voice playback", e);
                            handler.removeCallbacks(this);
                        }
                    }
                };
                handler.post(updater);

                holder.seekBarVoice.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser && mediaPlayer != null && holder.isPrepared) {
                            try {
                                mediaPlayer.seekTo(progress);
                            } catch (IllegalStateException e) {
                                Log.e("ChatAdapter", "Error seeking MediaPlayer", e);
                            }
                        }
                    }
                    @Override public void onStartTrackingTouch(SeekBar seekBar) { }
                    @Override public void onStopTrackingTouch(SeekBar seekBar) { }
                });
            });
            mediaPlayer.setOnCompletionListener(player -> {
                player.release();
                tempAudioFile.delete();
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String formatTime(int millis) {
        int totalSeconds = millis / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void deleteMessage(ChatMessage message) {
        Toast.makeText(context, "Delete Message", Toast.LENGTH_SHORT).show();
    }

    private void pinMessage(ChatMessage message) {
        Toast.makeText(context, "Pin Message", Toast.LENGTH_SHORT).show();
    }

    private void editMessage(ChatMessage message) {
        Toast.makeText(context, "Edit Message", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView senderName, messageText, timestamp;
        ImageView messageImage, profileImage, voiceMessageButton, optionsButton;
        // Container views for voice messages.
        android.widget.LinearLayout llVoiceMessage;
        TextView tvVoiceDuration;
        SeekBar seekBarVoice;
        public boolean isPrepared = false;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            senderName = itemView.findViewById(R.id.tvSenderName);
            messageText = itemView.findViewById(R.id.tvMessage);
            timestamp = itemView.findViewById(R.id.tvTimestamp);
            messageImage = itemView.findViewById(R.id.ivMessageImage);
            profileImage = itemView.findViewById(R.id.ivUserProfile);
            voiceMessageButton = itemView.findViewById(R.id.btnPlayVoiceMessage);
            optionsButton = itemView.findViewById(R.id.ivOptions);
            llVoiceMessage = itemView.findViewById(R.id.llVoiceMessage);
            tvVoiceDuration = itemView.findViewById(R.id.tvVoiceDuration);
            seekBarVoice = itemView.findViewById(R.id.seekBarVoice);
        }
    }

    private Bitmap decodeBase64ToBitmap(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            if (bitmap == null) {
                Log.e("ChatAdapter", "Decoded bitmap is NULL!");
            } else {
                Log.d("ChatAdapter", "Image Decoded Successfully!");
            }
            return bitmap;
        } catch (Exception e) {
            Log.e("ChatAdapter", "Error decoding Base64 image", e);
            return null;
        }
    }

    private void fetchUserProfileImage(String userId, ImageView profileImage) {
        userRef.child(userId).child("profileImage").get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                String profileBase64 = snapshot.getValue(String.class);
                Log.d("ChatAdapter", "Profile Image Retrieved for " + userId);
                if (profileBase64 != null && !profileBase64.isEmpty()) {
                    Bitmap bitmap = decodeBase64ToBitmap(profileBase64);
                    if (bitmap != null) {
                        profileCache.put(userId, bitmap);
                        profileImage.setImageBitmap(bitmap);
                    } else {
                        profileImage.setImageResource(R.drawable.default_profile);
                    }
                } else {
                    profileImage.setImageResource(R.drawable.default_profile);
                }
            } else {
                Log.e("ChatAdapter", "No profile image found for user: " + userId);
                profileImage.setImageResource(R.drawable.default_profile);
            }
        }).addOnFailureListener(e -> {
            Log.e("ChatAdapter", "Failed to fetch profile image for user: " + userId, e);
            profileImage.setImageResource(R.drawable.default_profile);
        });
    }
}
