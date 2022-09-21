package com.andrew.project.chatGuard.processor;

import com.andrew.project.chatGuard.api.constant.BotCommand;
import com.andrew.project.chatGuard.api.constant.ChatType;
import com.andrew.project.chatGuard.api.constant.RemoveType;
import com.andrew.project.chatGuard.api.entities.ChatInfo;
import com.andrew.project.chatGuard.api.entities.TaskInfo;
import com.andrew.project.chatGuard.api.executor.Executor;
import com.andrew.project.chatGuard.api.processor.Processor;
import com.andrew.project.chatGuard.api.service.ChatInfoService;
import com.andrew.project.chatGuard.api.service.TaskInfoService;
import com.andrew.project.chatGuard.mapper.Mapper;
import com.andrew.project.chatGuard.scheduler.SchedulerRemoveUser;
import com.andrew.project.chatGuard.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.groupadministration.LeaveChat;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RestrictChatMember;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.*;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Component
public class DefaultProcessorImpl implements Processor {

    private static final Logger LOGGER = LogManager.getLogger(DefaultProcessorImpl.class);

    private Executor executor;
    private ChatInfoService chatInfoService;
    private TaskInfoService taskInfoService;
    private SchedulerRemoveUser schedulerRemoveUser;
    private Util util;

    private static final String COMMAND_DELIMITER = " ";
    private static final String BOT_COMMAND = "bot_command";

