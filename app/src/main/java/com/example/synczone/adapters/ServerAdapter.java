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
import com.example.synczone.models.ServerModel;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ServerAdapter extends RecyclerView.Adapter<ServerAdapter.ServerViewHolder> {

    private Context context;
    private List<ServerModel> serverList;
    private OnServerClickListener onServerClickListener;

    // 🔹 Interface for Click Listener
    public interface OnServerClickListener {
        void onServerClick(ServerModel server);
    }

    // 🔹 Updated Constructor with Context and Click Listener
    public ServerAdapter(Context context, List<ServerModel> serverList, OnServerClickListener listener) {
        this.context = context;
        this.serverList = serverList;
        this.onServerClickListener = listener;
    }

    @NonNull
    @Override
    public ServerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_server, parent, false);
        return new ServerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServerViewHolder holder, int position) {
        ServerModel server = serverList.get(position);

        // 🔹 Set Server Name
        holder.serverName.setText(server.getServerName());

        // 🔹 Set Server Description
        holder.serverDescription.setText(server.getServerDescription());

        // 🔹 Decode Base64 Image and Set to CircleImageView
        if (server.getServerIcon() != null && !server.getServerIcon().isEmpty()) {
            byte[] decodedBytes = Base64.decode(server.getServerIcon(), Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            holder.serverIcon.setImageBitmap(bitmap);
        } else {
            holder.serverIcon.setImageResource(R.drawable.ic_server_placeholder); // Default Image
        }

        // 🔹 Handle Click Event
        holder.itemView.setOnClickListener(v -> {
            if (onServerClickListener != null) {
                onServerClickListener.onServerClick(server);
            }
        });
    }

    @Override
    public int getItemCount() {
        return serverList.size();
    }

    static class ServerViewHolder extends RecyclerView.ViewHolder {
        TextView serverName, serverDescription;
        CircleImageView serverIcon;

        public ServerViewHolder(@NonNull View itemView) {
            super(itemView);
            serverName = itemView.findViewById(R.id.serverName);
            serverDescription = itemView.findViewById(R.id.serverDescription);
            serverIcon = itemView.findViewById(R.id.serverImage);
        }
    }
}
