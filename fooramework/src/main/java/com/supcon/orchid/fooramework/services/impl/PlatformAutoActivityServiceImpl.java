package com.supcon.orchid.fooramework.services.impl;

import com.supcon.orchid.fooramework.annotation.signal.SignalManager;
import com.supcon.orchid.fooramework.support.Pair;
import com.supcon.orchid.fooramework.util.RequestCaches;
import com.supcon.orchid.fooramework.util.Transactions;
import com.supcon.orchid.fooramework.services.PlatformAutoActivityService;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.Map;

import static org.springframework.data.util.CastUtils.cast;

/**
 * 由于自由活动的报错问题、回滚问题（会在afterSubmit前触发
 * 所以只在自由活动处做标记，在afterSubmit后，触发之前标记的活动代码
 */
@Service
public class PlatformAutoActivityServiceImpl implements PlatformAutoActivityService {

    /**
     * 增加活动
     * 后以信号形式触发
     * @param signal 信号
     * @param params 参数
     */
    public void addActivity(String signal,Object... params){
        getActList().push(Pair.of(signal,params));
    }

    /**
     * 增加活动
     * 后以信号形式触发
     * @param signal 信号
     * @param params 参数
     */
    public void addActivity(String signal, Map<String,Object> params) {
        getActList().push(Pair.of(signal,params));
    }


    private <X> LinkedList<X> getActList(){
        //在事务提交前，进行触发
        return RequestCaches.computeIfAbsent("__act_list__",k->{
            Transactions.appendEventBeforeCommit(this::activateAll);
            return new LinkedList<>();
        });
    }

    /**
     * 触发标记过的活动
     */
    private void activateAll(){
        LinkedList<Pair<String,Object>> arrList = RequestCaches.get("__act_list__");
        if(arrList!=null){
            while (!arrList.isEmpty()){
                Pair<String,Object> pair = arrList.pollFirst();
                if(pair.getSecond() instanceof Map) {
                    Map<String,Object> params = cast(pair.getSecond());
                    SignalManager.propagate(pair.getFirst(),params);
                } else {
                    Object[] params = cast(pair.getSecond());
                    SignalManager.propagate(pair.getFirst(),params);
                }
            }
        }
    }

}
