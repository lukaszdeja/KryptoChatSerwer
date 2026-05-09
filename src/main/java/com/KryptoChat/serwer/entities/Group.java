package com.KryptoChat.serwer.entities;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "groups")
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long id;

    @Column(name = "group_name", nullable = false)
    private String groupName;

    @Column(name = "code", unique = true, nullable = false)
    private String kod;

    @OneToMany(mappedBy = "group")
    private List<User> users = new ArrayList<>();

    public Group() {}

    public Group(String groupName, String kod) {
        this.groupName = groupName;
        this.kod = kod;
    }

    public Long getId() {
        return id;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getKod() {
        return kod;
    }

    public void setKod(String kod) {
        this.kod = kod;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}