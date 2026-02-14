package com.dota2analyzer.data.service;

import com.dota2analyzer.core.model.opendota.MatchDetail;
import com.dota2analyzer.core.service.MatchCache;
import com.dota2analyzer.core.service.OpenDotaClient;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

public class DemDownloadService {

    private static final Logger log = LoggerFactory.getLogger(DemDownloadService.class);
    private static final Duration CLEANUP_AGE = Duration.ofDays(7);

    private final OpenDotaClient openDotaClient;
    private final MatchCache matchCache;
    private final Path replayDir;
    private final HttpClient httpClient;

    public DemDownloadService(OpenDotaClient openDotaClient, MatchCache matchCache) {
        this(openDotaClient, matchCache, null);
    }

    public DemDownloadService(OpenDotaClient openDotaClient, MatchCache matchCache, String replayDirPath) {
        this.openDotaClient = openDotaClient;
        this.matchCache = matchCache;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        if (replayDirPath != null) {
            this.replayDir = Paths.get(replayDirPath);
        } else {
            String home = System.getProperty("user.home");
            this.replayDir = Paths.get(home, ".dota2analyzer", "replays");
        }

        try {
            Files.createDirectories(this.replayDir);
        } catch (IOException e) {
            log.warn("Failed to create replay directory", e);
        }
    }

    /**
     * Ensure the replay file exists locally, downloading if necessary.
     * Returns the path to the .dem file, or empty if unavailable.
     */
    public Optional<Path> ensureReplayFile(long matchId) {
        return downloadReplay(matchId);
    }

    /**
     * Download and decompress a DEM replay file for the given match.
     * Returns the path to the .dem file, or empty if unavailable.
     */
    public Optional<Path> downloadReplay(long matchId) {
        // Check if already cached locally
        Path demFile = replayDir.resolve(matchId + ".dem");
        if (Files.exists(demFile)) {
            log.info("Replay already cached: {}", demFile);
            return Optional.of(demFile);
        }

        // Get match detail for cluster + replay_salt
        MatchDetail detail = matchCache.getMatchDetail(matchId, Duration.ofDays(30));
        if (detail == null) {
            detail = openDotaClient.getMatchDetail(matchId);
            if (detail != null) {
                matchCache.saveMatchDetail(matchId, detail);
            }
        }

        if (detail == null || detail.getCluster() == null || detail.getReplaySalt() == null) {
            log.warn("Cannot download replay for match {}: missing cluster or replay_salt", matchId);
            return Optional.empty();
        }

        String url = String.format("http://replay%d.valve.net/570/%d_%d.dem.bz2",
                detail.getCluster(), matchId, detail.getReplaySalt());

        log.info("Downloading replay from {}", url);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMinutes(5))
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                log.warn("Replay download failed with status {} for match {}", response.statusCode(), matchId);
                return Optional.empty();
            }

            // Decompress bz2 and write to local file
            Path tempFile = replayDir.resolve(matchId + ".dem.tmp");
            try (InputStream is = response.body();
                 BZip2CompressorInputStream bz2is = new BZip2CompressorInputStream(new BufferedInputStream(is));
                 OutputStream os = new BufferedOutputStream(Files.newOutputStream(tempFile))) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = bz2is.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }
            }

            Files.move(tempFile, demFile);
            log.info("Replay downloaded and decompressed: {} ({}KB)", demFile, Files.size(demFile) / 1024);
            return Optional.of(demFile);

        } catch (Exception e) {
            log.error("Failed to download replay for match {}", matchId, e);
            return Optional.empty();
        }
    }

    /**
     * Clean up replay files older than 7 days.
     */
    public void cleanupOldReplays() {
        try (Stream<Path> files = Files.list(replayDir)) {
            Instant cutoff = Instant.now().minus(CLEANUP_AGE);
            files.filter(f -> f.toString().endsWith(".dem"))
                    .filter(f -> {
                        try {
                            return Files.getLastModifiedTime(f).toInstant().isBefore(cutoff);
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .forEach(f -> {
                        try {
                            Files.delete(f);
                            log.info("Cleaned up old replay: {}", f.getFileName());
                        } catch (IOException e) {
                            log.warn("Failed to delete old replay: {}", f, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to list replay directory for cleanup", e);
        }
    }
}
