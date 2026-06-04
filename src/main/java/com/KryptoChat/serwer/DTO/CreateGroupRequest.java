package com.KryptoChat.serwer.DTO;

/**
 * Klasa definiująca obiekt utworzenia grupy, potrzebna do deserializacji ciała requesta
 */
public class CreateGroupRequest {

    private String groupName;

    public String creatorKey;

    /**
     * Konstruktor bezparametrowy, konieczny dla deserializacji i serializacji
     */
    public CreateGroupRequest() {}

    /**
     * Getter nazwy grupy
     * @return String groupName
     */
    public String getGroupName() { return groupName; }

    /**
     * Setter nazwy grupy
     * @param groupName - ustawiana nazwa grupy
     */
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getCreatorKey() { return creatorKey; }
    public void setCreatorKey(String key) { creatorKey = key; }
}
