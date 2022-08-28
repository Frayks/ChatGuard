package com.andrew.project.chatGuard.processor;

import com.andrew.project.chatGuard.api.constant.BotCommand;
import com.andrew.project.chatGuard.api.constant.ChatType;
import com.andrew.project.chatGuard.api.constant.RemoveType;
import com.andrew.project.chatGuard.api.entities.ChatInfo;
import com.andrew.project.chatGuard.api.executor.Executor;
import com.andrew.project.chatGuard.api.processor.Processor;
import com.andrew.project.chatGuard.api.service.ChatInfoService;
import com.andrew.project.chatGuard.mapper.Mapper;
import com.andrew.project.chatGuard.scheduler.SchedulerRemoveUser;
import com.andrew.project.chatGuard.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.groupadministration.LeaveChat;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.*;

import java.util.List;

@Component
public class DefaultProcessorImpl implements Processor {

    private static final Logger LOGGER = LogManager.getLogger(DefaultProcessorImpl.class);

    private Executor executor;
    private ChatInfoService chatInfoService;
    private SchedulerRemoveUser schedulerRemoveUser;
    private Util util;

    private static final String BOT_COMMAND = "bot_command";

    @Override
    public void processMessage(Message message) {
        Chat chat = message.getChat();
        if (ChatType.PRIVATE.equals(chat.getType())) {
            executor.execute(util.createPrivateChatMessage(message));
        } else {
            List<User> newChatMembers = util.removeThisBotUser(message.getNewChatMembers());
            ChatInfo chatInfo = chatInfoService.findById(chat.getId());
            if (!CollectionUtils.isEmpty(newChatMembers)) {
                LOGGER.info("New users have joined the chat!");
                if (chatInfo != null) {
                    if (chatInfo.getBotPermissions().isCanRestrictMembers()) {
                        executor.execute(util.createUserWelcomeMessage(message));
                        for (User newChatMember : newChatMembers) {
                            schedulerRemoveUser.createTask(chatInfo, newChatMember);
                        }
                    }
                } else {
                    LOGGER.error("ChatInfo is empty although it shouldn't be! " + message);
                }
            } else if (message.getLeftChatMember() != null && !util.isThisBot(message.getLeftChatMember())) {
                LOGGER.info("User has left chat!");
                if (chatInfo != null) {
                    String key = schedulerRemoveUser.createKey(chatInfo.getId(), message.getLeftChatMember().getId());
                    if (schedulerRemoveUser.hasTask(key)) {
                        schedulerRemoveUser.cancelTask(key);
                    }
                } else {
                    LOGGER.error("ChatInfo is empty although it shouldn't be! " + message);
                }
            } else if (message.hasEntities()) {
                if (chatInfo != null) {
                    processCommand(message, chatInfo);
                } else {
                    LOGGER.error("ChatInfo is empty although it shouldn't be! " + message);
                }
            } else if (message.getMigrateToChatId() != null) {
                LOGGER.info("Chat has been moved! oldChatId: " + message.getChatId() + ", newChatId: " + message.getMigrateToChatId());
                chatInfoService.deleteChatInfoById(message.getChatId());
            }
        }
    }

    private void processCommand(Message message, ChatInfo chatInfo) {
        MessageEntity messageEntity = message.getEntities().get(0);
        if (messageEntity.getType().equals(BOT_COMMAND)) {
            if (messageEntity.getText().equals(BotCommand.CHECK_SETTINGS + "@" + util.getUsername())) {
                executor.execute(util.createSettingsInfoMessage(message, chatInfo));
            } else {
                GetChatAdministrators getChatAdministrators = GetChatAdministrators.builder()
                        .chatId(message.getChatId())
                        .build();
                List<ChatMember> chatAdministratorList = executor.execute(getChatAdministrators);
                if (util.containsUser(chatAdministratorList, message.getFrom())) {
                    if (messageEntity.getText().equals(BotCommand.WAITING_TIME + "@" + util.getUsername())) {
                        try {
                            String[] command = message.getText().split(" ");
                            if (command.length == 2) {
                                long waitingTime = Long.parseLong(command[1]);
                                chatInfo.setWaitingTime(waitingTime);
                                chatInfoService.save(chatInfo);
                                executor.execute(util.createSettingsChangedMessage(message));
                            } else {
                                throw new Exception();
                            }
                        } catch (Exception ignore) {
                            executor.execute(util.createWrongFormatBotCommandMessage(message));
                        }
                    } else if (messageEntity.getText().equals(BotCommand.REMOVE_TYPE + "@" + util.getUsername())) {
                        String[] command = message.getText().split(" ");
                        try {
                            if (command.length == 2) {
                                if (RemoveType.KICK.equals(command[1])) {
                                    chatInfo.setBanUser(false);
                                } else if (RemoveType.BAN.equals(command[1])) {
                                    chatInfo.setBanUser(true);
                                } else {
                                    throw new Exception();
                                }
                                chatInfoService.save(chatInfo);
                                executor.execute(util.createSettingsChangedMessage(message));
                            } else {
                                throw new Exception();
                            }
                        } catch (Exception ignore) {
                            executor.execute(util.createWrongFormatBotCommandMessage(message));
                        }
                    }
                } else {
                    executor.execute(util.createUserNeedsRightsMessage(message));
                }
            }
        }
    }

