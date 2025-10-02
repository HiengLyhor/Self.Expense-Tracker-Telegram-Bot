package com.telegram.expense_tracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Entity
@Table(name = "user_info", schema = "public")
@Getter @Setter
public class UserInfo {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "username")
    private String username;

    @Column(name = "created_on")
    private Timestamp createdOn = new Timestamp(System.currentTimeMillis());

    protected UserInfo() {}

    public UserInfo(String username, Long userId) {
        this.username = username;
        this.id = userId;
    }

}
