package com.dota2analyzer.analysis.dem.model;

public class AbilityUsage {
    private int tick;
    private String abilityName;
    private String targetName;

    public AbilityUsage() {}

    public AbilityUsage(int tick, String abilityName, String targetName) {
        this.tick = tick;
        this.abilityName = abilityName;
        this.targetName = targetName;
    }

    public int getTick() { return tick; }
    public void setTick(int tick) { this.tick = tick; }

    public String getAbilityName() { return abilityName; }
    public void setAbilityName(String abilityName) { this.abilityName = abilityName; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
}
