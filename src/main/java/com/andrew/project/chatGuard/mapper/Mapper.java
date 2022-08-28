package com.andrew.project.chatGuard.mapper;

import com.andrew.project.chatGuard.api.entities.ChatInfo;
import org.telegram.telegrambots.meta.api.objects.Chat;

public class Mapper {

    public static ChatInfo mapToChatInfo(Chat chat) {
        ChatInfo chatInfo = new ChatInfo();
        chatInfo.setId(chat.getId());
        chatInfo.setType(chat.getType());
        chatInfo.setTitle(chat.getTitle());
        chatInfo.setUserName(chat.getUserName());
        return chatInfo;
    }

}
