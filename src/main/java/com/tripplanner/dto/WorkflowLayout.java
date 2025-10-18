package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Workflow layout settings for storing viewport and layout information.
 * Contains zoom, pan, and other layout-related settings for the workflow view.
 */
public class WorkflowLayout {
    
    @JsonProperty("zoom")
    private Double zoom = 1.0;
    
    @JsonProperty("panX")
    private Double panX = 0.0;
    
    @JsonProperty("panY")
    private Double panY = 0.0;
    
    @JsonProperty("direction")
    private String direction = "TB"; // "TB" (top-bottom), "LR" (left-right), "BT", "RL"
    
    @JsonProperty("nodeSpacing")
    private Integer nodeSpacing = 100;
    
    @JsonProperty("rankSpacing")
    private Integer rankSpacing = 150;
    
    @JsonProperty("autoLayout")
    private Boolean autoLayout = true;
    
    @JsonProperty("fitView")
    private Boolean fitView = true;
    
    @JsonProperty("minZoom")
    private Double minZoom = 0.1;
    
    @JsonProperty("maxZoom")
    private Double maxZoom = 4.0;
    
    public WorkflowLayout() {}
    
    public WorkflowLayout(Double zoom, Double panX, Double panY) {
        this.zoom = zoom;
        this.panX = panX;
        this.panY = panY;
    }
    
    // Getters and Setters
    public Double getZoom() {
        return zoom;
    }
    
    public void setZoom(Double zoom) {
        this.zoom = zoom;
    }
    
    public Double getPanX() {
        return panX;
    }
    
    public void setPanX(Double panX) {
        this.panX = panX;
    }
    
    public Double getPanY() {
        return panY;
    }
    
    public void setPanY(Double panY) {
        this.panY = panY;
    }
    
    public String getDirection() {
        return direction;
    }
    
    public void setDirection(String direction) {
        this.direction = direction;
    }
    
    public Integer getNodeSpacing() {
        return nodeSpacing;
    }
    
    public void setNodeSpacing(Integer nodeSpacing) {
        this.nodeSpacing = nodeSpacing;
    }
    
    public Integer getRankSpacing() {
        return rankSpacing;
    }
    
    public void setRankSpacing(Integer rankSpacing) {
        this.rankSpacing = rankSpacing;
    }
    
    public Boolean getAutoLayout() {
        return autoLayout;
    }
    
    public void setAutoLayout(Boolean autoLayout) {
        this.autoLayout = autoLayout;
    }
    
    public Boolean getFitView() {
        return fitView;
    }
    
    public void setFitView(Boolean fitView) {
        this.fitView = fitView;
    }
    
    public Double getMinZoom() {
        return minZoom;
    }
    
    public void setMinZoom(Double minZoom) {
        this.minZoom = minZoom;
    }
    
    public Double getMaxZoom() {
        return maxZoom;
    }
    
    public void setMaxZoom(Double maxZoom) {
        this.maxZoom = maxZoom;
    }
    
    @Override
    public String toString() {
        return "WorkflowLayout{" +
                "zoom=" + zoom +
                ", panX=" + panX +
                ", panY=" + panY +
                ", direction='" + direction + '\'' +
                ", nodeSpacing=" + nodeSpacing +
                ", rankSpacing=" + rankSpacing +
                ", autoLayout=" + autoLayout +
                ", fitView=" + fitView +
                ", minZoom=" + minZoom +
                ", maxZoom=" + maxZoom +
                '}';
    }
}