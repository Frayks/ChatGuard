package com.andrew.project.chatGuard.scheduler;

import com.andrew.project.chatGuard.api.entities.ChatInfo;
import com.andrew.project.chatGuard.api.executor.Executor;
import com.andrew.project.chatGuard.task.TaskRemoveUser;
import com.andrew.project.chatGuard.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class SchedulerRemoveUser {

    private static final Logger LOGGER = LogManager.getLogger(SchedulerRemoveUser.class);

    private final Map<String, ScheduledFuture<?>> taskMap = new HashMap<>();
    private Executor executor;
    private Util util;
    private ScheduledExecutorService scheduledExecutorService;
    public static final String DELIMITER = "_";

    public void createTask(ChatInfo chatInfo, Message sentMessage, User user) {
        String key = createKey(chatInfo.getId(), sentMessage.getMessageId(), user.getId());
        TaskRemoveUser taskRemoveUser = new TaskRemoveUser(chatInfo, user, executor, util);
        ScheduledFuture<?> future = scheduledExecutorService.schedule(taskRemoveUser, chatInfo.getWaitingTime(), TimeUnit.MINUTES);
        taskMap.put(key, future);
        LOGGER.info("Created task remove user! key=" + key + ", waitingTime=" + chatInfo.getWaitingTime() + ", banUser=" + chatInfo.isBanUser());
    }

    public List<String> getTasksKeysByParams(Long chatId, Integer messageId, Long userId) {
        List<String> keyList = new ArrayList<>(taskMap.keySet());
        keyList.removeIf(key -> !keyHasMatchingParts(key, chatId, messageId, userId));
        return keyList;
    }

    public void cancelTasks(List<String> keyList) {
        for (String key : keyList) {
            ScheduledFuture<?> scheduledFuture = taskMap.get(key);
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
                taskMap.remove(key);
                LOGGER.info("Removed task remove user! key=" + key);
            }
        }
    }

    private boolean keyHasMatchingParts(String key, Long chatId, Integer messageId, Long userId) {
        String[] partsOfKey = key.split(DELIMITER);
        boolean chatIdEquals = chatId == null || chatId.equals(Long.parseLong(partsOfKey[0]));
        boolean messageIdEquals = messageId == null || messageId.equals(Integer.parseInt(partsOfKey[1]));
        boolean userIdEquals = userId == null || userId.equals(Long.parseLong(partsOfKey[2]));
        return chatIdEquals && messageIdEquals && userIdEquals;
    }

    private String createKey(Long chatId, Integer messageId, Long userId) {
        return chatId + DELIMITER + messageId + DELIMITER + userId;
    }

    public Long getChatId(String key) {
        String[] partsOfKey = key.split(DELIMITER);
        return Long.parseLong(partsOfKey[0]);
    }

    public Integer getMessageId(String key) {
        String[] partsOfKey = key.split(DELIMITER);
        return Integer.parseInt(partsOfKey[1]);
    }

    public Long detUserId(String key) {
        String[] partsOfKey = key.split(DELIMITER);
        return Long.parseLong(partsOfKey[2]);
    }

    @Autowired
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    @Autowired
    public void setUtil(Util util) {
        this.util = util;
    }

    @Autowired
    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
    }
}
