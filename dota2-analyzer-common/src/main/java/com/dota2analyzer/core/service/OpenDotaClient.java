package com.dota2analyzer.core.service;

import com.dota2analyzer.core.model.opendota.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class OpenDotaClient implements DotaDataProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenDotaClient.class);
    private static final long MIN_INTERVAL_MS = 1100;
    private static final int MAX_RETRIES = 3;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final ReentrantLock rateLock = new ReentrantLock();
    private volatile long lastRequestTimeMs;

    public OpenDotaClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.opendota.com/api/")
                .build();
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public List<Hero> getHeroes() {
        String json = fetchWithRetry(() ->
                restClient.get().uri("heroes").retrieve().body(String.class));
        return parseList(json, new TypeReference<>() {});
    }

    public List<HeroStats> getHeroStats() {
        String json = fetchWithRetry(() ->
                restClient.get().uri("heroStats").retrieve().body(String.class));
        return parseList(json, new TypeReference<>() {});
    }

    public List<RecentMatch> getRecentMatches(long accountId, int limit) {
        String json = fetchWithRetry(() ->
                restClient.get()
                        .uri("players/{accountId}/recentMatches?limit={limit}&lobby_type=7", accountId, limit)
                        .retrieve().body(String.class));
        return parseList(json, new TypeReference<>() {});
    }

    public List<RecentMatch> getPlayerMatches(long accountId, int limit, int offset, int lobbyType) {
        String json = fetchWithRetry(() ->
                restClient.get()
                        .uri("players/{accountId}/matches?limit={limit}&offset={offset}&lobby_type={lobbyType}",
                                accountId, limit, offset, lobbyType)
                        .retrieve().body(String.class));
        return parseList(json, new TypeReference<>() {});
    }

    public MatchDetail getMatchDetail(long matchId) {
        try {
            String json = fetchWithRetry(() ->
                    restClient.get()
                            .uri("matches/{matchId}", matchId)
                            .retrieve().body(String.class));
            if (json == null) return null;
            return objectMapper.readValue(json, MatchDetail.class);
        } catch (Exception e) {
            log.error("Failed to get match detail for {}", matchId, e);
            return null;
        }
    }

    public BenchmarksResponse getHeroBenchmarks(int heroId) {
        try {
            String json = fetchWithRetry(() ->
                    restClient.get()
                            .uri("benchmarks?hero_id={heroId}", heroId)
                            .retrieve().body(String.class));
            if (json == null) return null;
            return objectMapper.readValue(json, BenchmarksResponse.class);
        } catch (Exception e) {
            log.error("Failed to get benchmarks for hero {}", heroId, e);
            return null;
        }
    }

    public Map<String, ItemConstants> getItemConstants() {
        try {
            String json = fetchWithRetry(() ->
                    restClient.get().uri("constants/items").retrieve().body(String.class));
            if (json == null) return new HashMap<>();
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to get item constants", e);
            return new HashMap<>();
        }
    }

    public boolean requestParse(long matchId) {
        try {
            fetchWithRetry(() -> {
                restClient.post().uri("request/{matchId}", matchId).retrieve().toBodilessEntity();
                return "";
            });
            return true;
        } catch (Exception e) {
            log.warn("Failed to request parse for match {}", matchId, e);
            return false;
        }
    }

    private void throttle() {
        rateLock.lock();
        try {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRequestTimeMs;
            if (elapsed < MIN_INTERVAL_MS) {
                long wait = MIN_INTERVAL_MS - elapsed;
                log.debug("Rate limit: waiting {}ms", wait);
                Thread.sleep(wait);
            }
            lastRequestTimeMs = System.currentTimeMillis();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            rateLock.unlock();
        }
    }

    private String fetchWithRetry(Supplier<String> request) {
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            throttle();
            try {
                return request.get();
            } catch (HttpClientErrorException.TooManyRequests e) {
                if (attempt == MAX_RETRIES) {
                    log.error("OpenDota 429 after {} retries, giving up", MAX_RETRIES);
                    throw e;
                }
                long backoff = (long) Math.pow(2, attempt + 1) * 1000;
                log.warn("OpenDota 429, retry {}/{} after {}ms", attempt + 1, MAX_RETRIES, backoff);
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
        return null;
    }

    private <T> List<T> parseList(String json, TypeReference<List<T>> typeRef) {
        if (json == null) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            log.error("Failed to parse JSON list", e);
            return new ArrayList<>();
        }
    }
}
