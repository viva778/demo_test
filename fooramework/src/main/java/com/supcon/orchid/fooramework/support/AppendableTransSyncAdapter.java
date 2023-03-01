package com.supcon.orchid.fooramework.support;

import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.util.LinkedList;
import java.util.List;

public class AppendableTransSyncAdapter extends TransactionSynchronizationAdapter {
    private final LinkedList<Runnable> beforeEvents = new LinkedList<>();

    private final List<Runnable> afterEvents = new LinkedList<>();

    public void appendBeforeEvent(Runnable beforeEvent){
        beforeEvents.add(beforeEvent);
    }

    public void appendAfterEvent(Runnable beforeEvent){
        afterEvents.add(beforeEvent);
    }

    @Override
    public void beforeCommit(boolean readOnly) {
        super.beforeCommit(readOnly);
        beforeEvents.forEach(Runnable::run);
    }

    @Override
    public void afterCommit() {
        super.afterCommit();
        afterEvents.forEach(Runnable::run);
    }
}
