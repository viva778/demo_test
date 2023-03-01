package com.supcon.orchid.material.superwms.services;

import com.supcon.orchid.ec.entities.abstracts.AbstractEcFullEntity;
import com.supcon.orchid.ec.entities.abstracts.AbstractEcPartEntity;
import com.supcon.orchid.workflow.engine.entities.WorkFlowVar;

import java.util.List;
import java.util.Map;

public interface TableInboundService {

    void standardInboundEvent(String bizType, AbstractEcFullEntity table, WorkFlowVar workFlowVar);

    void quickEffect(String bizType, AbstractEcFullEntity table);

    /**
     * 判断物品是否能被放入仓库
     * @param warehouseId 仓库id
     * @param goodId 物品id
     * @return 是否能被放入仓库
     */
    boolean allowToStore(Long warehouseId, Long goodId);

    void solveInbound(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details, String bizType);

    void createInboundTask(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details, String bizType);

    void deleteInboundTable(String tableNo, String bizType);

}
