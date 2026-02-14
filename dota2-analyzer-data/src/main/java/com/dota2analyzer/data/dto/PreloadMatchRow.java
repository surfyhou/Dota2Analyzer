package com.dota2analyzer.data.dto;

import java.util.ArrayList;
import java.util.List;

public class PreloadMatchRow {
    private long matchId;
    private boolean radiantWin;
    private int duration;
    private int startTime;
    private List<Integer> radiantHeroes = new ArrayList<>();
    private List<Integer> direHeroes = new ArrayList<>();

    public PreloadMatchRow() {}

    public long getMatchId() { return matchId; }
    public void setMatchId(long matchId) { this.matchId = matchId; }

    public boolean isRadiantWin() { return radiantWin; }
    public void setRadiantWin(boolean radiantWin) { this.radiantWin = radiantWin; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public int getStartTime() { return startTime; }
    public void setStartTime(int startTime) { this.startTime = startTime; }

    public List<Integer> getRadiantHeroes() { return radiantHeroes; }
    public void setRadiantHeroes(List<Integer> radiantHeroes) { this.radiantHeroes = radiantHeroes; }

    public List<Integer> getDireHeroes() { return direHeroes; }
    public void setDireHeroes(List<Integer> direHeroes) { this.direHeroes = direHeroes; }
}
