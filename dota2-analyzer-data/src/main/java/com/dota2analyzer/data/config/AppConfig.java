package com.dota2analyzer.data.config;

import com.dota2analyzer.core.service.HeroDataCache;
import com.dota2analyzer.core.service.MatchCache;
import com.dota2analyzer.core.service.OpenDotaClient;
import com.dota2analyzer.data.service.DemDownloadService;
import com.dota2analyzer.data.service.HeroImageCache;
import com.dota2analyzer.data.service.ItemImageCache;
import com.dota2analyzer.data.service.PreloadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class AppConfig {

    @Value("${analyzer.cache-only:false}")
    private boolean cacheOnly;

    @Value("${analyzer.db.url}")
    private String dbUrl;

    @Value("${analyzer.db.user}")
    private String dbUser;

    @Value("${analyzer.db.password}")
    private String dbPassword;

    @Value("${analyzer.permanent-accounts:}")
    private List<Long> permanentAccountsList;

    @Bean
    public OpenDotaClient openDotaClient() {
        return new OpenDotaClient(RestClient.builder());
    }

    @Bean
    public MatchCache matchCache() {
        Set<Long> permanentAccounts = permanentAccountsList != null
                ? permanentAccountsList.stream().collect(Collectors.toSet())
                : Set.of();
        return new MatchCache(dbUrl, dbUser, dbPassword, permanentAccounts);
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
