package com.KryptoChat.serwer.controllers;

import java.util.List;

public class GroupDetailsResponse {

    private Long groupId;
    private String groupName;
    private String code;
    private List<UserResponse> users;

    public GroupDetailsResponse(Long groupId, String groupName, String code, List<UserResponse> users) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.code = code;
        this.users = users;
    }

    public Long getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getCode() {
        return code;
    }

    public List<UserResponse> getUsers() {
        return users;
    }
}