package com.KryptoChat.serwer.controllers;

public class CreateGroupRequest {

    private String username;
    private String groupName;

    CreateGroupRequest() {}

    public String getUsername() { return username; }
    public String getGroupName() { return groupName; }
    public void setUsername(String username) { this.username = username; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
}