    @Override
    public void processCallbackQuery(CallbackQuery callbackQuery) {
        ChatInfo chatInfo = chatInfoService.findById(callbackQuery.getMessage().getChatId());
        if (chatInfo != null) {
            String key = schedulerRemoveUser.createKey(chatInfo.getId(), callbackQuery.getFrom().getId());
            if (schedulerRemoveUser.hasTask(key)) {
                schedulerRemoveUser.cancelTask(key);
                LOGGER.info("User has confirmed that he is not a bot!");
                executor.execute(util.createUserCongratulationsMessage(callbackQuery));
            }
        }
    }

    @Override
    public void processMyChatMember(ChatMemberUpdated chatMemberUpdated) {
        Chat chat = chatMemberUpdated.getChat();
        ChatMember newChatMember = chatMemberUpdated.getNewChatMember();
        if (ChatType.CHANNEL.equals(chat.getType())) {
            if (newChatMember instanceof ChatMemberLeft) {
                LOGGER.info("Bot has been removed from channel!");
            } else if (newChatMember instanceof ChatMemberAdministrator) {
                LOGGER.info("Bot has been added to channel!");
                LeaveChat leaveChat = LeaveChat.builder().chatId(chat.getId()).build();
                executor.execute(leaveChat);
                LOGGER.info("Exit from the channel is executed!");
            }
        } else {
            if (newChatMember instanceof ChatMemberMember) {
                ChatInfo chatInfo = chatInfoService.findById(chat.getId());
                if (chatInfo == null) {
                    LOGGER.info("Bot has been added to chat as member!");
                    chatInfo = Mapper.mapToChatInfo(chat);
                    chatInfoService.save(chatInfo);
                    executor.execute(util.createWelcomeMessage(chatMemberUpdated));
                } else {
                    LOGGER.info("Bot has lost administrator rights!");
                    chatInfo.getBotPermissions().disableAll();
                    chatInfoService.save(chatInfo);
                    executor.execute(util.createLostAdministratorRightsMessage(chatMemberUpdated));
                }
            } else if (newChatMember instanceof ChatMemberLeft) {
                LOGGER.info("Bot has been removed from chat!");
                chatInfoService.deleteChatInfoById(chat.getId());
            } else if (newChatMember instanceof ChatMemberBanned) {
                LOGGER.info("Bot has been banned from chat!");
                chatInfoService.deleteChatInfoById(chat.getId());
            } else if (newChatMember instanceof ChatMemberAdministrator) {
                LOGGER.info("Bot has been updated with administrator rights!");
                ChatInfo chatInfo = chatInfoService.findById(chat.getId());
                if (chatInfo != null) {
                    ChatMemberAdministrator chatMemberAdministrator = (ChatMemberAdministrator) newChatMember;
                    util.updateBotPermissionList(chatInfo, chatMemberAdministrator);
                    chatInfoService.save(chatInfo);
                    if (chatInfo.getBotPermissions().isCanRestrictMembers()) {
                        executor.execute(util.createEnoughRightsMessage(chatMemberUpdated));
                    } else {
                        executor.execute(util.createNotEnoughRightsMessage(chatMemberUpdated));
                    }
                } else {
                    LOGGER.error("ChatInfo is empty although it shouldn't be! " + chatMemberUpdated);
                }
            } else {
                LOGGER.error("Unexpected ChatMember type! " + chatMemberUpdated);
            }
        }
    }

    @Autowired
    @Lazy
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    @Autowired
    public void setChatInfoService(ChatInfoService chatInfoService) {
        this.chatInfoService = chatInfoService;
    }

    @Autowired
    @Lazy
    public void setSchedulerRemoveUser(SchedulerRemoveUser schedulerRemoveUser) {
        this.schedulerRemoveUser = schedulerRemoveUser;
    }

    @Autowired
    public void setUtil(Util util) {
        this.util = util;
    }
}
