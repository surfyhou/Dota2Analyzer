package com.dota2analyzer.analysis.config;

import com.dota2analyzer.analysis.dem.DemAnalysisEnhancer;
import com.dota2analyzer.analysis.dem.DemParseService;
import com.dota2analyzer.analysis.engine.MatchAnalyzer;
import com.dota2analyzer.analysis.service.DataServiceClient;
import com.dota2analyzer.core.service.DotaDataProvider;
import com.dota2analyzer.core.service.HeroDataCache;
import com.dota2analyzer.core.service.MatchCache;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
@ConfigurationProperties(prefix = "analyzer")
public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    private String cacheOnly = "true";
    private String disableBenchmarks = "false";
    private String avoidExternalWhenCached = "true";
    private String dataServiceUrl = "http://localhost:5087";
    private String replayDir;
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

    public void setCacheOnly(String cacheOnly) { this.cacheOnly = cacheOnly; }
    public void setDisableBenchmarks(String disableBenchmarks) { this.disableBenchmarks = disableBenchmarks; }
    public void setAvoidExternalWhenCached(String avoidExternalWhenCached) { this.avoidExternalWhenCached = avoidExternalWhenCached; }
    public void setDataServiceUrl(String dataServiceUrl) { this.dataServiceUrl = dataServiceUrl; }
    public void setReplayDir(String replayDir) { this.replayDir = replayDir; }
    public void setPermanentAccounts(List<Long> permanentAccounts) { this.permanentAccounts = permanentAccounts; }
    public void setDb(Db db) { this.db = db; }

    @Bean
    public MatchCache matchCache() {
        Set<Long> accounts = permanentAccounts != null ? new HashSet<>(permanentAccounts) : Set.of();
        log.info("MatchCache permanent accounts: {}", accounts);
        return new MatchCache(db.getUrl(), db.getUser(), db.getPassword(), accounts);
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
        return new MatchAnalyzer(dataProvider, heroDataCache, matchCache,
                Boolean.parseBoolean(cacheOnly), Boolean.parseBoolean(disableBenchmarks),
                Boolean.parseBoolean(avoidExternalWhenCached));
    }

    @Bean
    public DemParseService demParseService() {
        String dir = replayDir != null ? replayDir : System.getProperty("user.home") + "/.dota2analyzer/replays";
        return new DemParseService(dir);
    }

    @Bean
    public DemAnalysisEnhancer demAnalysisEnhancer() {
        return new DemAnalysisEnhancer();
    }
}
