package com.supcon.orchid.fooramework.services.impl;


import com.supcon.orchid.ec.entities.abstracts.AbstractEcFullEntity;
import com.supcon.orchid.ec.services.DataGridService;
import com.supcon.orchid.fooramework.support.Wrapper;
import com.supcon.orchid.fooramework.util.*;
import com.supcon.orchid.fooramework.entities.WfDiagram;
import com.supcon.orchid.fooramework.entities.WfEdge;
import com.supcon.orchid.fooramework.entities.WfNode;
import com.supcon.orchid.fooramework.entities.WfPathsInfo;
import com.supcon.orchid.fooramework.services.PlatformWorkflowService;
import com.supcon.orchid.fooramework.support.WorkflowVarFactory;
import com.supcon.orchid.workflow.engine.entities.Deployment;
import com.supcon.orchid.workflow.engine.entities.Pending;
import com.supcon.orchid.workflow.engine.entities.Transition;
import com.supcon.orchid.workflow.engine.entities.WorkFlowVar;
import com.supcon.orchid.workflow.engine.services.ProcessService;
import org.jbpm.pvm.internal.model.TransitionImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;


@Service("WMS_WorkflowService")
@Transactional
public class PlatformWorkflowServiceImpl implements PlatformWorkflowService {

    @Autowired
    private ProcessService processService;

    /**
     * 删除待办
     * @param userId 用户ID
     * @param tableInfoId 单据ID
     * @param activityName 活动名称
     */
    @Transactional
    public void deletePending(Long userId, Long tableInfoId, String activityName){
        Dbs.execute(
                "DELETE FROM "+Pending.TABLE_NAME+" WHERE USER_ID=? AND TABLE_INFO_ID=? AND ACTIVITY_NAME=?",
                userId,tableInfoId,activityName
        );
    }

    /**
     * 删除待办
     * @param userId 用户ID
     * @param tableInfoId 单据ID
     */
    @Transactional
    public void deletePending(Long userId, Long tableInfoId){
        Dbs.execute(
                "DELETE FROM "+Pending.TABLE_NAME+" WHERE USER_ID=? AND TABLE_INFO_ID=?",
                userId,tableInfoId
        );
    }

    /**
     * 删除待办
     * @param pendingId 待办ID
     */
    @Transactional
    public void deletePending(Long pendingId){
        Dbs.execute(
                "DELETE FROM "+Pending.TABLE_NAME+" WHERE ID=?",
                pendingId
        );
    }

    /**
     * 是否首次迁移
     */
    public boolean isFirstTransition(Long deploymentId,String transitionCode){
        return processService.findFirstTransitions(deploymentId)
                .stream()
                .anyMatch(transition -> transitionCode.equals(transition.getName()));
    }

    /**
     * 查找第一条迁移线
     */
    public WfEdge findFirstTransition(Long deploymentId){
        return processService.findFirstTransitions(deploymentId)
                .stream()
                .map(transition -> (TransitionImpl)transition)
                .filter(transition -> transition.getReject()==0&&transition.getCancel()==0)//非导向作废
                .findAny()
                .map(transition -> new WfEdge(transition.getName(),transition.getSource().getName(),transition.getDestination().getName()))
                .orElse(null);
    }

    /**
     * 查找开始迁移线
     */
    public WfEdge findStartTransition(Long deploymentId){
        return Optional.ofNullable(processService.findStartTransitions(deploymentId))
                .map(transition->new WfEdge(transition.getName(),transition.getSource().getName(),transition.getDestination().getName()))
                .orElse(null);
    }

    /**
     * 获取pendingId，优先当前操作人
     * @param tableInfoId 单据id
     * @return pendingId
     */
    public Long findPendingIdOperatorFirst(Long tableInfoId){
        Long currentPendingId = Dbs.first(
                "SELECT ID FROM "+ Pending.TABLE_NAME+" WHERE TABLE_INFO_ID=? AND USER_ID=? ",
                Long.class,
                tableInfoId, Organazations.getCurrentUserId()
        );
        if(currentPendingId!=null){
            return currentPendingId;
        } else {
            return Dbs.first(
                    "SELECT ID FROM "+ Pending.TABLE_NAME+" WHERE TABLE_INFO_ID=? ",
                    Long.class,
                    tableInfoId
            );
        }
    }


