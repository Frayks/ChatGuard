package com.andrew.project.chatGuard.api.service;

import com.andrew.project.chatGuard.api.entities.TaskInfo;

import java.util.List;

public interface TaskInfoService {

    TaskInfo findById(Long id);

    List<TaskInfo> findByChatIdAndUserId(Long chatId, Long userId);

    List<TaskInfo> findByChatIdAndMessageId(Long chatId, Integer messageId);

    TaskInfo save(TaskInfo taskInfo);

    int deleteTaskInfoRemoveUserById(Long id);

    void deleteAll();

}
