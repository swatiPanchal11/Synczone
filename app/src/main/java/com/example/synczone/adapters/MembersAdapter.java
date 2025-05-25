package com.example.synczone.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.synczone.R;
import com.example.synczone.models.MemberModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.MemberViewHolder> {
    private Context context;
    private List<MemberModel> membersList;
    private DatabaseReference usersRef, serverRef;
    private String currentUserId;
    private String serverId;
    private String currentUserRole; // Field for the logged-in user's role

    public MembersAdapter(Context context, List<MemberModel> membersList, String serverId, String currentUserId, String loggedInUserRole) {
        this.context = context;
        this.membersList = (membersList != null) ? membersList : new ArrayList<>();
        this.serverId = serverId;
        this.currentUserId = currentUserId;
        // Use the parameter "loggedInUserRole" to set the field.
        this.currentUserRole = (loggedInUserRole != null) ? loggedInUserRole.trim() : "";
        if(this.currentUserRole.isEmpty()){
            Log.e("MembersAdapter", "currentUserRole is empty. Fetching from database for currentUserId: " + currentUserId);
            // Fetch the current user's role from Firebase.
            FirebaseDatabase.getInstance().getReference("servers")
                    .child(serverId).child("members")
                    .child(currentUserId).child("role")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.exists()){
                                // Update the field (using MembersAdapter.this.currentUserRole)
                                MembersAdapter.this.currentUserRole = snapshot.getValue(String.class);
                                Log.d("MembersAdapter", "Fetched current user role: " + MembersAdapter.this.currentUserRole);
                                notifyDataSetChanged(); // refresh the list once the role is fetched
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) { }
                    });
        } else {
            Log.d("MembersAdapter", "Logged in user role (from constructor): " + this.currentUserRole);
        }
        this.usersRef = FirebaseDatabase.getInstance().getReference("Users");
        this.serverRef = FirebaseDatabase.getInstance().getReference("servers").child(serverId).child("members");
        fetchMembers();
    }

    private void fetchMembers() {
        serverRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                membersList.clear();
                for (DataSnapshot data : task.getResult().getChildren()) {
                    MemberModel member = data.getValue(MemberModel.class);
                    if (member != null) {
                        membersList.add(member);
                    }
                }
                notifyDataSetChanged();
                Log.d("RecyclerViewUpdate", "Members list updated: " + membersList.size());
            } else {
                Log.e("FirebaseError", "Error fetching members: " + task.getException().getMessage());
            }
        });
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        MemberModel member = membersList.get(position);
        String userId = member.getUserId();

        if (serverId == null || userId == null) return;

        // Reset views to default state.
        holder.roleSpinner.setVisibility(View.GONE);
        holder.removeMemberButton.setVisibility(View.GONE);

        // Fetch and display user details.
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String username = snapshot.child("username").getValue(String.class);
                    String profileImage = snapshot.child("profileImage").getValue(String.class);
                    holder.usernameTextView.setText(username != null ? username : "Unknown");

                    if (profileImage != null && !profileImage.isEmpty()) {
                        holder.profileImageView.setImageBitmap(decodeBase64Image(profileImage));
                    } else {
                        holder.profileImageView.setImageResource(R.drawable.default_profile);
                    }
                }
            }

            private Bitmap decodeBase64Image(String base64Image) {
                try {
                    byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                    return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                } catch (Exception e) {
                    Log.e("DecodeImage", "Error decoding image", e);
                    return null;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        // Fetch member role and set up role management UI.
        serverRef.child(userId).child("role").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Use a local variable for the member's role.
                    String memberRole = snapshot.getValue(String.class);
                    holder.roleTextView.setText(memberRole);
                    Log.d("RoleCheck", "Logged in user role: " + currentUserRole + ", member (" + userId + ") role: " + memberRole);

                    // Do not show any controls for the logged-in admin's own row.
                    if (userId.equals(currentUserId)) {
                        holder.roleSpinner.setVisibility(View.GONE);
                        holder.removeMemberButton.setVisibility(View.GONE);
                    }
                    // If logged in user is Admin, enable role management (both spinner and remove button) for other members.
                    else if (currentUserRole.equals("Admin")) {
                        setupRoleManagement(holder, userId, memberRole);
                    }
                    // If logged in user is Mod and the member is a Member, allow removal only.
                    else if (currentUserRole.equals("Mod") && memberRole.equals("Member")) {
                        holder.roleSpinner.setVisibility(View.GONE);
                        holder.removeMemberButton.setVisibility(View.VISIBLE);
                        holder.removeMemberButton.setOnClickListener(v ->
                                removeMember(userId, holder.usernameTextView.getText().toString())
                        );
                    }
                    // Otherwise, hide both controls.
                    else {
                        holder.roleSpinner.setVisibility(View.GONE);
                        holder.removeMemberButton.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void setupRoleManagement(MemberViewHolder holder, String userId, String memberRole) {
        // For role management by Admin, show both spinner and remove button.
        holder.roleSpinner.setVisibility(View.VISIBLE);
        holder.removeMemberButton.setVisibility(View.VISIBLE);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                context, R.array.roles, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        holder.roleSpinner.setAdapter(adapter);

        // Set spinner selection based on memberRole.
        // Assuming roles are ordered as: "Member", "Mod", "Admin" in R.array.roles.
        int selectedIndex = memberRole.equals("Mod") ? 1 : memberRole.equals("Admin") ? 2 : 0;
        holder.roleSpinner.setSelection(selectedIndex);

        // Use a flag to prevent triggering onItemSelected during initial setup.
        holder.roleSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            boolean isFirstSelection = true;
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                if (isFirstSelection) {
                    isFirstSelection = false;
                    return;
                }
                String newRole = parent.getItemAtPosition(pos).toString();
                // Update role only if there's a change.
                if (!newRole.equals(memberRole)) {
                    updateMemberRole(userId, newRole);
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        holder.removeMemberButton.setOnClickListener(v ->
                removeMember(userId, holder.usernameTextView.getText().toString())
        );
    }

    private void removeMember(String memberId, String username) {
        new AlertDialog.Builder(context)
                .setTitle("Remove Member")
                .setMessage("Are you sure you want to remove " + username + "?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    serverRef.child(memberId).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(context, "Member removed", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(context, "Failed to remove member", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void updateMemberRole(String memberId, String newRole) {
        serverRef.child(memberId).child("role").setValue(newRole)
                .addOnSuccessListener(aVoid -> Toast.makeText(context, "Role updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(context, "Failed to update role", Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return membersList.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView usernameTextView, roleTextView;
        CircleImageView profileImageView;
        ImageButton removeMemberButton;
        Spinner roleSpinner;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameTextView = itemView.findViewById(R.id.usernameTextView);
            roleTextView = itemView.findViewById(R.id.roleTextView);
            profileImageView = itemView.findViewById(R.id.profileImageView);
            removeMemberButton = itemView.findViewById(R.id.removeMemberButton);
            roleSpinner = itemView.findViewById(R.id.roleSpinner);
        }
    }
}
