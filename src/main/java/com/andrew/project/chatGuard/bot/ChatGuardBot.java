package com.andrew.project.chatGuard.bot;

import com.andrew.project.chatGuard.api.processor.Processor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class ChatGuardBot extends TelegramLongPollingBot {

    private static final Logger LOGGER = LogManager.getLogger(ChatGuardBot.class);
    @Value("${telegram.bot.username}")
    private String username;
    @Value("${telegram.bot.token}")
    private String token;
    private Processor processor;

    @Override

    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            processor.process(update);
        } catch (Exception e) {
            LOGGER.error(update, e);
        }
    }

    @Autowired
    public void setProcessor(Processor processor) {
        this.processor = processor;
    }

}
