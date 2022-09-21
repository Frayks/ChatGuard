package com.andrew.project.chatGuard.scheduler;

import com.andrew.project.chatGuard.api.entities.ChatInfo;
import com.andrew.project.chatGuard.api.entities.TaskInfo;
import com.andrew.project.chatGuard.api.executor.Executor;
import com.andrew.project.chatGuard.api.service.TaskInfoService;
import com.andrew.project.chatGuard.task.TaskRemoveUser;
import com.andrew.project.chatGuard.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.groupadministration.BanChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.UnbanChatMember;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class SchedulerRemoveUser implements InitializingBean {

    private static final Logger LOGGER = LogManager.getLogger(SchedulerRemoveUser.class);

    private final Map<Long, ScheduledFuture<?>> taskMap = new HashMap<>();
    private TaskInfoService taskInfoService;
    private Executor executor;
    private Util util;
    private ScheduledExecutorService scheduledExecutorService;

    @Override
    public void afterPropertiesSet() throws Exception {
        taskInfoService.deleteAll();
    }

    public void createTask(ChatInfo chatInfo, Message sentMessage, User user) {
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setChatId(chatInfo.getId());
        taskInfo.setMessageId(sentMessage.getMessageId());
        taskInfo.setUserId(user.getId());
        taskInfo.setFirstName(user.getFirstName());
        taskInfo = taskInfoService.save(taskInfo);
        TaskRemoveUser taskRemoveUser = new TaskRemoveUser(taskInfo.getId(), this);
        ScheduledFuture<?> future = scheduledExecutorService.schedule(taskRemoveUser, chatInfo.getWaitingTime(), TimeUnit.MINUTES);
        taskMap.put(taskInfo.getId(), future);
        LOGGER.info("Created task remove user! taskId=" + taskInfo.getId() + ", waitingTime=" + chatInfo.getWaitingTime() + ", banUser=" + chatInfo.isBanUser());
    }

    public void cancelTasks(List<TaskInfo> taskInfoList) {
        for (TaskInfo taskInfo : taskInfoList) {
            Long taskId = taskInfo.getId();
            ScheduledFuture<?> scheduledFuture = taskMap.get(taskId);
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
                taskMap.remove(taskId);
                taskInfoService.deleteTaskInfoById(taskId);
                LOGGER.info("Removed task remove user! taskId=" + taskId);
            }
        }
    }

    public void executeTaskRemoveUser(Long id) {
        TaskInfo taskInfo = taskInfoService.findById(id);
        LOGGER.info("Started task remove user! id=" + id +
                ", chatId=" + taskInfo.getChatId() +
                ", userId=" + taskInfo.getUserId() +
                ", banUser=" + taskInfo.isBanUser());
        BanChatMember banChatMember = BanChatMember.builder()
                .chatId(taskInfo.getChatId())
                .userId(taskInfo.getUserId())
                .build();
        taskInfoService.deleteTaskInfoById(id);
        executor.execute(util.createUserBannedMessage(taskInfo));
        executor.execute(banChatMember);
        if (!taskInfo.isBanUser()) {
            UnbanChatMember unbanChatMember = UnbanChatMember.builder()
                    .chatId(taskInfo.getChatId())
                    .userId(taskInfo.getUserId())
                    .onlyIfBanned(true)
                    .build();
            executor.execute(unbanChatMember);
        }
        List<TaskInfo> activeTaskList = taskInfoService.findByChatIdAndMessageId(taskInfo.getChatId(), taskInfo.getMessageId());
        if (activeTaskList.isEmpty()) {
            DeleteMessage deleteMessage = DeleteMessage.builder()
                    .chatId(taskInfo.getChatId())
                    .messageId(taskInfo.getMessageId())
                    .build();
            executor.execute(deleteMessage);
        }
    }


    @Autowired
    public void setTaskInfoRemoveUserService(TaskInfoService taskInfoService) {
        this.taskInfoService = taskInfoService;
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
