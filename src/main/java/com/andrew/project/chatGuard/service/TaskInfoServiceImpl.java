package com.andrew.project.chatGuard.service;

import com.andrew.project.chatGuard.api.entities.TaskInfo;
import com.andrew.project.chatGuard.api.repository.TaskInfoRepository;
import com.andrew.project.chatGuard.api.service.TaskInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TaskInfoServiceImpl implements TaskInfoService {

    private TaskInfoRepository taskInfoRepository;

    @Override
    public TaskInfo findById(Long id) {
        return taskInfoRepository.findById(id).orElse(null);
    }

    @Override
    public List<TaskInfo> findByChatIdAndUserId(Long chatId, Long userId) {
        return taskInfoRepository.findByChatIdAndUserId(chatId, userId);
    }

    @Override
    public List<TaskInfo> findByChatIdAndMessageId(Long chatId, Integer messageId) {
        return taskInfoRepository.findByChatIdAndMessageId(chatId, messageId);
    }

    @Override
    public TaskInfo save(TaskInfo taskInfo) {
        return taskInfoRepository.save(taskInfo);
    }

    @Override
    public int deleteTaskInfoRemoveUserById(Long id) {
        return taskInfoRepository.deleteTaskInfoRemoveUserById(id);
    }

    @Override
    public void deleteAll() {
        taskInfoRepository.deleteAll();
    }

    @Autowired
    public void setTaskInfoRemoveUserRepository(TaskInfoRepository taskInfoRepository) {
        this.taskInfoRepository = taskInfoRepository;
    }

}
