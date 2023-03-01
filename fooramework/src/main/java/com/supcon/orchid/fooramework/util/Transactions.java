package com.supcon.orchid.fooramework.util;

import com.supcon.orchid.fooramework.support.AppendableTransSyncAdapter;
import com.supcon.orchid.fooramework.support.Wrapper;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

public class Transactions {

    /**
     * 设置事务提交前的事件
     * @param event RUNNABLE对象
     */
    public static void appendEventBeforeCommit(Runnable event){
        AppendableTransSyncAdapter syncAdapter = getAdapter();
        syncAdapter.appendBeforeEvent(event);
    }

    /**
     * 设置事务提交后事件
     * @param event RUNNABLE对象
     */
    public static void appendEventAfterCommit(Runnable event){
        AppendableTransSyncAdapter syncAdapter = getAdapter();
        syncAdapter.appendAfterEvent(event);
    }

    private static AppendableTransSyncAdapter getAdapter(){
        //返回当前事务的Adapter，用于追加事件
        return TransactionCaches.computeIfAbsent("__sync_adaptor_", k->{
            //如果当前事务没有注册，则先进行注册（对于每个事务，只进行一次注册
            AppendableTransSyncAdapter adapter = new AppendableTransSyncAdapter();
            TransactionSynchronizationManager.registerSynchronization(adapter);
            return adapter;
        });
    }


    private static PlatformTransactionManager transactionManager;

    private static PlatformTransactionManager getTransactionManager(){
        return transactionManager!=null?transactionManager:(transactionManager= Springs.getBean(PlatformTransactionManager.class));
    }

    private static final Logger log = LoggerFactory.getLogger(Transactions.class);

    @SneakyThrows
    public static void run(Runnable script){
        Wrapper<Exception> eWrapper = new Wrapper<>();
        try {
            new TransactionTemplate(getTransactionManager()).execute((TransactionCallback<?>) transactionStatus -> {
                try {
                    script.run();
                } catch (Exception e) {
                    eWrapper.set(e);
                }
                return null;
            });
        } catch (Exception e){
            if(eWrapper.get()==null){
                eWrapper.set(e);
            } else {
                log.error("事务执行失败！",e);
            }
        }
        if(eWrapper.get()!=null){
            throw eWrapper.get();
        }
    }
}
