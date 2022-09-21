package com.andrew.project.chatGuard.util;

import com.andrew.project.chatGuard.api.constant.BotCommand;
import com.andrew.project.chatGuard.api.constant.RemoveType;
import com.andrew.project.chatGuard.api.entities.BotPermissions;
import com.andrew.project.chatGuard.api.entities.ChatInfo;
import com.andrew.project.chatGuard.api.entities.TaskInfo;
import com.andrew.project.chatGuard.api.property.Translations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendAnimation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
public class Util {

    private Translations translations;
    @Value("${telegram.bot.username}")
    private String username;
    private static final String PARSE_MODE_MARKDOWN = "markdown";

    public void updateBotPermissionList(ChatInfo chatInfo, ChatMemberAdministrator chatMemberAdministrator) {
        BotPermissions botPermissions = chatInfo.getBotPermissions();
        botPermissions.setCanBeEdited(Boolean.TRUE.equals(chatMemberAdministrator.getCanBeEdited()));
        botPermissions.setCanManageChat(Boolean.TRUE.equals(chatMemberAdministrator.getCanManageChat()));
        botPermissions.setCanPostMessages(Boolean.TRUE.equals(chatMemberAdministrator.getCanPostMessages()));
        botPermissions.setCanEditMessages(Boolean.TRUE.equals(chatMemberAdministrator.getCanEditMessages()));
        botPermissions.setCanDeleteMessages(Boolean.TRUE.equals(chatMemberAdministrator.getCanDeleteMessages()));
        botPermissions.setCanRestrictMembers(Boolean.TRUE.equals(chatMemberAdministrator.getCanRestrictMembers()));
        botPermissions.setCanPromoteMembers(Boolean.TRUE.equals(chatMemberAdministrator.getCanPromoteMembers()));
        botPermissions.setCanChangeInfo(Boolean.TRUE.equals(chatMemberAdministrator.getCanChangeInfo()));
        botPermissions.setCanInviteUsers(Boolean.TRUE.equals(chatMemberAdministrator.getCanInviteUsers()));
        botPermissions.setCanPinMessages(Boolean.TRUE.equals(chatMemberAdministrator.getCanPinMessages()));
        botPermissions.setCanManageVideoChats(Boolean.TRUE.equals(chatMemberAdministrator.getCanManageVideoChats()));
    }

    public SendMessage createUserWelcomeMessage(Message message) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
        inlineKeyboardButton.setText(translations.getButtonText());
        inlineKeyboardButton.setCallbackData(message.getChatId() + "_" + message.getMessageId());
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(Collections.singletonList(inlineKeyboardButton));
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(rowList);

        return SendMessage.builder()
                .text(translations.getUserWelcomeMsg())
                .parseMode(PARSE_MODE_MARKDOWN)
                .chatId(message.getChatId())
                .replyToMessageId(message.getMessageId())
                .replyMarkup(inlineKeyboardMarkup)
                .build();
    }

    public SendAnimation createUserCongratulationsMessage(CallbackQuery callbackQuery) {
        return SendAnimation.builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .animation(new InputFile(translations.getAnimationFileId()))
                .caption(String.format(translations.getUserCongratulationsMsg(), callbackQuery.getFrom().getFirstName(), callbackQuery.getFrom().getId()))
                .parseMode(PARSE_MODE_MARKDOWN)
                .build();
    }

    public SendMessage createSettingsInfoMessage(Message message, ChatInfo chatInfo) {
        return SendMessage.builder()
                .text(String.format(
                        translations.getSettingsInfoMsg(),
                        chatInfo.getWaitingTime(),
                        chatInfo.isBanUser() ? RemoveType.BAN : RemoveType.KICK,
                        chatInfo.getBotPermissions().isCanRestrictMembers() ? translations.getProvided() : translations.getNotProvided(),
                        chatInfo.getBotPermissions().isCanDeleteMessages() ? translations.getProvided() : translations.getNotProvided()
                ))
                .parseMode(PARSE_MODE_MARKDOWN)
                .chatId(message.getChatId())
                .build();
    }

    public SendMessage createSettingsChangedMessage(Message message) {
        return SendMessage.builder()
                .text(translations.getSettingsChangedMsg())
                .parseMode(PARSE_MODE_MARKDOWN)
                .chatId(message.getChatId())
                .build();
    }

    public SendMessage createWrongFormatBotCommandMessage(Message message) {
        return SendMessage.builder()
                .text(String.format(
                        translations.getWrongFormatBotCommandMsg(),
                        BotCommand.WAITING_TIME,
                        username,
                        BotCommand.REMOVE_TYPE,
                        username
                ))
                .parseMode(PARSE_MODE_MARKDOWN)
                .chatId(message.getChatId())
                .build();
    }

    public SendMessage createWelcomeMessage(ChatMemberUpdated chatMemberUpdated) {
        return SendMessage.builder()
                .text(String.format(
                        translations.getWelcomeMsg(),
                        BotCommand.CHECK_SETTINGS,
                        username
                ))
                .parseMode(PARSE_MODE_MARKDOWN)
                .chatId(chatMemberUpdated.getChat().getId())
                .build();
    }

    public SendMessage createLostAdministratorRightsMessage(ChatMemberUpdated chatMemberUpdated) {
        return SendMessage.builder()
                .text(translations.getLostAdministratorRightsMsg())
                .parseMode(PARSE_MODE_MARKDOWN)
                .chatId(chatMemberUpdated.getChat().getId())
                .build();
    }

    public SendMessage createEnoughRightsMessage(ChatMemberUpdated chatMemberUpdated) {
        return SendMessage.builder()
                .text(translations.getEnoughRightsMsg())
                .parseMode(PARSE_MODE_MARKDOWN)
                .chatId(chatMemberUpdated.getChat().getId())
                .build();
    }

    public SendMessage createNotEnoughRightsMessage(ChatMemberUpdated chatMemberUpdated) {
        return SendMessage.builder()
                .text(translations.getNotEnoughRightsMsg())
                .parseMode(PARSE_MODE_MARKDOWN)
                .chatId(chatMemberUpdated.getChat().getId())
                .build();
    }

    public SendMessage createChatTypeNotSupportedMessage(Chat chat) {
        return SendMessage.builder()
                .text(translations.getChatTypeNotSupportedMsg())
                .parseMode(PARSE_MODE_MARKDOWN)
                .chatId(chat.getId())
                .build();
    }

    public SendMessage createUserBannedMessage(TaskInfo taskInfo) {
        return SendMessage.builder()
                .text(String.format(translations.getUserBannedMsg(), taskInfo.getFirstName(), taskInfo.getUserId()))
                .parseMode(PARSE_MODE_MARKDOWN)
                .chatId(taskInfo.getChatId())
                .build();
    }

    public boolean containsUser(List<ChatMember> chatMemberList, User user) {
        for (ChatMember chatMember : chatMemberList) {
            if (chatMember.getUser().getId().equals(user.getId())) {
                return true;
            }
        }
        return false;
    }

    public List<User> removeBots(List<User> userList) {
        if (userList == null) {
            return null;
        }
        userList.removeIf(User::getIsBot);
        return userList;
    }

    public int getUnixTime(Date date) {
        return Math.toIntExact(date.getTime() / 1000);
    }

    public String getUsername() {
        return username;
    }

    @Autowired
    public void setTranslations(Translations translations) {
        this.translations = translations;
    }

}
