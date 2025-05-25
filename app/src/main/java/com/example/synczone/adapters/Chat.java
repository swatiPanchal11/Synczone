package com.example.synczone.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.synczone.R;
import com.example.synczone.models.ModelChat;

import java.util.List;

public class Chat extends RecyclerView.Adapter<Chat.ChatViewHolder> {

    private List<ModelChat> chatList;
    private String currentUserId;

    public Chat(List<ModelChat> chatList, String currentUserId) {
        this.chatList = chatList;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(viewType == 0 ? R.layout.item_chat_left : R.layout.item_chat_right, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ModelChat message = chatList.get(position);

        if (message.getMessage() != null && !message.getMessage().isEmpty()) {
            holder.textMessage.setVisibility(View.VISIBLE);
            holder.ivMessageImage.setVisibility(View.GONE);
            holder.textMessage.setText(message.getMessage());
        } else if (message.getImage() != null && !message.getImage().isEmpty()) {
            holder.textMessage.setVisibility(View.GONE);
            holder.ivMessageImage.setVisibility(View.VISIBLE);

            // Decode and display the Base64 image
            byte[] decodedBytes = Base64.decode(message.getImage(), Base64.DEFAULT);
            Bitmap decodedImage = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            holder.ivMessageImage.setImageBitmap(decodedImage);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String time = sdf.format(new Date(message.getTimestamp()));
        holder.textTimestamp.setText(time);
    }
    @Override
    public int getItemCount() {
        return chatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return chatList.get(position).getSenderId().equals(currentUserId) ? 1 : 0;
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage, textTimestamp;
        ImageView ivMessageImage;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.tvMessage);
            textTimestamp = itemView.findViewById(R.id.tvTimestamp);
            ivMessageImage = itemView.findViewById(R.id.ivMessageImage);
        }
    }
}
