package com.dota2analyzer.api.service;

import com.dota2analyzer.core.model.opendota.ItemConstants;
import com.dota2analyzer.core.service.HeroDataCache;
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

public class ItemImageCache {

    private static final Logger log = LoggerFactory.getLogger(ItemImageCache.class);
    private static final String[] BASE_URLS = {
            "https://cdn.opendota.com",
            "https://cdn.cloudflare.steamstatic.com"
    };

    private final HeroDataCache heroData;
    private final HttpClient httpClient;
    private final Path itemImageDir;

    public ItemImageCache(HeroDataCache heroData) {
        this.heroData = heroData;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

        String home = System.getProperty("user.home");
        this.itemImageDir = Paths.get(home, ".dota2analyzer", "assets", "items");
        try {
            Files.createDirectories(itemImageDir);
        } catch (IOException e) {
            log.warn("Failed to create item image directory", e);
        }
    }

    public String ensureItemImage(String itemKey) {
        if (itemKey == null || itemKey.isBlank()) return null;

        String normalized = normalizeKey(itemKey);
        if (normalized.isBlank()) return null;

        Path localPath = itemImageDir.resolve(normalized + ".png");
        if (Files.exists(localPath)) {
            log.debug("Item image cache hit {}", localPath);
            return localPath.toString();
        }

        heroData.ensureLoaded();
        ItemConstants constants = heroData.getItemConstants(normalized);
        if (constants == null || constants.getImg() == null || constants.getImg().isBlank()) {
            log.warn("Item constants or image path missing for {}", normalized);
            return null;
        }

        String imgPath = constants.getImg();

        // If absolute URL
        if (imgPath.startsWith("http")) {
            return downloadImage(imgPath, localPath);
        }

        // Try with base URLs
        String trimmed = imgPath.startsWith("/") ? imgPath : "/" + imgPath;
        for (String baseUrl : BASE_URLS) {
            String result = downloadImage(baseUrl + trimmed, localPath);
            if (result != null) return result;
        }

        return null;
    }

    private String downloadImage(String url, Path localPath) {
        log.info("Downloading item image {}", url);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET().build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                log.warn("Item image download failed {} for {}", response.statusCode(), url);
                return null;
            }
            try (InputStream is = response.body();
                 OutputStream os = new BufferedOutputStream(Files.newOutputStream(localPath))) {
                is.transferTo(os);
            }
            log.info("Item image saved {}", localPath);
            return localPath.toString();
        } catch (Exception ex) {
            log.warn("Item image download failed for {}", url, ex);
            return null;
        }
    }

    private static String normalizeKey(String key) {
        String normalized = key.trim().toLowerCase();
        if (normalized.startsWith("item_")) {
            normalized = normalized.substring(5);
        }
        return normalized;
    }
}
