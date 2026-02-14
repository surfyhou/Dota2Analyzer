package com.dota2analyzer.core.model.analysis;

public class InventoryItem {
    private String key;
    private String name;
    private String img;

    public InventoryItem() {}

    public InventoryItem(String key, String name, String img) {
        this.key = key;
        this.name = name;
        this.img = img;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImg() { return img; }
    public void setImg(String img) { this.img = img; }
}
