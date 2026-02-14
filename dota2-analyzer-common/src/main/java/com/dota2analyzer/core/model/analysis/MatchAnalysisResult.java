package com.dota2analyzer.core.model.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchAnalysisResult {
    private long matchId;
    private int heroId;
    private String heroName;
    private boolean won;
    private String resultText;
    private int laneRole;
    private boolean isPosition1;
    private String pickRound;
    private int pickIndex;
    private String laneResult;
    private int laneNetWorthDiff10;
    private String laneOpponentHero;
    private int laneOpponentHeroId;
    private List<String> laneAllyHeroes = new ArrayList<>();
    private List<String> laneEnemyHeroes = new ArrayList<>();
    private List<Integer> laneAllyHeroIds = new ArrayList<>();
    private List<Integer> laneEnemyHeroIds = new ArrayList<>();
    private String laneMatchup = "";
    private int laneKills;
    private int laneDeaths;
    private int playerDenies10;
    private int enemyDenies10;
    private List<String> laningDetails;
    private List<String> benchmarkNotes;
    private String performanceRating;
    private List<String> mistakes;
    private List<String> suggestions;
    private Map<String, String> statistics;
    private List<String> allyHeroes;
    private List<Integer> allyHeroIds;
    private List<String> enemyHeroes;
    private List<Integer> enemyHeroIds;
    private List<InventorySnapshot> inventoryTimeline;

    // DEM enhancement fields
    private boolean demDataAvailable;
    private Object heroPositionHeatmap;
    private Object tickEconomyTimeline;
    private Object wardPlacements;
    private Object combatDetails;
    private Object abilityTimeline;

    public MatchAnalysisResult() {}

    public long getMatchId() { return matchId; }
    public void setMatchId(long matchId) { this.matchId = matchId; }

    public int getHeroId() { return heroId; }
    public void setHeroId(int heroId) { this.heroId = heroId; }

    public String getHeroName() { return heroName; }
    public void setHeroName(String heroName) { this.heroName = heroName; }

    public boolean isWon() { return won; }
    public void setWon(boolean won) { this.won = won; }

    public String getResultText() { return resultText; }
    public void setResultText(String resultText) { this.resultText = resultText; }

    public int getLaneRole() { return laneRole; }
    public void setLaneRole(int laneRole) { this.laneRole = laneRole; }

    public boolean isPosition1() { return isPosition1; }
    public void setPosition1(boolean position1) { isPosition1 = position1; }

    public String getPickRound() { return pickRound; }
    public void setPickRound(String pickRound) { this.pickRound = pickRound; }

    public int getPickIndex() { return pickIndex; }
    public void setPickIndex(int pickIndex) { this.pickIndex = pickIndex; }

    public String getLaneResult() { return laneResult; }
    public void setLaneResult(String laneResult) { this.laneResult = laneResult; }

    public int getLaneNetWorthDiff10() { return laneNetWorthDiff10; }
    public void setLaneNetWorthDiff10(int laneNetWorthDiff10) { this.laneNetWorthDiff10 = laneNetWorthDiff10; }

    public String getLaneOpponentHero() { return laneOpponentHero; }
    public void setLaneOpponentHero(String laneOpponentHero) { this.laneOpponentHero = laneOpponentHero; }

    public int getLaneOpponentHeroId() { return laneOpponentHeroId; }
    public void setLaneOpponentHeroId(int laneOpponentHeroId) { this.laneOpponentHeroId = laneOpponentHeroId; }

    public List<String> getLaneAllyHeroes() { return laneAllyHeroes; }
    public void setLaneAllyHeroes(List<String> laneAllyHeroes) { this.laneAllyHeroes = laneAllyHeroes; }

    public List<String> getLaneEnemyHeroes() { return laneEnemyHeroes; }
    public void setLaneEnemyHeroes(List<String> laneEnemyHeroes) { this.laneEnemyHeroes = laneEnemyHeroes; }

    public List<Integer> getLaneAllyHeroIds() { return laneAllyHeroIds; }
    public void setLaneAllyHeroIds(List<Integer> laneAllyHeroIds) { this.laneAllyHeroIds = laneAllyHeroIds; }

    public List<Integer> getLaneEnemyHeroIds() { return laneEnemyHeroIds; }
    public void setLaneEnemyHeroIds(List<Integer> laneEnemyHeroIds) { this.laneEnemyHeroIds = laneEnemyHeroIds; }

    public String getLaneMatchup() { return laneMatchup; }
    public void setLaneMatchup(String laneMatchup) { this.laneMatchup = laneMatchup; }

    public int getLaneKills() { return laneKills; }
    public void setLaneKills(int laneKills) { this.laneKills = laneKills; }

    public int getLaneDeaths() { return laneDeaths; }
    public void setLaneDeaths(int laneDeaths) { this.laneDeaths = laneDeaths; }

    public int getPlayerDenies10() { return playerDenies10; }
    public void setPlayerDenies10(int playerDenies10) { this.playerDenies10 = playerDenies10; }

    public int getEnemyDenies10() { return enemyDenies10; }
    public void setEnemyDenies10(int enemyDenies10) { this.enemyDenies10 = enemyDenies10; }

    public List<String> getLaningDetails() { return laningDetails; }
    public void setLaningDetails(List<String> laningDetails) { this.laningDetails = laningDetails; }

    public List<String> getBenchmarkNotes() { return benchmarkNotes; }
    public void setBenchmarkNotes(List<String> benchmarkNotes) { this.benchmarkNotes = benchmarkNotes; }

    public String getPerformanceRating() { return performanceRating; }
    public void setPerformanceRating(String performanceRating) { this.performanceRating = performanceRating; }

    public List<String> getMistakes() { return mistakes; }
    public void setMistakes(List<String> mistakes) { this.mistakes = mistakes; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }

    public Map<String, String> getStatistics() { return statistics; }
    public void setStatistics(Map<String, String> statistics) { this.statistics = statistics; }

    public List<String> getAllyHeroes() { return allyHeroes; }
    public void setAllyHeroes(List<String> allyHeroes) { this.allyHeroes = allyHeroes; }

    public List<Integer> getAllyHeroIds() { return allyHeroIds; }
    public void setAllyHeroIds(List<Integer> allyHeroIds) { this.allyHeroIds = allyHeroIds; }

    public List<String> getEnemyHeroes() { return enemyHeroes; }
    public void setEnemyHeroes(List<String> enemyHeroes) { this.enemyHeroes = enemyHeroes; }

    public List<Integer> getEnemyHeroIds() { return enemyHeroIds; }
    public void setEnemyHeroIds(List<Integer> enemyHeroIds) { this.enemyHeroIds = enemyHeroIds; }

    public List<InventorySnapshot> getInventoryTimeline() { return inventoryTimeline; }
    public void setInventoryTimeline(List<InventorySnapshot> inventoryTimeline) { this.inventoryTimeline = inventoryTimeline; }

    public boolean isDemDataAvailable() { return demDataAvailable; }
    public void setDemDataAvailable(boolean demDataAvailable) { this.demDataAvailable = demDataAvailable; }

    public Object getHeroPositionHeatmap() { return heroPositionHeatmap; }
    public void setHeroPositionHeatmap(Object heroPositionHeatmap) { this.heroPositionHeatmap = heroPositionHeatmap; }

    public Object getTickEconomyTimeline() { return tickEconomyTimeline; }
    public void setTickEconomyTimeline(Object tickEconomyTimeline) { this.tickEconomyTimeline = tickEconomyTimeline; }

    public Object getWardPlacements() { return wardPlacements; }
    public void setWardPlacements(Object wardPlacements) { this.wardPlacements = wardPlacements; }

    public Object getCombatDetails() { return combatDetails; }
    public void setCombatDetails(Object combatDetails) { this.combatDetails = combatDetails; }

    public Object getAbilityTimeline() { return abilityTimeline; }
    public void setAbilityTimeline(Object abilityTimeline) { this.abilityTimeline = abilityTimeline; }
}
