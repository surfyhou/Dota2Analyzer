package com.dota2analyzer.analysis.dem;

import com.dota2analyzer.analysis.dem.model.DemParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class DemParseService {

    private static final Logger log = LoggerFactory.getLogger(DemParseService.class);
    private final String replayDirPath;

    public DemParseService(String replayDirPath) {
        this.replayDirPath = replayDirPath;
    }

    /**
     * Parse a DEM replay file for the given match.
     * Looks for the replay file at {replayDirPath}/{matchId}.dem (downloaded by the data service).
     * Returns the parsed result, or empty if the replay is unavailable.
     */
    public Optional<DemParseResult> parseMatch(long matchId) {
        log.info("Attempting DEM parse for match {}", matchId);

        Path demFile = Paths.get(replayDirPath, matchId + ".dem");
        if (!Files.exists(demFile)) {
            log.info("DEM file not available at {} for match {}, skipping DEM parse", demFile, matchId);
            return Optional.empty();
        }

        try {
            DemProcessor processor = new DemProcessor();
            DemParseResult result = processor.parse(demFile, matchId);
            return Optional.of(result);
        } catch (Exception e) {
            log.error("DEM parse failed for match {}", matchId, e);
            return Optional.empty();
        }
    }
}
