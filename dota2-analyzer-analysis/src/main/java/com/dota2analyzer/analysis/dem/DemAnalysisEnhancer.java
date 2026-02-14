package com.dota2analyzer.analysis.dem;

import com.dota2analyzer.analysis.dem.model.*;
import com.dota2analyzer.core.model.analysis.MatchAnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhances OpenDota-based analysis results with data from DEM replay parsing.
 */
public class DemAnalysisEnhancer {

    private static final Logger log = LoggerFactory.getLogger(DemAnalysisEnhancer.class);
    private static final int TICKS_PER_SECOND = 30;

    /**
     * Enhance the analysis result with DEM data.
     * Modifies the result in place, adding DEM-specific fields.
     */
    public void enhance(MatchAnalysisResult result, DemParseResult demData) {
        if (result == null || demData == null) return;

        log.info("Enhancing analysis for match {} with DEM data", result.getMatchId());

        result.setDemDataAvailable(true);

        // Build position heatmap data
        Map<String, Object> heatmapData = buildHeatmapData(demData);
        if (!heatmapData.isEmpty()) {
            result.setHeroPositionHeatmap(heatmapData);
        }

        // Build tick-level economy timeline
        Map<String, Object> economyData = buildEconomyData(demData);
        if (!economyData.isEmpty()) {
            result.setTickEconomyTimeline(economyData);
        }

        // Ward placement data
        if (!demData.getWardPlacements().isEmpty()) {
            result.setWardPlacements(demData.getWardPlacements());
        }

        // Combat detail summary
        Map<String, Object> combatSummary = buildCombatSummary(demData);
        if (!combatSummary.isEmpty()) {
            result.setCombatDetails(combatSummary);
        }

        // Ability timeline
        if (!demData.getAbilityUsages().isEmpty()) {
            result.setAbilityTimeline(demData.getAbilityUsages());
        }

        // Enhance laning details with position-based analysis
        enhanceLaningDetails(result, demData);

        log.info("DEM enhancement complete for match {}", result.getMatchId());
    }

    private Map<String, Object> buildHeatmapData(DemParseResult demData) {
        Map<String, Object> heatmap = new HashMap<>();
        for (Map.Entry<String, List<HeroPositionTick>> entry : demData.getHeroPositions().entrySet()) {
            List<int[]> positions = entry.getValue().stream()
                    .map(t -> new int[]{t.getCellX(), t.getCellY(), t.getTick()})
                    .collect(Collectors.toList());
            heatmap.put(entry.getKey(), positions);
        }
        return heatmap;
    }

    private Map<String, Object> buildEconomyData(DemParseResult demData) {
        Map<String, Object> economy = new HashMap<>();
        for (Map.Entry<String, List<EconomyTick>> entry : demData.getEconomyTimelines().entrySet()) {
            List<Map<String, Integer>> timeline = entry.getValue().stream()
                    .map(t -> {
                        Map<String, Integer> point = new LinkedHashMap<>();
                        point.put("tick", t.getTick());
                        point.put("time", t.getTick() / TICKS_PER_SECOND);
                        point.put("netWorth", t.getNetWorth());
                        point.put("lastHits", t.getLastHits());
                        point.put("denies", t.getDenies());
                        point.put("xp", t.getXp());
                        return point;
                    })
                    .collect(Collectors.toList());
            economy.put(entry.getKey(), timeline);
        }
        return economy;
    }

    private Map<String, Object> buildCombatSummary(DemParseResult demData) {
        Map<String, Object> summary = new HashMap<>();

        // Count kills by hero
        Map<String, Integer> killCounts = new HashMap<>();
        Map<String, Integer> deathCounts = new HashMap<>();
        long totalDamageEvents = 0;

        for (CombatEvent event : demData.getCombatEvents()) {
            if ("DOTA_COMBATLOG_DEATH".equals(event.getType())) {
                String attacker = event.getAttacker();
                String target = event.getTarget();
                if (attacker != null && attacker.startsWith("npc_dota_hero_")) {
                    killCounts.merge(attacker, 1, Integer::sum);
                }
                if (target != null && target.startsWith("npc_dota_hero_")) {
                    deathCounts.merge(target, 1, Integer::sum);
                }
            }
            if ("DOTA_COMBATLOG_DAMAGE".equals(event.getType())) {
                totalDamageEvents++;
            }
        }

        summary.put("killsByHero", killCounts);
        summary.put("deathsByHero", deathCounts);
        summary.put("totalDamageEvents", totalDamageEvents);
        summary.put("totalCombatEvents", demData.getCombatEvents().size());

        return summary;
    }

    private void enhanceLaningDetails(MatchAnalysisResult result, DemParseResult demData) {
        // Add DEM-based insights to laning details
        List<String> details = new ArrayList<>(result.getLaningDetails());

        // Check for early rotations (position changes in first 10 minutes)
        int tenMinuteTick = 10 * 60 * TICKS_PER_SECOND;
        for (Map.Entry<String, List<HeroPositionTick>> entry : demData.getHeroPositions().entrySet()) {
            List<HeroPositionTick> earlyTicks = entry.getValue().stream()
                    .filter(t -> t.getTick() <= tenMinuteTick)
                    .collect(Collectors.toList());

            if (earlyTicks.size() > 2) {
                // Detect lane changes by checking position variance
                int firstCellX = earlyTicks.get(0).getCellX();
                boolean laneChanged = earlyTicks.stream()
                        .anyMatch(t -> Math.abs(t.getCellX() - firstCellX) > 30);
                if (laneChanged) {
                    details.add("[DEM] " + entry.getKey() + " \u572810\u5206\u949F\u524D\u6709\u660E\u663E\u7684\u8DEF\u7EBF\u53D8\u52A8");
                }
            }
        }

        result.setLaningDetails(details);
    }
}
