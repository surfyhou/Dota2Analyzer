package com.dota2analyzer.api.config;

import com.dota2analyzer.api.service.HeroImageCache;
import com.dota2analyzer.api.service.ItemImageCache;
import com.dota2analyzer.api.service.PreloadService;
import com.dota2analyzer.clarity.analysis.DemAnalysisEnhancer;
import com.dota2analyzer.clarity.download.DemDownloadService;
import com.dota2analyzer.clarity.service.DemParseService;
import com.dota2analyzer.core.analysis.MatchAnalyzer;
import com.dota2analyzer.core.service.HeroDataCache;
import com.dota2analyzer.core.service.MatchCache;
import com.dota2analyzer.core.service.OpenDotaClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

    @Value("${analyzer.cache-only:false}")
    private boolean cacheOnly;

    @Value("${analyzer.disable-benchmarks:false}")
    private boolean disableBenchmarks;

    @Value("${analyzer.avoid-external-when-cached:true}")
    private boolean avoidExternalWhenCached;

    @Bean
    public OpenDotaClient openDotaClient() {
        return new OpenDotaClient(RestClient.builder());
    }

    @Bean
    public MatchCache matchCache() {
        return new MatchCache();
    }

    @Bean
    public HeroDataCache heroDataCache(OpenDotaClient openDotaClient, MatchCache matchCache) {
        return new HeroDataCache(openDotaClient, matchCache, cacheOnly);
    }

    @Bean
    public MatchAnalyzer matchAnalyzer(OpenDotaClient openDotaClient, HeroDataCache heroDataCache, MatchCache matchCache) {
        return new MatchAnalyzer(openDotaClient, heroDataCache, matchCache,
                cacheOnly, disableBenchmarks, avoidExternalWhenCached);
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

    @Bean
    public DemParseService demParseService(DemDownloadService demDownloadService) {
        return new DemParseService(demDownloadService);
    }

    @Bean
    public DemAnalysisEnhancer demAnalysisEnhancer() {
        return new DemAnalysisEnhancer();
    }
}
