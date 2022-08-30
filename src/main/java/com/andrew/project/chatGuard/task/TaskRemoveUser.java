package com.andrew.project.chatGuard.task;

import com.andrew.project.chatGuard.scheduler.SchedulerRemoveUser;

public class TaskRemoveUser implements Runnable {

    private Long id;
    private SchedulerRemoveUser schedulerRemoveUser;

    public TaskRemoveUser(Long id, SchedulerRemoveUser schedulerRemoveUser) {
        this.id = id;
        this.schedulerRemoveUser = schedulerRemoveUser;
    }

    @Override
    public void run() {
        schedulerRemoveUser.executeTaskRemoveUser(id);
    }

}
