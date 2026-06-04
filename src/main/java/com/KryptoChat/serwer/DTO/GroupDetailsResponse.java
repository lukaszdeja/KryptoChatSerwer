package com.KryptoChat.serwer.DTO;

import java.util.List;

/**
 * Klasa DTO odpowiedzi na żądanie członków grupy
 */
public class GroupDetailsResponse {

    private Long groupId;
    private String groupName;
    private String code;
    private List<UserResponse> users;

    /**
     * Konstruktor inicjujący pola klasy
     * @param groupId
     * @param groupName
     * @param code
     * @param users
     */
    public GroupDetailsResponse(Long groupId, String groupName, String code, List<UserResponse> users) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.code = code;
        this.users = users;
    }

    /**
     * Getter id grupy
     * @return Long groupId
     */
    public Long getGroupId() {
        return groupId;
    }

    /**
     * Getter nazwy grupy
     * @return String groupName
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * Getter kodu grupy
     * @return String code
     */
    public String getCode() {
        return code;
    }

    /**
     * Getter listy użytkowników
     * @return List<UserResponse> users
     */
    public List<UserResponse> getUsers() {
        return users;
    }
}