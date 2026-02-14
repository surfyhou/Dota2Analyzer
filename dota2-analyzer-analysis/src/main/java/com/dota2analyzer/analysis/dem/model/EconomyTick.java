package com.dota2analyzer.analysis.dem.model;

public class EconomyTick {
    private int tick;
    private int netWorth;
    private int lastHits;
    private int denies;
    private int xp;

    public EconomyTick() {}

    public EconomyTick(int tick, int netWorth, int lastHits, int denies, int xp) {
        this.tick = tick;
        this.netWorth = netWorth;
        this.lastHits = lastHits;
        this.denies = denies;
        this.xp = xp;
    }

    public int getTick() { return tick; }
    public void setTick(int tick) { this.tick = tick; }

    public int getNetWorth() { return netWorth; }
    public void setNetWorth(int netWorth) { this.netWorth = netWorth; }

    public int getLastHits() { return lastHits; }
    public void setLastHits(int lastHits) { this.lastHits = lastHits; }

    public int getDenies() { return denies; }
    public void setDenies(int denies) { this.denies = denies; }

    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }
}
