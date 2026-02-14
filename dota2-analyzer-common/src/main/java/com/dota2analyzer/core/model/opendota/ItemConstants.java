package com.dota2analyzer.core.model.opendota;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ItemConstants {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("img")
    private String img = "";

    @JsonProperty("dname")
    private String displayName = "";

    @JsonProperty("cost")
    private Integer cost;

    @JsonProperty("qual")
    private String quality = "";

    @JsonProperty("components")
    private List<String> components;

    public ItemConstants() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getImg() { return img; }
    public void setImg(String img) { this.img = img; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Integer getCost() { return cost; }
    public void setCost(Integer cost) { this.cost = cost; }

    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }

    public List<String> getComponents() { return components; }
    public void setComponents(List<String> components) { this.components = components; }
}