    /**
     * 并返回最顶部的活动
     */
    public List<WfNode> getTopActivityNodes(Long tableInfoId, Long deploymentId){
        WfDiagram diagram = getDiagram(deploymentId);
        Map<String,WfNode> code$node = diagram.getNodes().stream().collect(Collectors.toMap(
                WfNode::getCode,
                node->node
        ));

        return Dbs.stream(
                "select activityname_ from jbpm4_execution where activityname_ is not null and tableinfo_id_=?",
                String.class,
                tableInfoId
        ).map(code$node::get).collect(Collectors.toList());
    }

    /**
     * 获取下一条驳回线
     */
    public WfEdge getNextRejectTransition(Long tableInfoId, Long deploymentId){
        return Optional.ofNullable(Dbs.first(
                "select code,from_node_code,to_node_code " +
                        "from "+Transition.TABLE_NAME+" " +
                        "where type=2 and deployment_id=? and from_node_code in (" +
                        "select activityname_ from jbpm4_execution where activityname_ is not null and tableinfo_id_=?" +
                        ")",
                Object[].class,
                deploymentId,tableInfoId
        )).map(res-> ArrayOperator.of(res).map_to(String.class).get()).map(res->new WfEdge(res[0],res[1],res[2])).orElse(null);
    }

    /**
     * 保存单据到编辑
     * @param table 表头
     * @param processKey 工作流KEY
     * @param dataGridService 表格服务
     */
    @Transactional
    public void saveTable(AbstractEcFullEntity table, String processKey, DataGridService dataGridService){
        Long deploymentId = getCurrentDeploymentId(processKey);
        Assert.notNull(deploymentId,"找不到已发布工作流信息！");
        WorkFlowVar workFlowVar = new WorkFlowVar();
        workFlowVar.setOperateType("save");
        Object service = Entities.getEntityService(table.getClass());
        Reflects.call(
                service,"save",
                table,deploymentId,null,workFlowVar,dataGridService
        );
    }

    @Override
    public void effectBySuperEdit(AbstractEcFullEntity table, String processKey, DataGridService dataGridService){
        table.setStatus(99);
        Long deploymentId = getCurrentDeploymentId(processKey);
        Assert.notNull(deploymentId,"找不到已发布工作流信息！");
        WorkFlowVar workFlowVar = WorkflowVarFactory.normal(processService.findEndTransitions(deploymentId).getName());
        workFlowVar.setOperateType("save");

        Object service = Entities.getEntityService(table.getClass());
        String mtdNameOfSuperEdit = "saveSuperEdit"+Entities.getEntityName(table.getClass());
        //调用superEdit
        Reflects.call(
                service,mtdNameOfSuperEdit,
                table,workFlowVar,dataGridService,null,new boolean[]{false}
        );
    }

    /**
     * 将单据提交至下一环节
     * @param table 单据
     * @param dataGridService 表体服务
     */
    @Transactional
    public void submitTableToNext(AbstractEcFullEntity table, DataGridService dataGridService){
        //提交单据
        //1).获取迁移线&创建工作流对象
        WfEdge nextEdge = getNextTransition(table.getTableInfoId(),table.getDeploymentId());
        Assert.notNull(nextEdge,"找不到迁移线信息，无法提交单据！");
        WorkFlowVar workFlowVar = WorkflowVarFactory.normal(nextEdge.getCode());
        //2).查找pending(优先当前人)
        Long pendingId = findPendingIdOperatorFirst(table.getTableInfoId());
        //3).标记自动提交后调用提交
        RequestCaches.set("__auto_submit__",true);
        Object service = Entities.getEntityService(table.getClass());
        Reflects.call(
                service,"submit",
                table,table.getDeploymentId(),pendingId,workFlowVar,dataGridService
        );
        RequestCaches.set("__auto_submit__",false);
    }

