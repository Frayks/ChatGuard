package com.andrew.project.chatGuard.scheduler;

import com.andrew.project.chatGuard.api.entities.ChatInfo;
import com.andrew.project.chatGuard.api.executor.Executor;
import com.andrew.project.chatGuard.task.TaskRemoveUser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class SchedulerRemoveUser {

    private static final Logger LOGGER = LogManager.getLogger(SchedulerRemoveUser.class);

    private Map<String, ScheduledFuture<?>> taskMap = new HashMap<>();
    private Executor executor;
    private ScheduledExecutorService scheduledExecutorService;

    public void createTask(ChatInfo chatInfo, User user) {
        String key = createKey(chatInfo.getId(), user.getId());
        TaskRemoveUser taskRemoveUser = new TaskRemoveUser(chatInfo.getId(), user.getId(), chatInfo.isBanUser(), executor);
        ScheduledFuture<?> future = scheduledExecutorService.schedule(taskRemoveUser, chatInfo.getWaitingTime(), TimeUnit.MINUTES);
        taskMap.put(key, future);
        LOGGER.info("Created task remove user! key=" + key + ", waitingTime=" + chatInfo.getWaitingTime() + ", banUser=" + chatInfo.isBanUser());
    }


    public boolean hasTask(String key) {
        return taskMap.containsKey(key);
    }

    public void cancelTask(String key) {
        ScheduledFuture<?> scheduledFuture = taskMap.get(key);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            taskMap.remove(key);
            LOGGER.info("Removed task remove user! key=" + key);
        }
    }

    public String createKey(Long chatId, Long userId) {
        return chatId + "_" + userId;
    }

    @Autowired
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    @Autowired
    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
    }
}
