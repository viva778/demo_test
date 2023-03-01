package com.supcon.orchid.fooramework.services;


import com.supcon.orchid.ec.entities.abstracts.AbstractEcFullEntity;
import com.supcon.orchid.ec.services.DataGridService;
import com.supcon.orchid.fooramework.entities.WfDiagram;
import com.supcon.orchid.fooramework.entities.WfEdge;
import com.supcon.orchid.fooramework.entities.WfNode;
import com.supcon.orchid.fooramework.entities.WfPathsInfo;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface PlatformWorkflowService {


    /**
     * 删除待办
     * @param userId 用户ID
     * @param tableInfoId 单据ID
     * @param activityName 活动名称
     */
    void deletePending(Long userId, Long tableInfoId, String activityName);

    /**
     * 删除待办
     * @param userId 用户ID
     * @param tableInfoId 单据ID
     */
    void deletePending(Long userId, Long tableInfoId);

    /**
     * 删除待办
     * @param pendingId 待办ID
     */
    void deletePending(Long pendingId);

    /**
     * 是否首次迁移
     */
    boolean isFirstTransition(Long deploymentId,String transitionCode);

    /**
     * 查找第一条迁移线
     */
    WfEdge findFirstTransition(Long deploymentId);

    /**
     * 查找开始迁移线
     */
    WfEdge findStartTransition(Long deploymentId);

    /**
     * 获取pendingId，优先当前操作人
     * @param tableInfoId 单据id
     * @return pendingId
     */
    Long findPendingIdOperatorFirst(Long tableInfoId);


    /**
     * 并返回最顶部的活动
     */
    List<WfNode> getTopActivityNodes(Long tableInfoId, Long deploymentId);

    /**
     * 获取下一条驳回线
     */
    WfEdge getNextRejectTransition(Long tableInfoId, Long deploymentId);

    @Transactional
    void saveTable(AbstractEcFullEntity table, String processKey, DataGridService dataGridService);

    void effectBySuperEdit(AbstractEcFullEntity table, String processKey, DataGridService dataGridService);

    /**
     * 将单据提交至下一环节
     * @param table 单据
     * @param dataGridService 表体服务
     */
    void submitTableToNext(AbstractEcFullEntity table, DataGridService dataGridService);

    /**
     * 提交一个全新的单据
     * @param table 单据
     * @param dataGridService 表体服务
     */
    void submitNewTable(AbstractEcFullEntity table, String processKey, DataGridService dataGridService);

    /**
     * 获取下一条一般迁移线
     */
    WfEdge getNextTransition(Long tableInfoId, Long deploymentId);


    /**
     * 获取迁移线信息
     */
    WfEdge getEdgeInfo(Long deploymentId, String code);

    /**
     * 获取所有迁移线信息
     */
    List<WfEdge> getEdges(Long deploymentId);

    /**
     * 获取当前工作流deploymentId
     * @param processKey 工作流Key
     * @return deploymentId
     */
    Long getCurrentDeploymentId(String processKey);

    /**
     * 获取工作流deploymentId
     * @param processKey 工作流Key
     * @param version 版本号
     * @return deploymentId
     */
    Long getDeploymentId(String processKey, int version);

    /**
     * 获取工作流图像
     */
    WfDiagram getDiagram(Long deploymentId);

    WfPathsInfo getPathsInfo(Long deploymentId, String fromNode, String toNode);

    WfPathsInfo getPathsBetween(String fromNode,String toNode,WfDiagram diagram);
}
