package com.dota2analyzer.core.model.analysis;

import java.util.List;

public class InventorySnapshot {
    private int time;
    private List<InventoryItem> items;

    public InventorySnapshot() {}

    public InventorySnapshot(int time, List<InventoryItem> items) {
        this.time = time;
        this.items = items;
    }

    public int getTime() { return time; }
    public void setTime(int time) { this.time = time; }

    public List<InventoryItem> getItems() { return items; }
    public void setItems(List<InventoryItem> items) { this.items = items; }
}
