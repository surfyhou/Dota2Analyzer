package com.dota2analyzer.core.service;

import com.dota2analyzer.core.model.opendota.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.*;

public class OpenDotaClient {

    private static final Logger log = LoggerFactory.getLogger(OpenDotaClient.class);
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OpenDotaClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.opendota.com/api/")
                .build();
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public List<Hero> getHeroes() {
        String json = restClient.get().uri("heroes").retrieve().body(String.class);
        return parseList(json, new TypeReference<>() {});
    }

    public List<HeroStats> getHeroStats() {
        String json = restClient.get().uri("heroStats").retrieve().body(String.class);
        return parseList(json, new TypeReference<>() {});
    }

    public List<RecentMatch> getRecentMatches(long accountId, int limit) {
        String json = restClient.get()
                .uri("players/{accountId}/recentMatches?limit={limit}&lobby_type=7", accountId, limit)
                .retrieve().body(String.class);
        return parseList(json, new TypeReference<>() {});
    }

    public List<RecentMatch> getPlayerMatches(long accountId, int limit, int offset, int lobbyType) {
        String json = restClient.get()
                .uri("players/{accountId}/matches?limit={limit}&offset={offset}&lobby_type={lobbyType}",
                        accountId, limit, offset, lobbyType)
                .retrieve().body(String.class);
        return parseList(json, new TypeReference<>() {});
    }

    public MatchDetail getMatchDetail(long matchId) {
        try {
            String json = restClient.get()
                    .uri("matches/{matchId}", matchId)
                    .retrieve().body(String.class);
            if (json == null) return null;
            return objectMapper.readValue(json, MatchDetail.class);
        } catch (Exception e) {
            log.error("Failed to get match detail for {}", matchId, e);
            return null;
        }
    }

    public BenchmarksResponse getHeroBenchmarks(int heroId) {
        try {
            String json = restClient.get()
                    .uri("benchmarks?hero_id={heroId}", heroId)
                    .retrieve().body(String.class);
            if (json == null) return null;
            return objectMapper.readValue(json, BenchmarksResponse.class);
        } catch (Exception e) {
            log.error("Failed to get benchmarks for hero {}", heroId, e);
            return null;
        }
    }

    public Map<String, ItemConstants> getItemConstants() {
        try {
            String json = restClient.get().uri("constants/items").retrieve().body(String.class);
            if (json == null) return new HashMap<>();
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to get item constants", e);
            return new HashMap<>();
        }
    }

    public boolean requestParse(long matchId) {
        try {
            restClient.post().uri("request/{matchId}", matchId).retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.warn("Failed to request parse for match {}", matchId, e);
            return false;
        }
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
