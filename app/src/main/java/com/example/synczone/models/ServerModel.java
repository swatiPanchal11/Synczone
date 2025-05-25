package com.example.synczone.models;

import android.graphics.Bitmap;
import java.util.Map;

public class ServerModel {
    private String createdBy;
    private String inviteCode;
    private Map<String, MemberModel> members;
    private String serverIcon;
    private String serverName;
    private String serverId;
    private String serverDescription;

    // New field for decoded image (not saved to Firebase)
    private transient Bitmap decodedImage;

    // Empty constructor (required for Firebase)
    public ServerModel() {
    }

    // Constructor
    public ServerModel(String createdBy, String inviteCode, Map<String, MemberModel> members,
                       String serverIcon, String serverName, String serverId, String serverDescription) {
        this.createdBy = createdBy;
        this.inviteCode = inviteCode;
        this.members = members;
        this.serverIcon = serverIcon;
        this.serverName = serverName;
        this.serverId = serverId;
        this.serverDescription = serverDescription;
    }

    // Getters and Setters
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }

    public Map<String, MemberModel> getMembers() { return members; }
    public void setMembers(Map<String, MemberModel> members) { this.members = members; }

    public String getServerIcon() { return serverIcon; }
    public void setServerIcon(String serverIcon) { this.serverIcon = serverIcon; }

    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }

    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }

    public String getServerDescription() { return serverDescription; }
    public void setServerDescription(String serverDescription) { this.serverDescription = serverDescription; }

    // Setter and Getter for Decoded Image
    public Bitmap getDecodedImage() { return decodedImage; }
    public void setDecodedImage(Bitmap decodedImage) { this.decodedImage = decodedImage; }
}
