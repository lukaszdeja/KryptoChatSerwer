package com.KryptoChat.serwer.DTO;

/**
 * DTO zapytań o dołączenie do grupy
 */
public class JoinGroupRequest {

    private String code;

    /**
     * Konstruktor bezparametrowy potrzebny do serializacji i deserializacji
     */
    public JoinGroupRequest() {}

    /**
     * Getter kodu potrzebny do serializacji i deserializacji
     * @return String code
     */
    public String getCode() { return code; }

    /**
     * Setter kodu potrzebny do serializacji i deserializacji
     * @param code
     */
    public void setCode(String code) { this.code = code; }
}
