package com.dota2analyzer.analysis.config;

import com.dota2analyzer.analysis.dem.DemAnalysisEnhancer;
import com.dota2analyzer.analysis.dem.DemParseService;
import com.dota2analyzer.analysis.engine.MatchAnalyzer;
import com.dota2analyzer.analysis.service.DataServiceClient;
import com.dota2analyzer.core.service.DotaDataProvider;
import com.dota2analyzer.core.service.HeroDataCache;
import com.dota2analyzer.core.service.MatchCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class AppConfig {

    @Value("${analyzer.db.url}")
    private String dbUrl;

    @Value("${analyzer.db.user}")
    private String dbUser;

    @Value("${analyzer.db.password}")
    private String dbPassword;

    @Value("${analyzer.permanent-accounts:}")
    private List<Long> permanentAccounts;

    @Value("${analyzer.cache-only:true}")
    private boolean cacheOnly;

    @Value("${analyzer.disable-benchmarks:false}")
    private boolean disableBenchmarks;

    @Value("${analyzer.avoid-external-when-cached:true}")
    private boolean avoidExternalWhenCached;

    @Value("${analyzer.data-service-url:http://localhost:5087}")
    private String dataServiceUrl;

    @Value("${analyzer.replay-dir:#{T(System).getProperty('user.home') + '/.dota2analyzer/replays'}}")
    private String replayDir;

    @Bean
    public MatchCache matchCache() {
        Set<Long> accounts = permanentAccounts != null
                ? permanentAccounts.stream().collect(Collectors.toSet())
                : Set.of();
        return new MatchCache(dbUrl, dbUser, dbPassword, accounts);
    }

    @Bean
    public HeroDataCache heroDataCache(MatchCache matchCache) {
        return new HeroDataCache(null, matchCache, true);
    }

    @Bean
    public DataServiceClient dataServiceClient(MatchCache matchCache) {
        return new DataServiceClient(dataServiceUrl, matchCache);
    }

    @Bean
    public MatchAnalyzer matchAnalyzer(DotaDataProvider dataProvider, HeroDataCache heroDataCache, MatchCache matchCache) {
        return new MatchAnalyzer(dataProvider, heroDataCache, matchCache, cacheOnly, disableBenchmarks, avoidExternalWhenCached);
    }

    @Bean
    public DemParseService demParseService() {
        return new DemParseService(replayDir);
    }

    @Bean
    public DemAnalysisEnhancer demAnalysisEnhancer() {
        return new DemAnalysisEnhancer();
    }
}
