package com.dota2analyzer.core.model.opendota;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PickBan {

    @JsonProperty("is_pick")
    private boolean isPick;

    @JsonProperty("hero_id")
    private int heroId;

    @JsonProperty("team")
    private int team;

    @JsonProperty("order")
    private int order;

    public PickBan() {}

    public boolean isPick() { return isPick; }
    public void setPick(boolean pick) { isPick = pick; }

    public int getHeroId() { return heroId; }
    public void setHeroId(int heroId) { this.heroId = heroId; }

    public int getTeam() { return team; }
    public void setTeam(int team) { this.team = team; }

    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }
}
