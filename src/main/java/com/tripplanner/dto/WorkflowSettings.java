package com.tripplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Workflow settings for storing user preferences and view configuration.
 * Contains settings that control workflow behavior and appearance.
 */
public class WorkflowSettings {
    
    @JsonProperty("showGrid")
    private Boolean showGrid = true;
    
    @JsonProperty("snapToGrid")
    private Boolean snapToGrid = false;
    
    @JsonProperty("gridSize")
    private Integer gridSize = 20;
    
    @JsonProperty("showMiniMap")
    private Boolean showMiniMap = true;
    
    @JsonProperty("showControls")
    private Boolean showControls = true;
    
    @JsonProperty("nodesDraggable")
    private Boolean nodesDraggable = true;
    
    @JsonProperty("nodesConnectable")
    private Boolean nodesConnectable = false;
    
    @JsonProperty("elementsSelectable")
    private Boolean elementsSelectable = true;
    
    @JsonProperty("multiSelectionKeyCode")
    private String multiSelectionKeyCode = "Meta";
    
    @JsonProperty("deleteKeyCode")
    private String deleteKeyCode = "Backspace";
    
    @JsonProperty("theme")
    private String theme = "light"; // "light", "dark"
    
    @JsonProperty("customStyles")
    private Map<String, Object> customStyles;
    
    public WorkflowSettings() {}
    
    // Getters and Setters
    public Boolean getShowGrid() {
        return showGrid;
    }
    
    public void setShowGrid(Boolean showGrid) {
        this.showGrid = showGrid;
    }
    
    public Boolean getSnapToGrid() {
        return snapToGrid;
    }
    
    public void setSnapToGrid(Boolean snapToGrid) {
        this.snapToGrid = snapToGrid;
    }
    
    public Integer getGridSize() {
        return gridSize;
    }
    
    public void setGridSize(Integer gridSize) {
        this.gridSize = gridSize;
    }
    
    public Boolean getShowMiniMap() {
        return showMiniMap;
    }
    
    public void setShowMiniMap(Boolean showMiniMap) {
        this.showMiniMap = showMiniMap;
    }
    
    public Boolean getShowControls() {
        return showControls;
    }
    
    public void setShowControls(Boolean showControls) {
        this.showControls = showControls;
    }
    
    public Boolean getNodesDraggable() {
        return nodesDraggable;
    }
    
    public void setNodesDraggable(Boolean nodesDraggable) {
        this.nodesDraggable = nodesDraggable;
    }
    
    public Boolean getNodesConnectable() {
        return nodesConnectable;
    }
    
    public void setNodesConnectable(Boolean nodesConnectable) {
        this.nodesConnectable = nodesConnectable;
    }
    
    public Boolean getElementsSelectable() {
        return elementsSelectable;
    }
    
    public void setElementsSelectable(Boolean elementsSelectable) {
        this.elementsSelectable = elementsSelectable;
    }
    
    public String getMultiSelectionKeyCode() {
        return multiSelectionKeyCode;
    }
    
    public void setMultiSelectionKeyCode(String multiSelectionKeyCode) {
        this.multiSelectionKeyCode = multiSelectionKeyCode;
    }
    
    public String getDeleteKeyCode() {
        return deleteKeyCode;
    }
    
    public void setDeleteKeyCode(String deleteKeyCode) {
        this.deleteKeyCode = deleteKeyCode;
    }
    
    public String getTheme() {
        return theme;
    }
    
    public void setTheme(String theme) {
        this.theme = theme;
    }
    
    public Map<String, Object> getCustomStyles() {
        return customStyles;
    }
    
    public void setCustomStyles(Map<String, Object> customStyles) {
        this.customStyles = customStyles;
    }
    
    @Override
    public String toString() {
        return "WorkflowSettings{" +
                "showGrid=" + showGrid +
                ", snapToGrid=" + snapToGrid +
                ", gridSize=" + gridSize +
                ", showMiniMap=" + showMiniMap +
                ", showControls=" + showControls +
                ", nodesDraggable=" + nodesDraggable +
                ", nodesConnectable=" + nodesConnectable +
                ", elementsSelectable=" + elementsSelectable +
                ", multiSelectionKeyCode='" + multiSelectionKeyCode + '\'' +
                ", deleteKeyCode='" + deleteKeyCode + '\'' +
                ", theme='" + theme + '\'' +
                ", customStyles=" + customStyles +
                '}';
    }
}