package org.opennms.plugins.zabbix.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GraphItem {
    @JsonProperty("drawtype")
    private String drawType;
    @JsonProperty("sortorder")
    private String sortOrder;
    @JsonProperty("yaxisside")
    private String yAxisSide;
    private String color;

    @JsonProperty("item")
    private GraphItemItem item;

    public String getDrawType() {
        return drawType;
    }

    public void setDrawType(String drawType) {
        this.drawType = drawType;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getyAxisSide() {
        return yAxisSide;
    }

    public void setyAxisSide(String yAxisSide) {
        this.yAxisSide = yAxisSide;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public GraphItemItem getItem() {
        return item;
    }

    public void setItem(GraphItemItem item) {
        this.item = item;
    }
}
