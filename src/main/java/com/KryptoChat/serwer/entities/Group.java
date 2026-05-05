package com.KryptoChat.serwer.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "groups")
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long group_id;

    @Column(nullable = false)
    private String group_name;

    @Column(name = "code", unique = true, nullable = false)
    private String kod;

    public Group() {}

    public Group(String groupName, String kod) {
        this.group_name = groupName;
        this.kod = kod;
    }

    public Long getId() { return group_id; }

    public String getGroupName() { return group_name; }
    public void setGroupName(String groupName) { this.group_name = groupName; }

    public String getKod() { return kod; }
    public void setKod(String kod) { this.kod = kod; }
}


