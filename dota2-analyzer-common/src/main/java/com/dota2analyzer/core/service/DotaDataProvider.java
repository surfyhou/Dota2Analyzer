package com.dota2analyzer.core.service;

import com.dota2analyzer.core.model.opendota.BenchmarksResponse;
import com.dota2analyzer.core.model.opendota.MatchDetail;
import com.dota2analyzer.core.model.opendota.RecentMatch;

import java.util.List;

public interface DotaDataProvider {
    List<RecentMatch> getPlayerMatches(long accountId, int limit, int offset, int lobbyType);
    MatchDetail getMatchDetail(long matchId);
    BenchmarksResponse getHeroBenchmarks(int heroId);
    boolean requestParse(long matchId);
}
