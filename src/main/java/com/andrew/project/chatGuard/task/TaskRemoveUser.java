package com.andrew.project.chatGuard.task;

import com.andrew.project.chatGuard.api.executor.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.meta.api.methods.groupadministration.BanChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.UnbanChatMember;

public class TaskRemoveUser implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(TaskRemoveUser.class);

    private Long chatId;
    private Long userId;
    private boolean banUser;
    private Executor executor;

    public TaskRemoveUser(Long chatId, Long userId, boolean banUser, Executor executor) {
        this.chatId = chatId;
        this.userId = userId;
        this.banUser = banUser;
        this.executor = executor;
    }

    @Override
    public void run() {
        LOGGER.info("Started task remove user! chatId=" + chatId + ", userId=" + userId + ", banUser=" + banUser);
        BanChatMember banChatMember = BanChatMember.builder()
                .chatId(chatId)
                .userId(userId)
                .build();
        executor.execute(banChatMember);
        if (!banUser) {
            UnbanChatMember unbanChatMember = UnbanChatMember.builder()
                    .chatId(chatId)
                    .userId(userId)
                    .onlyIfBanned(true)
                    .build();
            executor.execute(unbanChatMember);
        }
    }
}
