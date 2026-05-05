package com.KryptoChat.serwer.controllers;

public class GroupResponse {

    private Long groupId;
    private String message;
    private String code;

    public GroupResponse() {}

    public GroupResponse(Long groupId, String code, String message) {
        this.groupId = groupId;
        this.code = code;
        this.message = message;
    }

    // gettery
    public Long getGroupId() { return groupId; }
    public String getCode() { return code; }
    public String getMessage() { return message; }

    // settery
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public void setCode(String code) { this.code = code; }
    public void setMessage(String message) { this.message = message; }
}
