package com.andrew.project.chatGuard.task;

import com.andrew.project.chatGuard.api.entities.ChatInfo;
import com.andrew.project.chatGuard.api.executor.Executor;
import com.andrew.project.chatGuard.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.meta.api.methods.groupadministration.BanChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.UnbanChatMember;
import org.telegram.telegrambots.meta.api.objects.User;

public class TaskRemoveUser implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(TaskRemoveUser.class);

    private final ChatInfo chatInfo;
    private final User user;
    private final Executor executor;
    private final Util util;

    public TaskRemoveUser(ChatInfo chatInfo, User user, Executor executor, Util util) {
        this.chatInfo = chatInfo;
        this.user = user;
        this.executor = executor;
        this.util = util;
    }

    @Override
    public void run() {
        LOGGER.info("Started task remove user! chatId=" + chatInfo.getId() + ", userId=" + user.getId() + ", banUser=" + chatInfo.isBanUser());
        BanChatMember banChatMember = BanChatMember.builder()
                .chatId(chatInfo.getId())
                .userId(user.getId())
                .build();
        executor.execute(banChatMember);
        if (!chatInfo.isBanUser()) {
            UnbanChatMember unbanChatMember = UnbanChatMember.builder()
                    .chatId(chatInfo.getId())
                    .userId(user.getId())
                    .onlyIfBanned(true)
                    .build();
            executor.execute(unbanChatMember);
        }
        executor.execute(util.createUserBannedMessage(chatInfo, user));
    }
}
