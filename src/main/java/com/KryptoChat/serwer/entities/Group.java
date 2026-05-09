package com.KryptoChat.serwer.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "groups")
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long id;

    @Column(name = "group_name",nullable = false)
    private String groupName;

    @Column(name = "code", unique = true, nullable = false)
    private String kod;

    public Group() {}

    public Group(String groupName, String kod) {
        this.groupName = groupName;
        this.kod = kod;
    }

    public Long getId() { return id; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getKod() { return kod; }
    public void setKod(String kod) { this.kod = kod; }
}


