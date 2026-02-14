package com.dota2analyzer.core.model.opendota;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Hero {

    @JsonProperty("id")
    private int id;

    @JsonProperty("name")
    private String name = "";

    @JsonProperty("localized_name")
    private String localizedName = "";

    public Hero() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocalizedName() { return localizedName; }
    public void setLocalizedName(String localizedName) { this.localizedName = localizedName; }
}
