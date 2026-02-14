package com.dota2analyzer.core.model.opendota;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KillLogEntry {

    @JsonProperty("time")
    private int time;

    @JsonProperty("key")
    private String key = "";

    public KillLogEntry() {}

    public int getTime() { return time; }
    public void setTime(int time) { this.time = time; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
}
