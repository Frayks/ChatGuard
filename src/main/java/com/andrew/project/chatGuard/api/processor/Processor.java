package com.andrew.project.chatGuard.api.processor;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.ChatMemberUpdated;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface Processor {

    void processMessage(Message message);

    void processCallbackQuery(CallbackQuery callbackQuery);

    void processMyChatMember(ChatMemberUpdated chatMemberUpdated);

    default void process(Update update) {
        if (update.hasMessage()) {
            processMessage(update.getMessage());
        } else if (update.hasMyChatMember()) {
            processMyChatMember(update.getMyChatMember());
        } else if (update.hasCallbackQuery()) {
            processCallbackQuery(update.getCallbackQuery());
        }
    }

}