    /**
     * 提交一个全新的单据
     * @param table 单据
     * @param dataGridService 表体服务
     */
    @Transactional
    public void submitNewTable(AbstractEcFullEntity table, String processKey, DataGridService dataGridService){
        //获取工作流信息
        Long deploymentId = getCurrentDeploymentId(processKey);
        Assert.notNull(deploymentId,"找不到已发布工作流信息！");
        //提交单据
        //1).获取迁移线&创建工作流对象
        WfEdge nextEdge = findFirstTransition(deploymentId);
        Assert.notNull(nextEdge,"找不到迁移线信息，无法提交单据！");
        WorkFlowVar workFlowVar = WorkflowVarFactory.normal(nextEdge.getCode());
        //2).标记自动提交后调用提交
        //3).标记自动提交后调用提交
        RequestCaches.set("__auto_submit__",true);
        Object service = Entities.getEntityService(table.getClass());
        Reflects.call(
                service,"submit",
                table,deploymentId,null,workFlowVar,dataGridService
        );
        RequestCaches.set("__auto_submit__",false);
    }

    /**
     * 获取下一条一般迁移线
     */
    public WfEdge getNextTransition(Long tableInfoId, Long deploymentId){
        return Optional.ofNullable(Dbs.first(
                "select code,from_node_code,to_node_code " +
                        "from "+Transition.TABLE_NAME+" " +
                        "where type=1 and deployment_id=? and from_node_code in (" +
                        "select activityname_ from jbpm4_execution where activityname_ is not null and tableinfo_id_=?" +
                        ")",
                Object[].class,
                deploymentId,tableInfoId
        )).map(res-> ArrayOperator.of(res).map_to(String.class).get()).map(res->new WfEdge(res[0],res[1],res[2])).orElse(null);
    }


    /**
     * 获取迁移线信息
     */
    public WfEdge getEdgeInfo(Long deploymentId, String code){
        return Optional.ofNullable(Dbs.first(
                "select from_node_code,to_node_code from "+Transition.TABLE_NAME+" where deployment_id=? and code=? ",
                Object[].class,
                deploymentId, code
        )).map(res-> ArrayOperator.of(res).map_to(String.class).get()).map(res->new WfEdge(code,res[0],res[1])).orElse(null);
    }

    /**
     * 获取所有迁移线信息
     */
    public List<WfEdge> getEdges(Long deploymentId){
        return Dbs.stream(
                "select code,from_node_code,to_node_code from "+Transition.TABLE_NAME+" where deployment_id=? ",
                Object[].class,
                deploymentId
        ).map(res-> ArrayOperator.of(res).map_to(String.class).get()).map(res->new WfEdge(res[0],res[1],res[2])).collect(Collectors.toList());
    }

    /**
     * 获取当前工作流deploymentId
     * @param processKey 工作流Key
     * @return deploymentId
     */
    @Transactional
    public Long getCurrentDeploymentId(String processKey) {
        return Dbs.first(
                "SELECT ID FROM "+ Deployment.TABLE_NAME +" WHERE IS_CURRENT_VERSION=1 AND PROCESS_KEY=? ",
                Long.class,
                processKey
        );
    }

    /**
     * 获取工作流deploymentId
     * @param processKey 工作流Key
     * @param version 版本号
     * @return deploymentId
     */
    @Transactional
    public Long getDeploymentId(String processKey, int version) {
        return Dbs.first(
                "SELECT ID FROM "+Deployment.TABLE_NAME+" WHERE PROCESS_KEY=? AND PROCESS_VERSION=?",
                Long.class,
                processKey,version
        );
    }

