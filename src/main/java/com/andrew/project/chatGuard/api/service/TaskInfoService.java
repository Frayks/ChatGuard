package com.andrew.project.chatGuard.api.service;

import com.andrew.project.chatGuard.api.entities.TaskInfo;

import java.util.List;

public interface TaskInfoService {

    TaskInfo findById(Long id);

    List<TaskInfo> findByChatId(Long chatId);

    List<TaskInfo> findByChatIdAndUserId(Long chatId, Long userId);

    List<TaskInfo> findByChatIdAndMessageId(Long chatId, Integer messageId);

    TaskInfo save(TaskInfo taskInfo);

    int deleteTaskInfoById(Long id);

    int deleteTaskInfoByChatId(Long chatId);

    void deleteAll();

}
