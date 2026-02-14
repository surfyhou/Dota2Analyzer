package com.dota2analyzer.core.service;

import com.dota2analyzer.core.model.opendota.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchCache {

    private static final Logger log = LoggerFactory.getLogger(MatchCache.class);
    private final String dbPath;
    private final ObjectMapper objectMapper;
    private volatile boolean initialized;

    public MatchCache() {
        this(null);
    }

    public MatchCache(String dbPath) {
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        if (dbPath != null) {
            this.dbPath = dbPath;
        } else {
            String home = System.getProperty("user.home");
            Path folder = Paths.get(home, ".dota2analyzer");
            try {
                Files.createDirectories(folder);
            } catch (Exception e) {
                log.warn("Failed to create cache directory", e);
            }
            this.dbPath = folder.resolve("match-cache.db").toString();
        }
    }

    public MatchDetail getMatchDetail(long matchId, Duration maxAge) {
        ensureInitialized();
        try (Connection conn = getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT json, updated_at FROM match_cache WHERE match_id = ?");
            stmt.setLong(1, matchId);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                log.debug("Match cache miss: {}", matchId);
                return null;
            }
            String json = rs.getString(1);
            String updatedAtStr = rs.getString(2);
            if (maxAge != null && isExpired(updatedAtStr, maxAge)) {
                log.debug("Match cache expired: {}", matchId);
                return null;
            }
            log.debug("Match cache hit: {}", matchId);
            return objectMapper.readValue(json, MatchDetail.class);
        } catch (Exception e) {
            log.warn("Failed to get match detail from cache", e);
            return null;
        }
    }

    public void saveMatchDetail(long matchId, MatchDetail detail) {
        ensureInitialized();
        try (Connection conn = getConnection()) {
            String json = objectMapper.writeValueAsString(detail);
            String now = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO match_cache(match_id, json, updated_at) VALUES (?, ?, ?) " +
                    "ON CONFLICT(match_id) DO UPDATE SET json = ?, updated_at = ?");
            stmt.setLong(1, matchId);
            stmt.setString(2, json);
            stmt.setString(3, now);
            stmt.setString(4, json);
            stmt.setString(5, now);
            stmt.executeUpdate();
            log.debug("Match cache saved: {}", matchId);
        } catch (Exception e) {
            log.warn("Failed to save match detail to cache", e);
        }
    }

    public List<RecentMatch> getRecentMatches(long accountId, Duration maxAge) {
        String json = getCacheRow("recent_matches_cache", "account_id", accountId, maxAge);
        if (json == null) {
            log.debug("Recent matches cache miss: {}", accountId);
            return null;
        }
        log.debug("Recent matches cache hit: {}", accountId);
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse recent matches from cache", e);
            return null;
        }
    }

    public void saveRecentMatches(long accountId, List<RecentMatch> matches) {
        saveCacheRow("recent_matches_cache", "account_id", accountId, matches);
        log.debug("Recent matches cache saved: {} ({})", accountId, matches.size());
    }

    public List<Hero> getHeroes(Duration maxAge) {
        String json = getCacheRow("hero_cache", "cache_key", "heroes", maxAge);
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse heroes from cache", e);
            return null;
        }
    }

    public void saveHeroes(List<Hero> heroes) {
        saveCacheRow("hero_cache", "cache_key", "heroes", heroes);
    }

    public List<HeroStats> getHeroStats(Duration maxAge) {
        String json = getCacheRow("hero_cache", "cache_key", "hero_stats", maxAge);
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse hero stats from cache", e);
            return null;
        }
    }

    public void saveHeroStats(List<HeroStats> stats) {
        saveCacheRow("hero_cache", "cache_key", "hero_stats", stats);
    }

    public Map<String, ItemConstants> getItemConstants(Duration maxAge) {
        String json = getCacheRow("hero_cache", "cache_key", "item_constants", maxAge);
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse item constants from cache", e);
            return null;
        }
    }

    public void saveItemConstants(Map<String, ItemConstants> items) {
        saveCacheRow("hero_cache", "cache_key", "item_constants", items);
    }

    private void ensureInitialized() {
        if (initialized) return;
        synchronized (this) {
            if (initialized) return;
            try (Connection conn = getConnection()) {
                Statement stmt = conn.createStatement();
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS match_cache (" +
                    "match_id INTEGER PRIMARY KEY, json TEXT NOT NULL, updated_at TEXT NOT NULL)");
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS recent_matches_cache (" +
                    "account_id INTEGER PRIMARY KEY, json TEXT NOT NULL, updated_at TEXT NOT NULL)");
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS hero_cache (" +
                    "cache_key TEXT PRIMARY KEY, json TEXT NOT NULL, updated_at TEXT NOT NULL)");
                initialized = true;
            } catch (Exception e) {
                log.error("Failed to initialize cache database", e);
            }
        }
    }

    private String getCacheRow(String table, String keyColumn, Object key, Duration maxAge) {
        ensureInitialized();
        try (Connection conn = getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT json, updated_at FROM " + table + " WHERE " + keyColumn + " = ?");
            if (key instanceof Long) {
                stmt.setLong(1, (Long) key);
            } else {
                stmt.setString(1, key.toString());
            }
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) return null;
            String json = rs.getString(1);
            String updatedAtStr = rs.getString(2);
            if (maxAge != null && isExpired(updatedAtStr, maxAge)) return null;
            return json;
        } catch (Exception e) {
            log.warn("Failed to get cache row from {}", table, e);
            return null;
        }
    }

    private void saveCacheRow(String table, String keyColumn, Object key, Object payload) {
        ensureInitialized();
        try (Connection conn = getConnection()) {
            String json = objectMapper.writeValueAsString(payload);
            String now = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO " + table + "(" + keyColumn + ", json, updated_at) VALUES (?, ?, ?) " +
                    "ON CONFLICT(" + keyColumn + ") DO UPDATE SET json = ?, updated_at = ?");
            if (key instanceof Long) {
                stmt.setLong(1, (Long) key);
            } else {
                stmt.setString(1, key.toString());
            }
            stmt.setString(2, json);
            stmt.setString(3, now);
            stmt.setString(4, json);
            stmt.setString(5, now);
            stmt.executeUpdate();
        } catch (Exception e) {
            log.warn("Failed to save cache row to {}", table, e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }

    private boolean isExpired(String updatedAtStr, Duration maxAge) {
        try {
            OffsetDateTime updatedAt = OffsetDateTime.parse(updatedAtStr);
            return updatedAt.plus(maxAge).isBefore(OffsetDateTime.now());
        } catch (Exception e) {
            return true;
        }
    }
}
