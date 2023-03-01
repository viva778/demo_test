package com.supcon.orchid.material.superwms.entities.support;

import lombok.Data;

import java.util.List;

/**
 * 工作流图
 */
@Data
public class WfDiagram {
    private WfNode startNode;

    private WfNode endNode;

    private List<WfNode> nodes;

    private List<WfEdge> edges;

    public int getSignature(){
        int hash = 0;
        for (WfNode node : nodes) {
            hash+=node.getCode().hashCode();
        }
        for (WfEdge edge : edges) {
            hash+=edge.getCode().hashCode();
        }
        return hash;
    }
}