    /**
     * 获取工作流图像
     */
    public WfDiagram getDiagram(Long deploymentId){
        return NativeCaches.lkComputeIfAbsent(deploymentId, k->{
            WfDiagram diagram = new WfDiagram();
            List<WfEdge> edges = getEdges(deploymentId);
            List<WfNode> nodes = new LinkedList<>();
            Map<String,WfNode> code$node = new HashMap<>();
            edges.forEach(edge->{
                //向来的节点增加出边
                WfNode fromNode = code$node.computeIfAbsent(edge.getFromNode(), code->{
                    WfNode node = new WfNode(code);
                    nodes.add(node);
                    return node;
                });
                if(fromNode.getOutSequences()==null){
                    fromNode.setOutSequences(new LinkedList<>());
                }
                List<WfEdge> outSequences = fromNode.getOutSequences();
                outSequences.add(edge);

                //向去的节点增加入边
                WfNode toNode = code$node.computeIfAbsent(edge.getToNode(), code->{
                    WfNode node = new WfNode(code);
                    nodes.add(node);
                    return node;
                });
                if(toNode.getInSequences()==null){
                    toNode.setInSequences(new LinkedList<>());
                }
                List<WfEdge> inSequences = toNode.getInSequences();
                inSequences.add(edge);

                //判断并设置起始和结束节点
                if(fromNode.getCode().startsWith("start_")){
                    diagram.setStartNode(fromNode);
                }
                if(toNode.getCode().startsWith("end_")){
                    diagram.setEndNode(toNode);
                }
            });
            diagram.setEdges(edges);
            diagram.setNodes(nodes);
            return diagram;
        });
    }

    public WfPathsInfo getPathsInfo(Long deploymentId, String fromNode, String toNode){
        return NativeCaches.computeIfAbsent(
                Strings.join("",deploymentId,fromNode,toNode),
                k->getPathsBetween(fromNode,toNode,getDiagram(deploymentId))
        );
    }

    public WfPathsInfo getPathsBetween(String fromNode,String toNode,WfDiagram diagram){
        Map<String,WfNode> code$node = diagram.getNodes().stream().collect(Collectors.toMap(
                WfNode::getCode,
                node->node
        ));
        Map<String,WfEdge> code$edge = diagram.getEdges().stream().collect(Collectors.toMap(
                WfEdge::getCode,
                edge->edge
        ));
        WfNode startNode = code$node.get(fromNode)!=null?
                code$node.get(fromNode):diagram.getStartNode();
        WfNode endNode = code$node.get(toNode)!=null?
                code$node.get(toNode):diagram.getEndNode();

        Set<String> path = new LinkedHashSet<>();
        Set<String> thisPath = new LinkedHashSet<>();
        List<Set<String>> paths = new ArrayList<>();
        Set<String> startEdges = new HashSet<>();
        Set<String> endEdges = new HashSet<>();

        Wrapper<Consumer<WfNode>> selfWrapper = new Wrapper<>();
        Consumer<WfNode> dfsFindPath = currentNode->{
            //从currentNode出发，访问下一节点
            if(currentNode.getOutSequences()!=null){
                currentNode.getOutSequences().forEach(edge->{
                    //避免重复访问
                    if(thisPath.add(edge.getCode())){
                        WfNode nextNode = code$node.get(edge.getToNode());
                        if(nextNode.getCode().equals(endNode.getCode())){
                            //到达终点dfs结束
                            paths.add(new LinkedHashSet<>(thisPath));
                            //增加起始和结束节点
                            startEdges.add(thisPath.iterator().next());
                            endEdges.add(edge.getCode());
                        } else {
                            //继续dfs
                            selfWrapper.get().accept(nextNode);
                        }
                        thisPath.remove(edge.getCode());
                    }
                });
            }
        };
        selfWrapper.set(dfsFindPath);
        dfsFindPath.accept(startNode);
        Set<String> nodes = new LinkedHashSet<>();
        //根据路径长度排序
        paths.sort(Comparator.comparingInt(Set::size));
        List<String> passedPathSigList = new LinkedList<>();
        paths.forEach(p->{
            String pathSig = String.join("",p);
            for (String passedPathSig : passedPathSigList) {
                //如果和最短路径完全重合，则排除
                if(pathSig.endsWith(passedPathSig)){
                    return;
                }
            }
            path.addAll(p);
            p.stream().map(code$edge::get).forEach(edge->{
                nodes.add(edge.getFromNode());
                nodes.add(edge.getToNode());
            });
            passedPathSigList.add(pathSig);
        });
        return new WfPathsInfo(fromNode,toNode,startEdges,endEdges,path,nodes);
    }
}
