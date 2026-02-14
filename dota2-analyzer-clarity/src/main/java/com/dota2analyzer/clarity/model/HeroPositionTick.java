package com.dota2analyzer.clarity.model;

public class HeroPositionTick {
    private int tick;
    private int cellX;
    private int cellY;

    public HeroPositionTick() {}

    public HeroPositionTick(int tick, int cellX, int cellY) {
        this.tick = tick;
        this.cellX = cellX;
        this.cellY = cellY;
    }

    public int getTick() { return tick; }
    public void setTick(int tick) { this.tick = tick; }

    public int getCellX() { return cellX; }
    public void setCellX(int cellX) { this.cellX = cellX; }

    public int getCellY() { return cellY; }
    public void setCellY(int cellY) { this.cellY = cellY; }
}
