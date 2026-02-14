package com.dota2analyzer.data.service;

import com.dota2analyzer.core.model.opendota.Hero;
import com.dota2analyzer.core.service.HeroDataCache;
import com.dota2analyzer.core.service.MatchCache;
import com.dota2analyzer.core.service.OpenDotaClient;
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
import java.util.List;

public class HeroImageCache {

    private static final Logger log = LoggerFactory.getLogger(HeroImageCache.class);
    private static final String[] BASE_URLS = {
            "https://cdn.opendota.com/apps/dota2/images/heroes",
            "https://cdn.cloudflare.steamstatic.com/apps/dota2/images/heroes"
    };

    private final HeroDataCache heroData;
    private final MatchCache cache;
    private final OpenDotaClient client;
    private final HttpClient httpClient;
    private final Path heroImageDir;

    public HeroImageCache(HeroDataCache heroData, MatchCache cache, OpenDotaClient client) {
        this.heroData = heroData;
        this.cache = cache;
        this.client = client;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

        String home = System.getProperty("user.home");
        this.heroImageDir = Paths.get(home, ".dota2analyzer", "assets", "heroes");
        try {
            Files.createDirectories(heroImageDir);
        } catch (IOException e) {
            log.warn("Failed to create hero image directory", e);
        }
    }

    public String ensureHeroImage(int heroId) {
        log.debug("EnsureHeroImage heroId={}", heroId);
        String key = resolveHeroKey(heroId);
        if (key == null || key.isBlank()) return null;

        String heroKey = key.replace("npc_dota_hero_", "");
        String fileName = heroKey + "_full.png";
        Path localPath = heroImageDir.resolve(fileName);

        if (Files.exists(localPath)) {
            log.debug("Hero image cache hit {}", localPath);
            return localPath.toString();
        }

        for (String baseUrl : BASE_URLS) {
            String url = baseUrl + "/" + heroKey + "_full.png";
            log.info("Downloading hero image {}", url);
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .GET().build();
                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() != 200) {
                    log.warn("Hero image download failed {} for {}", response.statusCode(), url);
                    continue;
                }
                try (InputStream is = response.body();
                     OutputStream os = new BufferedOutputStream(Files.newOutputStream(localPath))) {
                    is.transferTo(os);
                }
                log.info("Hero image saved {}", localPath);
                return localPath.toString();
            } catch (Exception ex) {
                log.warn("Hero image download failed for {}", url, ex);
            }
        }

        return null;
    }

    private String resolveHeroKey(int heroId) {
        heroData.ensureLoaded();
        String key = heroData.getHeroKey(heroId);
        if (key != null && !key.isBlank()) return key;

        List<Hero> heroes = cache.getHeroes(Duration.ofDays(30));
        if (heroes == null || heroes.isEmpty()) {
            heroes = client.getHeroes();
            if (!heroes.isEmpty()) {
                cache.saveHeroes(heroes);
            }
        }

        return heroes.stream()
                .filter(h -> h.getId() == heroId)
                .map(Hero::getName)
                .findFirst().orElse("");
    }
}
