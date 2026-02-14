package com.dota2analyzer.core.model.opendota;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HeroStats {

    @JsonProperty("id")
    private int id;

    @JsonProperty("pro_win")
    private int proWin;

    @JsonProperty("pro_pick")
    private int proPick;

    public HeroStats() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProWin() { return proWin; }
    public void setProWin(int proWin) { this.proWin = proWin; }

    public int getProPick() { return proPick; }
    public void setProPick(int proPick) { this.proPick = proPick; }
}
