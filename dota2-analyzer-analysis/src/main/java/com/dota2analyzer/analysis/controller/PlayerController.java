package com.dota2analyzer.analysis.controller;

import com.dota2analyzer.analysis.dto.RecentMatchDto;
import com.dota2analyzer.analysis.engine.MatchAnalyzer;
import com.dota2analyzer.core.model.analysis.AnalysisResponse;
import com.dota2analyzer.core.model.analysis.AnalysisSummary;
import com.dota2analyzer.core.model.analysis.MatchAnalysisResult;
import com.dota2analyzer.core.model.opendota.RecentMatch;
import com.dota2analyzer.core.service.HeroDataCache;
import com.dota2analyzer.core.service.MatchCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private static final Logger log = LoggerFactory.getLogger(PlayerController.class);

    private final HeroDataCache heroData;
    private final MatchCache cache;
    private final MatchAnalyzer analyzer;

    public PlayerController(HeroDataCache heroData, MatchCache cache, MatchAnalyzer analyzer) {
        this.heroData = heroData;
        this.cache = cache;
        this.analyzer = analyzer;
    }

    @GetMapping("/{accountId}/recent")
    public List<RecentMatchDto> getRecentMatches(
            @PathVariable long accountId,
            @RequestParam(required = false, defaultValue = "20") int limit) {

        int take = Math.min(Math.max(limit, 1), 50);
        log.info("GET recent matches AccountId={} Limit={}", accountId, take);
        heroData.ensureLoaded();

        List<RecentMatch> matches = cache.getRecentMatches(accountId, Duration.ofMinutes(30));
        if (matches == null) {
            matches = new ArrayList<>();
        }

        return matches.stream().limit(take).map(match -> {
            boolean isRadiant = match.getPlayerSlot() < 128;
            boolean won = (isRadiant && match.isRadiantWin()) || (!isRadiant && !match.isRadiantWin());
            return new RecentMatchDto(
                    match.getMatchId(),
                    heroData.getHeroName(match.getHeroId()),
                    won ? "\u80DC\u5229" : "\u5931\u8D25",
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

    private AnalysisSummary buildSummary(List<MatchAnalysisResult> analyses) {
        int total = analyses.size();
        int wins = (int) analyses.stream().filter(MatchAnalysisResult::isWon).count();
        int parsed = (int) analyses.stream().filter(a -> !a.getLaneResult().contains("\u5C1A\u672A\u89E3\u6790")).count();
        int unparsed = total - parsed;
        double winRate = total == 0 ? 0 : (double) wins / total * 100;
        return new AnalysisSummary(total, wins, winRate, parsed, unparsed);
    }
}
