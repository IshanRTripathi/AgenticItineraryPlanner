package com.tripplanner.dto.tools;

import com.tripplanner.dto.NormalizedNode;
import java.util.ArrayList;
import java.util.List;

public class GetNodesByTypeResult {
    private boolean success;
    private List<NormalizedNode> nodes;
    private int totalCount;
    private String error;
    
    public GetNodesByTypeResult() {
        this.nodes = new ArrayList<>();
    }
    
    public static GetNodesByTypeResult error(String error) {
        GetNodesByTypeResult result = new GetNodesByTypeResult();
        result.success = false;
        result.error = error;
        return result;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public List<NormalizedNode> getNodes() {
        return nodes;
    }
    
    public void setNodes(List<NormalizedNode> nodes) {
        this.nodes = nodes;
    }
    
    public int getTotalCount() {
        return totalCount;
    }
    
    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
}
