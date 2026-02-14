package com.dota2analyzer.api.service;

import com.dota2analyzer.core.model.opendota.MatchDetail;
import com.dota2analyzer.core.model.opendota.RecentMatch;
import com.dota2analyzer.core.service.MatchCache;
import com.dota2analyzer.core.service.OpenDotaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MatchSyncService {

    private static final Logger log = LoggerFactory.getLogger(MatchSyncService.class);

    private final OpenDotaClient client;
    private final MatchCache cache;
    private final HeroImageCache heroImages;
    private final List<Long> syncAccounts;

    public MatchSyncService(OpenDotaClient client, MatchCache cache, HeroImageCache heroImages,
                           @Value("${analyzer.permanent-accounts:}") List<Long> syncAccounts) {
        this.client = client;
        this.cache = cache;
        this.heroImages = heroImages;
        this.syncAccounts = syncAccounts != null ? syncAccounts : List.of();
    }

    @Scheduled(fixedRate = 30 * 60 * 1000, initialDelay = 60 * 1000)
    public void syncNewMatches() {
        for (long accountId : syncAccounts) {
            try {
                syncAccount(accountId);
            } catch (Exception e) {
                log.error("Sync failed for account {}", accountId, e);
            }
        }
    }

    private void syncAccount(long accountId) {
        log.info("Sync check for account {}", accountId);

        // Fetch latest 20 matches from OpenDota
        List<RecentMatch> latest = client.getPlayerMatches(accountId, 20, 0, 7);
        if (latest.isEmpty()) {
            log.info("No matches returned from OpenDota for {}", accountId);
            return;
        }

        // Load existing cached match list
        List<RecentMatch> cached = cache.getRecentMatches(accountId, null);
        if (cached == null) cached = new ArrayList<>();

        // Find new match IDs not in cache
        var cachedIds = cached.stream().map(RecentMatch::getMatchId).collect(Collectors.toSet());
        List<RecentMatch> newMatches = latest.stream()
                .filter(m -> !cachedIds.contains(m.getMatchId()))
                .collect(Collectors.toList());

        if (newMatches.isEmpty()) {
            log.info("No new matches for account {}", accountId);
            return;
        }

        log.info("Found {} new matches for account {}", newMatches.size(), accountId);

        // Merge new matches into cached list and save
        List<RecentMatch> merged = new ArrayList<>(cached);
        merged.addAll(newMatches);
        merged = merged.stream()
                .collect(Collectors.toMap(RecentMatch::getMatchId, m -> m, (a, b) -> a))
                .values().stream()
                .sorted(Comparator.comparingInt(RecentMatch::getStartTime).reversed())
                .collect(Collectors.toList());
        cache.saveRecentMatches(accountId, merged);

        // Fetch details for new matches
        for (RecentMatch match : newMatches) {
            try {
                MatchDetail detail = cache.getMatchDetail(match.getMatchId(), null);
                if (detail == null) {
                    client.requestParse(match.getMatchId());
                    detail = client.getMatchDetail(match.getMatchId());
                    if (detail != null) {
                        cache.saveMatchDetail(match.getMatchId(), detail);
                    }
                }

                // Preload hero images
                if (detail != null && detail.getPlayers() != null) {
                    for (var p : detail.getPlayers()) {
                        heroImages.ensureHeroImage(p.getHeroId());
                    }
                }

                log.info("Synced match {} for account {}", match.getMatchId(), accountId);
            } catch (Exception e) {
                log.warn("Failed to sync match {} for account {}", match.getMatchId(), accountId, e);
            }
        }
    }
}
