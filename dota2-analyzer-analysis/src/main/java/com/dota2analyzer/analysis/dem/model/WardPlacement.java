package com.dota2analyzer.analysis.dem.model;

public class WardPlacement {
    private int tick;
    private int cellX;
    private int cellY;
    private String type; // "observer" or "sentry"
    private String player;

    public WardPlacement() {}

    public WardPlacement(int tick, int cellX, int cellY, String type, String player) {
        this.tick = tick;
        this.cellX = cellX;
        this.cellY = cellY;
        this.type = type;
        this.player = player;
    }

    public int getTick() { return tick; }
    public void setTick(int tick) { this.tick = tick; }

    public int getCellX() { return cellX; }
    public void setCellX(int cellX) { this.cellX = cellX; }

    public int getCellY() { return cellY; }
    public void setCellY(int cellY) { this.cellY = cellY; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPlayer() { return player; }
    public void setPlayer(String player) { this.player = player; }
}
