package com.andrew.project.chatGuard.api.repository;

import com.andrew.project.chatGuard.api.entities.ChatInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface ChatInfoRepository extends JpaRepository<ChatInfo, Long> {

    int deleteChatInfoById(Long id);

}
