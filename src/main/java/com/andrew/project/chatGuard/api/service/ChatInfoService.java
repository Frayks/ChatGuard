package com.andrew.project.chatGuard.api.service;


import com.andrew.project.chatGuard.api.entities.ChatInfo;

import java.util.List;

public interface ChatInfoService {

    List<ChatInfo> findAll();

    ChatInfo findById(Long id);

    ChatInfo save(ChatInfo user);

    int deleteChatInfoById(Long id);

}
