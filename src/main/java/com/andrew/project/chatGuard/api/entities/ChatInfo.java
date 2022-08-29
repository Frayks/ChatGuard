package com.andrew.project.chatGuard.api.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

@Getter
@Setter
@ToString
@Entity
public class ChatInfo {

    @Id
    private Long id;
    private String type;
    private String title;
    private String userName;
    private int waitingTime = 5;
    private boolean banUser;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(nullable = false)
    private BotPermissions botPermissions = new BotPermissions();

}
