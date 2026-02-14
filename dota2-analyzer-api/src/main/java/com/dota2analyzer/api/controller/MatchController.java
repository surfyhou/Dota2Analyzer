package com.dota2analyzer.api.controller;

import com.dota2analyzer.clarity.analysis.DemAnalysisEnhancer;
import com.dota2analyzer.clarity.model.DemParseResult;
import com.dota2analyzer.clarity.service.DemParseService;
import com.dota2analyzer.core.analysis.MatchAnalyzer;
import com.dota2analyzer.core.model.analysis.MatchAnalysisResult;
import com.dota2analyzer.core.model.opendota.MatchDetail;
import com.dota2analyzer.core.model.opendota.PlayerDetail;
import com.dota2analyzer.core.model.opendota.RecentMatch;
import com.dota2analyzer.core.service.MatchCache;
import com.dota2analyzer.core.service.OpenDotaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private static final Logger log = LoggerFactory.getLogger(MatchController.class);

    private final MatchAnalyzer analyzer;
    private final OpenDotaClient client;
    private final MatchCache cache;
    private final DemParseService demParseService;
    private final DemAnalysisEnhancer demEnhancer;

    public MatchController(MatchAnalyzer analyzer, OpenDotaClient client, MatchCache cache,
                          DemParseService demParseService, DemAnalysisEnhancer demEnhancer) {
        this.analyzer = analyzer;
        this.client = client;
        this.cache = cache;
        this.demParseService = demParseService;
        this.demEnhancer = demEnhancer;
    }

    @GetMapping("/{matchId}/analyze")
    public ResponseEntity<?> analyzeMatch(
            @PathVariable long matchId,
            @RequestParam long accountId,
            @RequestParam(required = false, defaultValue = "true") boolean requestParse,
            @RequestParam(required = false, defaultValue = "false") boolean enableDem) {

        log.info("GET analyze match MatchId={} AccountId={} Parse={} EnableDem={}", matchId, accountId, requestParse, enableDem);

        if (requestParse) {
            client.requestParse(matchId);
        }

        MatchDetail detail = cache.getMatchDetail(matchId, Duration.ofDays(7));
        if (detail == null) {
            detail = client.getMatchDetail(matchId);
            if (detail != null) {
                cache.saveMatchDetail(matchId, detail);
            }
        }

        if (detail == null || detail.getPlayers() == null) {
            log.warn("Match not parsed or not found MatchId={}", matchId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "对局尚未解析或不存在"));
        }

        int accountId32 = (int) accountId;
        PlayerDetail player = detail.getPlayers().stream()
                .filter(p -> p.getAccountId() != null && p.getAccountId() == accountId32)
                .findFirst().orElse(null);

        if (player == null) {
            log.warn("Account {} not found in match {}", accountId, matchId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "未在对局中找到该玩家"));
        }

        RecentMatch recentMatch = new RecentMatch();
        recentMatch.setMatchId(detail.getMatchId());
        recentMatch.setPlayerSlot(player.getPlayerSlot());
        recentMatch.setRadiantWin(detail.isRadiantWin());
        recentMatch.setDuration(detail.getDuration());
        recentMatch.setHeroId(player.getHeroId());
        recentMatch.setKills(player.getKills());
        recentMatch.setDeaths(player.getDeaths());
        recentMatch.setAssists(player.getAssists());
        recentMatch.setLastHits(player.getLastHits());
        recentMatch.setDenies(player.getDenies());
        recentMatch.setGoldPerMin(player.getGoldPerMin());
        recentMatch.setXpPerMin(player.getXpPerMin());
        recentMatch.setHeroDamage(player.getHeroDamage());
        recentMatch.setTowerDamage(player.getTowerDamage());
        recentMatch.setHeroHealing(0);
        recentMatch.setLevel(player.getLevel());

        MatchAnalysisResult analysis = analyzer.analyzeMatch(recentMatch, accountId, requestParse, false);
        if (analysis == null) {
            log.warn("Analysis failed MatchId={} AccountId={}", matchId, accountId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "无法生成分析"));
        }

        // Optionally enhance with DEM data
        if (enableDem) {
            try {
                Optional<DemParseResult> demResult = demParseService.parseMatch(matchId);
                demResult.ifPresent(dem -> demEnhancer.enhance(analysis, dem));
            } catch (Exception e) {
                log.warn("DEM enhancement failed for match {}, continuing with base analysis", matchId, e);
            }
        }

        return ResponseEntity.ok(analysis);
    }
}
