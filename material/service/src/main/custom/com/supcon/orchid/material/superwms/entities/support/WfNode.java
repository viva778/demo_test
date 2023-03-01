package com.supcon.orchid.material.superwms.entities.support;

import lombok.Data;

import java.util.List;

/**
 * 工作流节点
 */
@Data
public class WfNode {

    public WfNode(String code) {
        this.code = code;
    }

    private String code;

    private List<WfEdge> outSequences;

    private List<WfEdge> inSequences;
}
