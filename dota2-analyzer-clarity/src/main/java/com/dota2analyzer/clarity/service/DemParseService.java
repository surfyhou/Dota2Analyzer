package com.dota2analyzer.clarity.service;

import com.dota2analyzer.clarity.download.DemDownloadService;
import com.dota2analyzer.clarity.model.DemParseResult;
import com.dota2analyzer.clarity.processor.DemProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Optional;

public class DemParseService {

    private static final Logger log = LoggerFactory.getLogger(DemParseService.class);
    private final DemDownloadService downloadService;

    public DemParseService(DemDownloadService downloadService) {
        this.downloadService = downloadService;
    }

    /**
     * Download and parse a DEM replay file for the given match.
     * Returns the parsed result, or empty if the replay is unavailable.
     */
    public Optional<DemParseResult> parseMatch(long matchId) {
        log.info("Attempting DEM parse for match {}", matchId);

        Optional<Path> demFile = downloadService.downloadReplay(matchId);
        if (demFile.isEmpty()) {
            log.info("DEM file not available for match {}, skipping DEM parse", matchId);
            return Optional.empty();
        }

        try {
            DemProcessor processor = new DemProcessor();
            DemParseResult result = processor.parse(demFile.get(), matchId);
            return Optional.of(result);
        } catch (Exception e) {
            log.error("DEM parse failed for match {}", matchId, e);
            return Optional.empty();
        }
    }
}
