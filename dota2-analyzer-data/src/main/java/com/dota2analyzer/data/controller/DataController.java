package com.dota2analyzer.data.controller;

import com.dota2analyzer.core.model.opendota.BenchmarksResponse;
import com.dota2analyzer.core.model.opendota.MatchDetail;
import com.dota2analyzer.core.model.opendota.RecentMatch;
import com.dota2analyzer.core.service.MatchCache;
import com.dota2analyzer.core.service.OpenDotaClient;
import com.dota2analyzer.data.dto.PreloadStatus;
import com.dota2analyzer.data.service.DemDownloadService;
import com.dota2analyzer.data.service.PreloadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/internal")
public class DataController {

    private final OpenDotaClient openDotaClient;
    private final MatchCache matchCache;
    private final DemDownloadService demDownloadService;
    private final PreloadService preloadService;

    public DataController(OpenDotaClient openDotaClient, MatchCache matchCache,
                         DemDownloadService demDownloadService, PreloadService preloadService) {
        this.openDotaClient = openDotaClient;
        this.matchCache = matchCache;
        this.demDownloadService = demDownloadService;
        this.preloadService = preloadService;
    }

    @PostMapping("/players/{accountId}/matches")
    public ResponseEntity<List<RecentMatch>> fetchPlayerMatches(
            @PathVariable long accountId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "7") int lobbyType) {
        List<RecentMatch> matches = openDotaClient.getPlayerMatches(accountId, limit, offset, lobbyType);
        if (!matches.isEmpty()) {
            matchCache.saveRecentMatches(accountId, matches);
        }
        return ResponseEntity.ok(matches);
    }

    @PostMapping("/matches/{matchId}/fetch")
    public ResponseEntity<MatchDetail> fetchMatchDetail(@PathVariable long matchId) {
        MatchDetail detail = openDotaClient.getMatchDetail(matchId);
        if (detail != null) {
            matchCache.saveMatchDetail(matchId, detail);
            return ResponseEntity.ok(detail);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/matches/{matchId}/request-parse")
    public ResponseEntity<Boolean> requestParse(@PathVariable long matchId) {
        boolean result = openDotaClient.requestParse(matchId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/heroes/{heroId}/benchmarks")
    public ResponseEntity<BenchmarksResponse> fetchHeroBenchmarks(@PathVariable int heroId) {
        BenchmarksResponse benchmarks = openDotaClient.getHeroBenchmarks(heroId);
        if (benchmarks != null) {
            matchCache.saveBenchmark(heroId, benchmarks);
            return ResponseEntity.ok(benchmarks);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/matches/{matchId}/download-dem")
    public ResponseEntity<String> downloadDem(@PathVariable long matchId) {
        Optional<Path> path = demDownloadService.ensureReplayFile(matchId);
        return path.map(p -> ResponseEntity.ok(p.toString()))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/players/{accountId}/preload")
    public ResponseEntity<PreloadStatus> startPreload(
            @PathVariable long accountId,
            @RequestParam(defaultValue = "50") int count) {
        PreloadStatus status = preloadService.start(accountId, count);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/players/{accountId}/preload-status")
    public ResponseEntity<PreloadStatus> getPreloadStatus(@PathVariable long accountId) {
        PreloadStatus status = preloadService.getStatus(accountId);
        return ResponseEntity.ok(status);
    }
}
