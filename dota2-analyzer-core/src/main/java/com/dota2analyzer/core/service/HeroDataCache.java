package com.dota2analyzer.core.service;

import com.dota2analyzer.core.model.opendota.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class HeroDataCache {

    private static final Logger log = LoggerFactory.getLogger(HeroDataCache.class);
    private final OpenDotaClient client;
    private final MatchCache cache;
    private final boolean cacheOnly;
    private final ReentrantLock gate = new ReentrantLock();

    private Map<Integer, String> heroNames = new HashMap<>();
    private Map<Integer, String> heroKeys = new HashMap<>();
    private Map<Integer, Double> heroWinRates = new HashMap<>();
    private Map<String, ItemConstants> itemConstants = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private Map<Integer, String> itemIdToKey = new HashMap<>();
    private volatile boolean loaded;

    public HeroDataCache(OpenDotaClient client, MatchCache cache, boolean cacheOnly) {
        this.client = client;
        this.cache = cache;
        this.cacheOnly = cacheOnly;
    }

    public void ensureLoaded() {
        if (loaded) return;
        gate.lock();
        try {
            if (loaded) return;

            // Load heroes
            List<Hero> heroes = cache.getHeroes(Duration.ofDays(30));
            if (heroes == null || heroes.isEmpty()) {
                if (cacheOnly) {
                    log.warn("Hero cache empty and cache-only enabled. Skipping OpenDota.");
                    heroes = new ArrayList<>();
                } else {
                    log.info("Hero cache empty, fetching from OpenDota");
                    heroes = client.getHeroes();
                    if (!heroes.isEmpty()) {
                        cache.saveHeroes(heroes);
                    }
                }
            } else if (heroes.stream().anyMatch(h -> h.getName() == null || h.getName().isBlank())) {
                if (cacheOnly) {
                    log.warn("Hero cache missing key names and cache-only enabled.");
                } else {
                    log.info("Hero cache missing key names, refreshing from OpenDota");
                    List<Hero> refreshed = client.getHeroes();
                    if (!refreshed.isEmpty()) {
                        heroes = refreshed;
                        cache.saveHeroes(heroes);
                    }
                }
            }

            heroNames = new HashMap<>();
            heroKeys = new HashMap<>();
            for (Hero h : heroes) {
                heroNames.put(h.getId(), h.getLocalizedName());
                heroKeys.put(h.getId(), h.getName());
            }

            // Load hero stats
            List<HeroStats> stats = cache.getHeroStats(Duration.ofDays(30));
            if (stats == null || stats.isEmpty()) {
                if (cacheOnly) {
                    log.warn("Hero stats cache empty and cache-only enabled.");
                    stats = new ArrayList<>();
                } else {
                    log.info("Hero stats cache empty, fetching from OpenDota");
                    stats = client.getHeroStats();
                    if (!stats.isEmpty()) {
                        cache.saveHeroStats(stats);
                    }
                }
            }

            heroWinRates = new HashMap<>();
            for (HeroStats s : stats) {
                if (s.getProPick() > 0) {
                    heroWinRates.put(s.getId(), (double) s.getProWin() / s.getProPick());
                }
            }

            // Load item constants
            Map<String, ItemConstants> items = cache.getItemConstants(Duration.ofDays(30));
            if (items == null || items.isEmpty()) {
                if (cacheOnly) {
                    log.warn("Item constants cache empty and cache-only enabled.");
                    items = new HashMap<>();
                } else {
                    log.info("Item constants cache empty, fetching from OpenDota");
                    items = client.getItemConstants();
                    if (!items.isEmpty()) {
                        cache.saveItemConstants(items);
                    }
                }
            } else if (items.values().stream().allMatch(i -> i == null || i.getId() == null)) {
                if (!cacheOnly) {
                    log.info("Item constants cache missing ids, refreshing from OpenDota");
                    Map<String, ItemConstants> refreshed = client.getItemConstants();
                    if (!refreshed.isEmpty()) {
                        items = refreshed;
                        cache.saveItemConstants(items);
                    }
                }
            } else if (items.values().stream().allMatch(i -> i == null || i.getComponents() == null)) {
                if (!cacheOnly) {
                    log.info("Item constants cache missing components, refreshing from OpenDota");
                    Map<String, ItemConstants> refreshed = client.getItemConstants();
                    if (!refreshed.isEmpty()) {
                        items = refreshed;
                        cache.saveItemConstants(items);
                    }
                }
            }

            this.itemConstants = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            this.itemConstants.putAll(items);

            this.itemIdToKey = new HashMap<>();
            for (Map.Entry<String, ItemConstants> entry : this.itemConstants.entrySet()) {
                if (entry.getValue() != null && entry.getValue().getId() != null
                        && entry.getValue().getId() > 0 && entry.getKey() != null && !entry.getKey().isBlank()) {
                    this.itemIdToKey.putIfAbsent(entry.getValue().getId(), entry.getKey());
                }
            }

            loaded = true;
            log.info("Hero data loaded. Heroes={} Stats={} Items={}", heroNames.size(), heroWinRates.size(), this.itemConstants.size());
        } finally {
            gate.unlock();
        }
    }

    public String getHeroName(int heroId) {
        String name = heroNames.get(heroId);
        return name != null ? name : "英雄" + heroId;
    }

    public String getHeroKey(int heroId) {
        String key = heroKeys.get(heroId);
        return (key != null && !key.isBlank()) ? key : "";
    }

    public boolean tryGetWinRate(int heroId, double[] out) {
        Double rate = heroWinRates.get(heroId);
        if (rate != null) {
            out[0] = rate;
            return true;
        }
        return false;
    }

    public ItemConstants getItemConstants(String key) {
        return itemConstants.get(key);
    }

    public boolean tryGetItemKeyById(int itemId, String[] out) {
        if (itemId <= 0) {
            out[0] = "";
            return false;
        }
        String resolved = itemIdToKey.get(itemId);
        if (resolved != null && !resolved.isBlank()) {
            out[0] = resolved;
            return true;
        }
        out[0] = "";
        return false;
    }
}
