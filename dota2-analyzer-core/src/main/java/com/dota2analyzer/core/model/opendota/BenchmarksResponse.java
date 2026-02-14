package com.dota2analyzer.core.model.opendota;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BenchmarksResponse {

    @JsonProperty("hero_id")
    private int heroId;

    @JsonProperty("result")
    private Map<String, List<BenchmarkEntry>> result = new HashMap<>();

    public BenchmarksResponse() {}

    public int getHeroId() { return heroId; }
    public void setHeroId(int heroId) { this.heroId = heroId; }

    public Map<String, List<BenchmarkEntry>> getResult() { return result; }
    public void setResult(Map<String, List<BenchmarkEntry>> result) { this.result = result; }
}
