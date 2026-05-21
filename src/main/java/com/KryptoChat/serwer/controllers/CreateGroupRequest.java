package com.KryptoChat.serwer.controllers;

/**
 * Klasa definiująca obiekt utworzenia grupy, potrzebna do deserializacji ciała requesta
 */
public class CreateGroupRequest {

    private String groupName;

    /**
     * Konstruktor bezparametrowy, konieczny dla deserializacji i serializacji
     */
    CreateGroupRequest() {}

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
}
