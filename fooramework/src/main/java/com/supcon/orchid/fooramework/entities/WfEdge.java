package com.supcon.orchid.fooramework.entities;

import lombok.Data;

/**
 * 工作流迁移线
 */
@Data
public class WfEdge {
    private String code;

    private String fromNode;

    private String toNode;

    public WfEdge(String code, String fromNode, String toNode) {
        this.code = code;
        this.fromNode = fromNode;
        this.toNode = toNode;
    }
}
