package com.tripplanner.dto;

import com.tripplanner.service.ChangeEngine;

/**
 * Wrapper for EditorAgent results that includes additional metadata like cost impact.
 * This allows us to pass extra information from EditorAgent to OrchestratorService.
 */
public class EditorAgentResult {
    
    private ChangeEngine.ApplyResult applyResult;
    private CostImpact costImpact;
    
    public EditorAgentResult() {}
    
    public EditorAgentResult(ChangeEngine.ApplyResult applyResult) {
        this.applyResult = applyResult;
    }
    
    public EditorAgentResult(ChangeEngine.ApplyResult applyResult, CostImpact costImpact) {
        this.applyResult = applyResult;
        this.costImpact = costImpact;
    }
    
    // Getters and Setters
    public ChangeEngine.ApplyResult getApplyResult() {
        return applyResult;
    }
    
    public void setApplyResult(ChangeEngine.ApplyResult applyResult) {
        this.applyResult = applyResult;
    }
    
    public CostImpact getCostImpact() {
        return costImpact;
    }
    
    public void setCostImpact(CostImpact costImpact) {
        this.costImpact = costImpact;
    }
    
    @Override
    public String toString() {
        return "EditorAgentResult{" +
                "applyResult=" + applyResult +
                ", costImpact=" + costImpact +
                '}';
    }
}
