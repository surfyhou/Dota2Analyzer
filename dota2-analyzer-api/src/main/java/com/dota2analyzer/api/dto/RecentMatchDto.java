package com.dota2analyzer.api.dto;

public class RecentMatchDto {
    private long matchId;
    private String heroName;
    private String resultText;
    private boolean won;
    private String kda;
    private String gpmXpm;
    private int durationMinutes;

    public RecentMatchDto() {}

    public RecentMatchDto(long matchId, String heroName, String resultText, boolean won,
                          String kda, String gpmXpm, int durationMinutes) {
        this.matchId = matchId;
        this.heroName = heroName;
        this.resultText = resultText;
        this.won = won;
        this.kda = kda;
        this.gpmXpm = gpmXpm;
        this.durationMinutes = durationMinutes;
    }

    public long getMatchId() { return matchId; }
    public void setMatchId(long matchId) { this.matchId = matchId; }

    public String getHeroName() { return heroName; }
    public void setHeroName(String heroName) { this.heroName = heroName; }

    public String getResultText() { return resultText; }
    public void setResultText(String resultText) { this.resultText = resultText; }

    public boolean isWon() { return won; }
    public void setWon(boolean won) { this.won = won; }

    public String getKda() { return kda; }
    public void setKda(String kda) { this.kda = kda; }

    public String getGpmXpm() { return gpmXpm; }
    public void setGpmXpm(String gpmXpm) { this.gpmXpm = gpmXpm; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
}