    @Override
    public void processMessage(Message message) {
        Chat chat = message.getChat();
        if (ChatType.PRIVATE.equals(chat.getType())) {
            executor.execute(util.createChatTypeNotSupportedMessage(message.getChat()));
        } else {
            List<User> newChatMembers = util.removeBots(message.getNewChatMembers());
            ChatInfo chatInfo = chatInfoService.findById(chat.getId());
            if (!CollectionUtils.isEmpty(newChatMembers)) {
                LOGGER.info("New users have joined the chat!");
                if (chatInfo.getBotPermissions().isCanRestrictMembers() && chatInfo.getBotPermissions().isCanDeleteMessages()) {
                    Message sentMessage = executor.execute(util.createUserWelcomeMessage(message));
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.MINUTE, chatInfo.getWaitingTime());
                    for (User newChatMember : newChatMembers) {
                        RestrictChatMember restrictChatMember = RestrictChatMember.builder()
                                .chatId(chat.getId())
                                .userId(newChatMember.getId())
                                .permissions(new ChatPermissions())
                                .untilDate(util.getUnixTime(calendar.getTime()))
                                .build();
                        executor.execute(restrictChatMember);
                        schedulerRemoveUser.createTask(chatInfo, sentMessage, newChatMember);
                    }
                }
            } else if (message.getLeftChatMember() != null && !message.getLeftChatMember().getIsBot()) {
                LOGGER.info("User has left chat!");
                List<TaskInfo> taskInfoList = taskInfoService.findByChatIdAndUserId(chatInfo.getId(), message.getLeftChatMember().getId());
                if (!taskInfoList.isEmpty()) {
                    schedulerRemoveUser.cancelTasks(taskInfoList);
                    List<TaskInfo> activeTaskList = taskInfoService.findByChatIdAndMessageId(chatInfo.getId(), taskInfoList.get(0).getMessageId());
                    if (activeTaskList.isEmpty()) {
                        DeleteMessage deleteMessage = DeleteMessage.builder()
                                .chatId(chatInfo.getId())
                                .messageId(taskInfoList.get(0).getMessageId())
                                .build();
                        executor.execute(deleteMessage);
                    }
                }
            } else if (message.hasEntities()) {
                processCommand(message, chatInfo);
            }
        }
    }

    private void processCommand(Message message, ChatInfo chatInfo) {
        MessageEntity messageEntity = message.getEntities().get(0);
        if (messageEntity.getType().equals(BOT_COMMAND)) {
            GetChatAdministrators getChatAdministrators = GetChatAdministrators.builder()
                    .chatId(message.getChatId())
                    .build();
            List<ChatMember> chatAdministratorList = executor.execute(getChatAdministrators);
            if (util.containsUser(chatAdministratorList, message.getFrom())) {
                if (messageEntity.getText().equals(BotCommand.CHECK_SETTINGS + "@" + util.getUsername())) {
                    executor.execute(util.createSettingsInfoMessage(message, chatInfo));
                } else if (messageEntity.getText().equals(BotCommand.WAITING_TIME + "@" + util.getUsername())) {
                    boolean processingError = false;
                    try {
                        String[] command = message.getText().split(COMMAND_DELIMITER);
                        if (command.length == 2) {
                            int waitingTime = Integer.parseInt(command[1]);
                            if (waitingTime > 0) {
                                chatInfo.setWaitingTime(waitingTime);
                                chatInfoService.save(chatInfo);
                                executor.execute(util.createSettingsChangedMessage(message));
                            } else {
                                processingError = true;
                            }
                        } else {
                            processingError = true;
                        }
                    } catch (Exception ignore) {
                        processingError = true;
                    }
                    if (processingError) {
                        executor.execute(util.createWrongFormatBotCommandMessage(message));
                    }
                } else if (messageEntity.getText().equals(BotCommand.REMOVE_TYPE + "@" + util.getUsername())) {
                    boolean processingError = false;
                    try {
                        String[] command = message.getText().split(COMMAND_DELIMITER);
                        if (command.length == 2) {
                            if (RemoveType.KICK.equals(command[1])) {
                                chatInfo.setBanUser(false);
                            } else if (RemoveType.BAN.equals(command[1])) {
                                chatInfo.setBanUser(true);
                            } else {
                                processingError = true;
                            }
                            chatInfoService.save(chatInfo);
                            executor.execute(util.createSettingsChangedMessage(message));
                        } else {
                            processingError = true;
                        }
                    } catch (Exception ignore) {
                        processingError = true;
                    }
                    if (processingError) {
                        executor.execute(util.createWrongFormatBotCommandMessage(message));
                    }
                }
            }
        }
    }

    @Override
    public void processCallbackQuery(CallbackQuery callbackQuery) {
        ChatInfo chatInfo = chatInfoService.findById(callbackQuery.getMessage().getChatId());
        List<TaskInfo> taskInfoList = taskInfoService.findByChatIdAndUserId(chatInfo.getId(), callbackQuery.getFrom().getId());
        if (!taskInfoList.isEmpty()) {
            LOGGER.info("User has confirmed that he is not a bot!");
            schedulerRemoveUser.cancelTasks(taskInfoList);
            GetChat getChat = GetChat.builder().chatId(callbackQuery.getMessage().getChatId()).build();
            Chat chat = executor.execute(getChat);
            RestrictChatMember restrictChatMember = RestrictChatMember.builder()
                    .chatId(callbackQuery.getMessage().getChatId())
                    .userId(callbackQuery.getFrom().getId())
                    .permissions(chat.getPermissions())
                    .untilDate(util.getUnixTime(new Date()))
                    .build();
            executor.execute(restrictChatMember);
            if (taskInfoList.size() == 1) {
                DeleteMessage deleteMessage = DeleteMessage.builder()
                        .chatId(callbackQuery.getMessage().getChatId())
                        .messageId(callbackQuery.getMessage().getMessageId())
                        .build();
                executor.execute(deleteMessage);
            }
            executor.execute(util.createUserCongratulationsMessage(callbackQuery));
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
        } else if (ChatType.GROUP.equals(chat.getType())) {
            if (newChatMember instanceof ChatMemberLeft) {
                LOGGER.info("Bot has been removed from group!");
            } else if (newChatMember instanceof ChatMemberMember) {
                LOGGER.info("Bot has been added to group!");
                executor.execute(util.createChatTypeNotSupportedMessage(chat));
                LeaveChat leaveChat = LeaveChat.builder().chatId(chat.getId()).build();
                executor.execute(leaveChat);
                LOGGER.info("Exit from the group is executed!");
            }
        } else if (ChatType.SUPERGROUP.equals(chat.getType())) {
            if (newChatMember instanceof ChatMemberMember) {
                ChatInfo chatInfo = chatInfoService.findById(chat.getId());
                if (chatInfo == null) {
                    LOGGER.info("Bot has been added to supergroup as member!");
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
                LOGGER.info("Bot has been removed from supergroup!");
                chatInfoService.deleteChatInfoById(chat.getId());
                removeAllTasksForChat(chat.getId());
            } else if (newChatMember instanceof ChatMemberBanned) {
                LOGGER.info("Bot has been banned from supergroup!");
                chatInfoService.deleteChatInfoById(chat.getId());
                removeAllTasksForChat(chat.getId());
            } else if (newChatMember instanceof ChatMemberAdministrator) {
                LOGGER.info("Bot has been updated with administrator rights!");
                ChatInfo chatInfo = chatInfoService.findById(chat.getId());
                ChatMemberAdministrator chatMemberAdministrator = (ChatMemberAdministrator) newChatMember;
                util.updateBotPermissionList(chatInfo, chatMemberAdministrator);
                chatInfoService.save(chatInfo);
                if (chatInfo.getBotPermissions().isCanRestrictMembers() && chatInfo.getBotPermissions().isCanDeleteMessages()) {
                    executor.execute(util.createEnoughRightsMessage(chatMemberUpdated));
                } else {
                    executor.execute(util.createNotEnoughRightsMessage(chatMemberUpdated));
                }
            } else {
                LOGGER.error("Unexpected ChatMember type! " + chatMemberUpdated);
            }
        }
    }

    private void removeAllTasksForChat(Long chatId) {
        List<TaskInfo> taskInfoList = taskInfoService.findByChatId(chatId);
        if (!taskInfoList.isEmpty()) {
            schedulerRemoveUser.cancelTasks(taskInfoList);
            taskInfoService.deleteTaskInfoByChatId(chatId);
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
    public void setTaskInfoRemoveUserService(TaskInfoService taskInfoService) {
        this.taskInfoService = taskInfoService;
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
