package com.dota2analyzer.clarity.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DemParseResult {
    private long matchId;
    // heroName -> list of position ticks
    private Map<String, List<HeroPositionTick>> heroPositions = new HashMap<>();
    // heroName -> list of economy ticks
    private Map<String, List<EconomyTick>> economyTimelines = new HashMap<>();
    private List<CombatEvent> combatEvents = new ArrayList<>();
    private List<WardPlacement> wardPlacements = new ArrayList<>();
    private List<AbilityUsage> abilityUsages = new ArrayList<>();

    public DemParseResult() {}

    public DemParseResult(long matchId) {
        this.matchId = matchId;
    }

    public long getMatchId() { return matchId; }
    public void setMatchId(long matchId) { this.matchId = matchId; }

    public Map<String, List<HeroPositionTick>> getHeroPositions() { return heroPositions; }
    public void setHeroPositions(Map<String, List<HeroPositionTick>> heroPositions) { this.heroPositions = heroPositions; }

    public Map<String, List<EconomyTick>> getEconomyTimelines() { return economyTimelines; }
    public void setEconomyTimelines(Map<String, List<EconomyTick>> economyTimelines) { this.economyTimelines = economyTimelines; }

    public List<CombatEvent> getCombatEvents() { return combatEvents; }
    public void setCombatEvents(List<CombatEvent> combatEvents) { this.combatEvents = combatEvents; }

    public List<WardPlacement> getWardPlacements() { return wardPlacements; }
    public void setWardPlacements(List<WardPlacement> wardPlacements) { this.wardPlacements = wardPlacements; }

    public List<AbilityUsage> getAbilityUsages() { return abilityUsages; }
    public void setAbilityUsages(List<AbilityUsage> abilityUsages) { this.abilityUsages = abilityUsages; }
}
