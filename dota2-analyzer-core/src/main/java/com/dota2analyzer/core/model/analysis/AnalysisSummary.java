package com.dota2analyzer.core.model.analysis;

public class AnalysisSummary {
    private int totalMatches;
    private int wins;
    private double winRate;
    private int parsedMatches;
    private int unparsedMatches;

    public AnalysisSummary() {}

    public AnalysisSummary(int totalMatches, int wins, double winRate, int parsedMatches, int unparsedMatches) {
        this.totalMatches = totalMatches;
        this.wins = wins;
        this.winRate = winRate;
        this.parsedMatches = parsedMatches;
        this.unparsedMatches = unparsedMatches;
    }

    public int getTotalMatches() { return totalMatches; }
    public void setTotalMatches(int totalMatches) { this.totalMatches = totalMatches; }

    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }

    public double getWinRate() { return winRate; }
    public void setWinRate(double winRate) { this.winRate = winRate; }

    public int getParsedMatches() { return parsedMatches; }
    public void setParsedMatches(int parsedMatches) { this.parsedMatches = parsedMatches; }

    public int getUnparsedMatches() { return unparsedMatches; }
    public void setUnparsedMatches(int unparsedMatches) { this.unparsedMatches = unparsedMatches; }
}
