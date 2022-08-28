package com.andrew.project.chatGuard.service;


import com.andrew.project.chatGuard.api.entities.ChatInfo;
import com.andrew.project.chatGuard.api.repository.ChatInfoRepository;
import com.andrew.project.chatGuard.api.service.ChatInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ChatInfoServiceImpl implements ChatInfoService {

    private ChatInfoRepository chatInfoRepository;


    @Override
    public List<ChatInfo> findAll() {
        return chatInfoRepository.findAll();
    }

    @Override
    @Cacheable(value = "chatsInfo", unless = "#result == null")
    public ChatInfo findById(Long id) {
        return chatInfoRepository.findById(id).orElse(null);
    }

    @Override
    @CacheEvict(value = "chatsInfo", key = "#chatInfo.id")
    public ChatInfo save(ChatInfo chatInfo) {
        return chatInfoRepository.save(chatInfo);
    }

    @Override
    @CacheEvict("chatsInfo")
    public int deleteChatInfoById(Long id) {
        return chatInfoRepository.deleteChatInfoById(id);
    }

    @Autowired
    public void setChatInfoRepository(ChatInfoRepository chatInfoRepository) {
        this.chatInfoRepository = chatInfoRepository;
    }
}
