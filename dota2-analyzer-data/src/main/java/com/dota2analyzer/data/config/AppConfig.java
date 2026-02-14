package com.dota2analyzer.data.config;

import com.dota2analyzer.core.service.HeroDataCache;
import com.dota2analyzer.core.service.MatchCache;
import com.dota2analyzer.core.service.OpenDotaClient;
import com.dota2analyzer.data.service.DemDownloadService;
import com.dota2analyzer.data.service.HeroImageCache;
import com.dota2analyzer.data.service.ItemImageCache;
import com.dota2analyzer.data.service.PreloadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
@ConfigurationProperties(prefix = "analyzer")
public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    private boolean cacheOnly = false;
    private List<Long> permanentAccounts = new ArrayList<>();
    private Db db = new Db();

    public static class Db {
        private String url;
        private String user;
        private String password;
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUser() { return user; }
        public void setUser(String user) { this.user = user; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public void setCacheOnly(boolean cacheOnly) { this.cacheOnly = cacheOnly; }
    public void setPermanentAccounts(List<Long> permanentAccounts) { this.permanentAccounts = permanentAccounts; }
    public void setDb(Db db) { this.db = db; }

    @Bean
    public OpenDotaClient openDotaClient() {
        return new OpenDotaClient(RestClient.builder());
    }

    @Bean
    public MatchCache matchCache() {
        Set<Long> accounts = permanentAccounts != null ? new HashSet<>(permanentAccounts) : Set.of();
        log.info("MatchCache permanent accounts: {}", accounts);
        return new MatchCache(db.getUrl(), db.getUser(), db.getPassword(), accounts);
    }

    @Bean
    public HeroDataCache heroDataCache(OpenDotaClient openDotaClient, MatchCache matchCache) {
        return new HeroDataCache(openDotaClient, matchCache, cacheOnly);
    }

    @Bean
    public HeroImageCache heroImageCache(HeroDataCache heroDataCache, MatchCache matchCache, OpenDotaClient openDotaClient) {
        return new HeroImageCache(heroDataCache, matchCache, openDotaClient);
    }

    @Bean
    public ItemImageCache itemImageCache(HeroDataCache heroDataCache) {
        return new ItemImageCache(heroDataCache);
    }

    @Bean
    public PreloadService preloadService(OpenDotaClient openDotaClient, MatchCache matchCache, HeroImageCache heroImageCache) {
        return new PreloadService(openDotaClient, matchCache, heroImageCache);
    }

    @Bean
    public DemDownloadService demDownloadService(OpenDotaClient openDotaClient, MatchCache matchCache) {
        return new DemDownloadService(openDotaClient, matchCache);
    }
}
