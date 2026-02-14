package com.dota2analyzer.analysis.service;

import com.dota2analyzer.core.model.opendota.*;
import com.dota2analyzer.core.service.DotaDataProvider;
import com.dota2analyzer.core.service.MatchCache;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.*;

public class DataServiceClient implements DotaDataProvider {
    private static final Logger log = LoggerFactory.getLogger(DataServiceClient.class);
    private final RestClient restClient;
    private final MatchCache cache;
    private final ObjectMapper objectMapper;

    public DataServiceClient(String dataServiceUrl, MatchCache cache) {
        this.restClient = RestClient.builder().baseUrl(dataServiceUrl).build();
        this.cache = cache;
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public List<RecentMatch> getPlayerMatches(long accountId, int limit, int offset, int lobbyType) {
        // Always fetch from data service (this is a list query, not cached individually)
        try {
            String json = restClient.post()
                    .uri("/internal/players/{accountId}/matches?limit={limit}&offset={offset}&lobbyType={lobbyType}",
                            accountId, limit, offset, lobbyType)
                    .retrieve().body(String.class);
            if (json == null) return new ArrayList<>();
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to fetch player matches from data service", e);
            return new ArrayList<>();
        }
    }

    @Override
    public MatchDetail getMatchDetail(long matchId) {
        // Cache-first
        MatchDetail cached = cache.getMatchDetail(matchId, null);
        if (cached != null) return cached;

        try {
            String json = restClient.post()
                    .uri("/internal/matches/{matchId}/fetch", matchId)
                    .retrieve().body(String.class);
            if (json == null) return null;
            return objectMapper.readValue(json, MatchDetail.class);
        } catch (Exception e) {
            log.error("Failed to fetch match detail from data service for {}", matchId, e);
            return null;
        }
    }

    @Override
    public BenchmarksResponse getHeroBenchmarks(int heroId) {
        // Cache-first (1 day TTL)
        BenchmarksResponse cached = cache.getBenchmark(heroId, Duration.ofDays(1));
        if (cached != null) return cached;

        try {
            String json = restClient.post()
                    .uri("/internal/heroes/{heroId}/benchmarks", heroId)
                    .retrieve().body(String.class);
            if (json == null) return null;
            return objectMapper.readValue(json, BenchmarksResponse.class);
        } catch (Exception e) {
            log.error("Failed to fetch benchmarks from data service for hero {}", heroId, e);
            return null;
        }
    }

    @Override
    public boolean requestParse(long matchId) {
        try {
            restClient.post()
                    .uri("/internal/matches/{matchId}/request-parse", matchId)
                    .retrieve().body(String.class);
            return true;
        } catch (Exception e) {
            log.warn("Failed to request parse via data service for {}", matchId, e);
            return false;
        }
    }
}
