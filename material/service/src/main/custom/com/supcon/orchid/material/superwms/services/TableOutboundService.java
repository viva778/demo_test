package com.supcon.orchid.material.superwms.services;

import com.supcon.orchid.ec.entities.abstracts.AbstractEcFullEntity;
import com.supcon.orchid.ec.entities.abstracts.AbstractEcPartEntity;
import com.supcon.orchid.workflow.engine.entities.WorkFlowVar;

import java.util.List;

public interface TableOutboundService {

    void standardOutboundEvent(String bizType, AbstractEcFullEntity table, WorkFlowVar workFlowVar);

    void quickEffect(String bizType, AbstractEcFullEntity table);

    void createOutboundTask(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details, String bizType);

    void solveOutbound(AbstractEcFullEntity table, List<? extends AbstractEcPartEntity> details, String bizType);

    void deleteOutboundTable(String tableNo, String bizType);

}
