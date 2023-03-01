package com.supcon.orchid.material.superwms.entities.support;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WfPathsInfo implements Serializable {

    private String startNode;

    private String endNode;

    private Set<String> startPaths;

    private Set<String> endPaths;

    private Set<String> innerPaths;

    private Set<String> nodes;

    public boolean isEdgeFromStart(String edge){
        return startPaths.contains(edge);
    }

    public boolean isEdgeToEnd(String edge){
        return endPaths.contains(edge);
    }

    public boolean isEdgeInner(String edge){
        return innerPaths.contains(edge);
    }

    public boolean isNodeInner(String node){
        return nodes.contains(node);
    }
}
