package com.dota2analyzer.analysis.dem;

import com.dota2analyzer.analysis.dem.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.Clarity;
import skadistats.clarity.model.CombatLogEntry;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.processor.entities.Entities;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.gameevents.OnCombatLogEntry;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.processor.reader.OnTickEnd;
import skadistats.clarity.source.MappedFileSource;

import java.nio.file.Path;
import java.util.*;

@UsesEntities
public class DemProcessor {

    private static final Logger log = LoggerFactory.getLogger(DemProcessor.class);
    private static final int TICKS_PER_SECOND = 30;
    private static final int SAMPLE_INTERVAL_TICKS = TICKS_PER_SECOND; // Sample every second

    private final Map<String, List<HeroPositionTick>> heroPositions = new HashMap<>();
    private final Map<String, List<EconomyTick>> economyTimelines = new HashMap<>();
    private final List<CombatEvent> combatEvents = new ArrayList<>();
    private final List<WardPlacement> wardPlacements = new ArrayList<>();
    private final List<AbilityUsage> abilityUsages = new ArrayList<>();

    private int currentTick = 0;
    private Context ctx;

    public DemParseResult parse(Path demFile, long matchId) {
        log.info("Starting DEM parse for match {} from {}", matchId, demFile);

        try {
            SimpleRunner runner = new SimpleRunner(new MappedFileSource(demFile.toString()));
            runner.runWith(this);
        } catch (Exception e) {
            log.error("Failed to parse DEM file for match {}", matchId, e);
        }

        DemParseResult result = new DemParseResult(matchId);
        result.setHeroPositions(heroPositions);
        result.setEconomyTimelines(economyTimelines);
        result.setCombatEvents(combatEvents);
        result.setWardPlacements(wardPlacements);
        result.setAbilityUsages(abilityUsages);

        log.info("DEM parse complete for match {}. Heroes tracked: {}, Combat events: {}, Wards: {}",
                matchId, heroPositions.size(), combatEvents.size(), wardPlacements.size());

        return result;
    }

    @OnTickEnd
    public void onTickEnd(Context ctx, boolean synthetic) {
        this.ctx = ctx;
        currentTick = ctx.getTick();

        if (currentTick % SAMPLE_INTERVAL_TICKS != 0) return;

        try {
            Entities entities = ctx.getProcessor(Entities.class);
            if (entities == null) return;

            // Iterate through hero entities
            Iterator<Entity> it = entities.getAllByDtName("CDOTA_Unit_Hero");
            while (it.hasNext()) {
                Entity hero = it.next();
                if (hero == null) continue;

                String heroName = getEntityName(hero);
                if (heroName == null || heroName.isEmpty()) continue;

                // Position tracking
                try {
                    Integer cellX = hero.getProperty("CBodyComponent.m_cellX");
                    Integer cellY = hero.getProperty("CBodyComponent.m_cellY");
                    if (cellX != null && cellY != null) {
                        heroPositions.computeIfAbsent(heroName, k -> new ArrayList<>())
                                .add(new HeroPositionTick(currentTick, cellX, cellY));
                    }
                } catch (Exception ignored) {}

                // Economy tracking
                try {
                    Integer netWorth = getIntProperty(hero, "m_iNetWorth");
                    Integer lastHits = getIntProperty(hero, "m_iLastHitCount");
                    Integer denies = getIntProperty(hero, "m_iDenyCount");
                    Integer xp = getIntProperty(hero, "m_iCurrentXP");

                    if (netWorth != null) {
                        economyTimelines.computeIfAbsent(heroName, k -> new ArrayList<>())
                                .add(new EconomyTick(currentTick,
                                        netWorth != null ? netWorth : 0,
                                        lastHits != null ? lastHits : 0,
                                        denies != null ? denies : 0,
                                        xp != null ? xp : 0));
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            // Silently skip tick errors
        }
    }

    @OnCombatLogEntry
    public void onCombatLogEntry(Context ctx, CombatLogEntry cle) {
        try {
            String type = cle.getType().name();
            String attacker = cle.getAttackerName();
            String target = cle.getTargetName();
            String inflictor = cle.getInflictorName();
            int value = cle.getValue();

            combatEvents.add(new CombatEvent(ctx.getTick(), type, attacker, target, inflictor, value));

            // Detect ward placements
            if ("DOTA_COMBATLOG_PURCHASE".equals(type)) {
                if (inflictor != null && (inflictor.contains("ward_observer") || inflictor.contains("ward_sentry"))) {
                    String wardType = inflictor.contains("observer") ? "observer" : "sentry";
                    wardPlacements.add(new WardPlacement(ctx.getTick(), 0, 0, wardType, attacker));
                }
            }

            // Detect ability usage
            if ("DOTA_COMBATLOG_ABILITY".equals(type) && inflictor != null && !inflictor.isEmpty()) {
                abilityUsages.add(new AbilityUsage(ctx.getTick(), inflictor, target));
            }
        } catch (Exception ignored) {}
    }

    private String getEntityName(Entity entity) {
        try {
            Object name = entity.getProperty("m_iszUnitName");
            return name != null ? name.toString() : null;
        } catch (Exception e) {
            try {
                String dtName = entity.getDtClass().getDtName();
                if (dtName != null && dtName.startsWith("CDOTA_Unit_Hero_")) {
                    return "npc_dota_hero_" + dtName.substring("CDOTA_Unit_Hero_".length()).toLowerCase();
                }
            } catch (Exception ignored) {}
            return null;
        }
    }

    private Integer getIntProperty(Entity entity, String property) {
        try {
            Object val = entity.getProperty(property);
            if (val instanceof Number) {
                return ((Number) val).intValue();
            }
        } catch (Exception ignored) {}
        return null;
    }
}
