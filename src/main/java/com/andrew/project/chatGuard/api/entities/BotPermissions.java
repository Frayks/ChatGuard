package com.andrew.project.chatGuard.api.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
public class BotPermissions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private boolean canBeEdited;
    private boolean canManageChat;
    private boolean canPostMessages;
    private boolean canEditMessages;
    private boolean canDeleteMessages;
    private boolean canRestrictMembers;
    private boolean canPromoteMembers;
    private boolean canChangeInfo;
    private boolean canInviteUsers;
    private boolean canPinMessages;
    private boolean canManageVideoChats;

    @OneToOne(mappedBy = "botPermissions")
    @JoinColumn(nullable = false)
    private ChatInfo chatInfo;

    public void disableAll() {
        canBeEdited = false;
        canManageChat = false;
        canPostMessages = false;
        canEditMessages = false;
        canDeleteMessages = false;
        canRestrictMembers = false;
        canPromoteMembers = false;
        canChangeInfo = false;
        canInviteUsers = false;
        canPinMessages = false;
        canManageVideoChats = false;
    }

}
