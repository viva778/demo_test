package com.supcon.orchid.fooramework.support;

import com.supcon.orchid.fooramework.util.Jacksons;
import com.supcon.orchid.fooramework.util.Maps;
import com.supcon.orchid.workflow.engine.entities.WorkFlowVar;

import java.util.Collections;

public class WorkflowVarFactory {

    public static WorkFlowVar reject(String outcome){
        return Jacksons.convert(
                Maps.immutable(
                        "outcome",outcome,
                        "comment","",
                        "operateType","submit",
                        "outcomeMap", Collections.singleton(Maps.immutable(
                                "dec","驳回",
                                "type","reject",
                                "outcome",outcome
                        )),
                        "outcomeType","reject",
                        "activityType","task"
                ),
                WorkFlowVar.class
        );
    }

    public static WorkFlowVar normal(String outcome){
        return Jacksons.convert(
                Maps.immutable(
                        "outcome",outcome,
                        "comment","",
                        "operateType","submit",
                        "outcomeMap", Collections.singleton(Maps.immutable(
                                "dec","提交",
                                "type","normal",
                                "outcome",outcome
                        )),
                        "outcomeType","normal",
                        "activityType","task"
                ),
                WorkFlowVar.class
        );
    }
}
