package com.andrew.project.chatGuard.api.executor;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendAnimation;

import java.io.Serializable;

public interface Executor {

    <T extends Serializable, Method extends BotApiMethod<T>> T execute(Method method);

    void execute(SendAnimation sendAnimation);

}
