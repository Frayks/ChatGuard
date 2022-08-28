package com.andrew.project.chatGuard.executor;

import com.andrew.project.chatGuard.api.executor.Executor;
import com.andrew.project.chatGuard.bot.ChatGuardBot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendAnimation;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.Serializable;

@Component
public class DefaultExecutorImpl implements Executor {

    private static final Logger LOGGER = LogManager.getLogger(DefaultExecutorImpl.class);

    private ChatGuardBot chatGuardBot;

    @Override
    public <T extends Serializable, Method extends BotApiMethod<T>> T execute(Method method) {
        try {
            return chatGuardBot.execute(method);
        } catch (TelegramApiException e) {
            LOGGER.error("", e);
        }
        return null;
    }

    @Override
    public void execute(SendAnimation sendAnimation) {
        try {
            chatGuardBot.execute(sendAnimation);
        } catch (TelegramApiException e) {
            LOGGER.error("", e);
        }
    }

    @Autowired
    public void setChatGuardBot(ChatGuardBot chatGuardBot) {
        this.chatGuardBot = chatGuardBot;
    }

}
