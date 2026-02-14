package com.dota2analyzer.api.controller;

import com.dota2analyzer.api.dto.PreloadMatchRow;
import com.dota2analyzer.api.dto.PreloadStatus;
import com.dota2analyzer.api.dto.RecentMatchDto;
import com.dota2analyzer.api.service.PreloadService;
import com.dota2analyzer.core.analysis.MatchAnalyzer;
import com.dota2analyzer.core.model.analysis.AnalysisResponse;
import com.dota2analyzer.core.model.analysis.AnalysisSummary;
import com.dota2analyzer.core.model.analysis.MatchAnalysisResult;
import com.dota2analyzer.core.model.opendota.MatchDetail;
import com.dota2analyzer.core.model.opendota.RecentMatch;
import com.dota2analyzer.core.service.HeroDataCache;
import com.dota2analyzer.core.service.MatchCache;
import com.dota2analyzer.core.service.OpenDotaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private static final Logger log = LoggerFactory.getLogger(PlayerController.class);

    private final HeroDataCache heroData;
    private final OpenDotaClient client;
    private final MatchCache cache;
    private final MatchAnalyzer analyzer;
    private final PreloadService preloadService;

    public PlayerController(HeroDataCache heroData, OpenDotaClient client, MatchCache cache,
                           MatchAnalyzer analyzer, PreloadService preloadService) {
        this.heroData = heroData;
        this.client = client;
        this.cache = cache;
        this.analyzer = analyzer;
        this.preloadService = preloadService;
    }

    @GetMapping("/{accountId}/recent")
    public List<RecentMatchDto> getRecentMatches(
            @PathVariable long accountId,
            @RequestParam(required = false, defaultValue = "20") int limit) {

        int take = Math.min(Math.max(limit, 1), 50);
        log.info("GET recent matches AccountId={} Limit={}", accountId, take);
        heroData.ensureLoaded();

        List<RecentMatch> matches = cache.getRecentMatches(accountId, Duration.ofMinutes(30));
        if (matches == null || matches.isEmpty()) {
            matches = client.getRecentMatches(accountId, take);
            if (!matches.isEmpty()) {
                cache.saveRecentMatches(accountId, matches);
            }
        }

        return matches.stream().map(match -> {
            boolean isRadiant = match.getPlayerSlot() < 128;
            boolean won = (isRadiant && match.isRadiantWin()) || (!isRadiant && !match.isRadiantWin());
            return new RecentMatchDto(
                    match.getMatchId(),
                    heroData.getHeroName(match.getHeroId()),
                    won ? "胜利" : "失败",
                    won,
                    match.getKills() + "/" + match.getDeaths() + "/" + match.getAssists(),
                    match.getGoldPerMin() + "/" + match.getXpPerMin(),
                    match.getDuration() / 60
            );
        }).collect(Collectors.toList());
    }

    @PostMapping("/{accountId}/analyze-recent")
    public AnalysisResponse analyzeRecent(
            @PathVariable long accountId,
            @RequestParam(required = false, defaultValue = "20") int limit,
            @RequestParam(required = false, defaultValue = "true") boolean requestParse,
            @RequestParam(required = false, defaultValue = "true") boolean onlyPos1) {

        int desiredCount = Math.min(Math.max(limit, 1), 50);
        int fetchLimit = Math.min(Math.max(desiredCount, 200), 200);
        log.info("POST analyze recent AccountId={} Desired={} FetchLimit={} OnlyPos1={} Parse={}",
                accountId, desiredCount, fetchLimit, onlyPos1, requestParse);

        List<MatchAnalysisResult> analyses = analyzer.analyzeRecent(
                accountId, desiredCount, fetchLimit, requestParse, onlyPos1);

        AnalysisSummary summary = buildSummary(analyses);
        return new AnalysisResponse(summary, analyses);
    }

    @PostMapping("/{accountId}/preload")
    public PreloadStatus preload(
            @PathVariable long accountId,
            @RequestParam(required = false, defaultValue = "100") int count) {

        int total = Math.min(Math.max(count, 1), 200);
        return preloadService.start(accountId, total);
    }

    @GetMapping("/{accountId}/preload-status")
    public PreloadStatus preloadStatus(@PathVariable long accountId) {
        return preloadService.getStatus(accountId);
    }

    @GetMapping("/{accountId}/cached-matches")
    public List<PreloadMatchRow> cachedMatches(
            @PathVariable long accountId,
            @RequestParam(required = false, defaultValue = "100") int count) {

        int take = Math.min(Math.max(count, 1), 200);
        List<RecentMatch> matches = cache.getRecentMatches(accountId, Duration.ofDays(30));
        if (matches == null) matches = new ArrayList<>();

        List<PreloadMatchRow> rows = new ArrayList<>();
        List<RecentMatch> sorted = matches.stream()
                .sorted(Comparator.comparingInt(RecentMatch::getStartTime).reversed())
                .limit(take)
                .collect(Collectors.toList());

        Duration matchTtl = cache.isPermanentAccount(accountId) ? null : Duration.ofDays(30);
        for (RecentMatch match : sorted) {
            MatchDetail detail = cache.getMatchDetail(match.getMatchId(), matchTtl);
            List<Integer> radiantHeroes = new ArrayList<>();
            List<Integer> direHeroes = new ArrayList<>();
            boolean radiantWin = match.isRadiantWin();
            int duration = match.getDuration();

            if (detail != null && detail.getPlayers() != null) {
                radiantWin = detail.isRadiantWin();
                duration = detail.getDuration();
                for (var p : detail.getPlayers()) {
                    if (p.getPlayerSlot() < 128) radiantHeroes.add(p.getHeroId());
                    else direHeroes.add(p.getHeroId());
                }
            }

            PreloadMatchRow row = new PreloadMatchRow();
            row.setMatchId(match.getMatchId());
            row.setRadiantWin(radiantWin);
            row.setDuration(duration);
            row.setStartTime(match.getStartTime());
            row.setRadiantHeroes(radiantHeroes);
            row.setDireHeroes(direHeroes);
            rows.add(row);
        }

        return rows;
    }

    private AnalysisSummary buildSummary(List<MatchAnalysisResult> analyses) {
        int total = analyses.size();
        int wins = (int) analyses.stream().filter(MatchAnalysisResult::isWon).count();
        int parsed = (int) analyses.stream().filter(a -> !a.getLaneResult().contains("尚未解析")).count();
        int unparsed = total - parsed;
        double winRate = total == 0 ? 0 : (double) wins / total * 100;
        return new AnalysisSummary(total, wins, winRate, parsed, unparsed);
    }
}
