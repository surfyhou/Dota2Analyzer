package com.dota2analyzer.core.model.opendota;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class MatchDetail {

    @JsonProperty("match_id")
    private long matchId;

    @JsonProperty("duration")
    private int duration;

    @JsonProperty("radiant_win")
    private boolean radiantWin;

    @JsonProperty("players")
    private List<PlayerDetail> players;

    @JsonProperty("picks_bans")
    private List<PickBan> picksBans;

    @JsonProperty("cluster")
    private Integer cluster;

    @JsonProperty("replay_salt")
    private Integer replaySalt;

    public MatchDetail() {}

    public long getMatchId() { return matchId; }
    public void setMatchId(long matchId) { this.matchId = matchId; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public boolean isRadiantWin() { return radiantWin; }
    public void setRadiantWin(boolean radiantWin) { this.radiantWin = radiantWin; }

    public List<PlayerDetail> getPlayers() { return players; }
    public void setPlayers(List<PlayerDetail> players) { this.players = players; }

    public List<PickBan> getPicksBans() { return picksBans; }
    public void setPicksBans(List<PickBan> picksBans) { this.picksBans = picksBans; }

    public Integer getCluster() { return cluster; }
    public void setCluster(Integer cluster) { this.cluster = cluster; }

    public Integer getReplaySalt() { return replaySalt; }
    public void setReplaySalt(Integer replaySalt) { this.replaySalt = replaySalt; }
}
