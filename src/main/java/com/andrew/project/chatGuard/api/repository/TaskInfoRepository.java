package com.andrew.project.chatGuard.api.repository;

import com.andrew.project.chatGuard.api.entities.TaskInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface TaskInfoRepository extends JpaRepository<TaskInfo, Long> {

    List<TaskInfo> findByChatIdAndUserId(Long chatId, Long userId);

    List<TaskInfo> findByChatIdAndMessageId(Long chatId, Integer messageId);

    int deleteTaskInfoRemoveUserById(Long id);

}
