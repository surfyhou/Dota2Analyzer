package com.dota2analyzer.core.model.opendota;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RecentMatch {

    @JsonProperty("match_id")
    private long matchId;

    @JsonProperty("player_slot")
    private int playerSlot;

    @JsonProperty("radiant_win")
    private boolean radiantWin;

    @JsonProperty("duration")
    private int duration;

    @JsonProperty("start_time")
    private int startTime;

    @JsonProperty("hero_id")
    private int heroId;

    @JsonProperty("kills")
    private int kills;

    @JsonProperty("deaths")
    private int deaths;

    @JsonProperty("assists")
    private int assists;

    @JsonProperty("last_hits")
    private int lastHits;

    @JsonProperty("denies")
    private int denies;

    @JsonProperty("gold_per_min")
    private int goldPerMin;

    @JsonProperty("xp_per_min")
    private int xpPerMin;

    @JsonProperty("hero_damage")
    private int heroDamage;

    @JsonProperty("tower_damage")
    private int towerDamage;

    @JsonProperty("hero_healing")
    private int heroHealing;

    @JsonProperty("level")
    private int level;

    public RecentMatch() {}

    public long getMatchId() { return matchId; }
    public void setMatchId(long matchId) { this.matchId = matchId; }

    public int getPlayerSlot() { return playerSlot; }
    public void setPlayerSlot(int playerSlot) { this.playerSlot = playerSlot; }

    public boolean isRadiantWin() { return radiantWin; }
    public void setRadiantWin(boolean radiantWin) { this.radiantWin = radiantWin; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public int getStartTime() { return startTime; }
    public void setStartTime(int startTime) { this.startTime = startTime; }

    public int getHeroId() { return heroId; }
    public void setHeroId(int heroId) { this.heroId = heroId; }

    public int getKills() { return kills; }
    public void setKills(int kills) { this.kills = kills; }

    public int getDeaths() { return deaths; }
    public void setDeaths(int deaths) { this.deaths = deaths; }

    public int getAssists() { return assists; }
    public void setAssists(int assists) { this.assists = assists; }

    public int getLastHits() { return lastHits; }
    public void setLastHits(int lastHits) { this.lastHits = lastHits; }

    public int getDenies() { return denies; }
    public void setDenies(int denies) { this.denies = denies; }

    public int getGoldPerMin() { return goldPerMin; }
    public void setGoldPerMin(int goldPerMin) { this.goldPerMin = goldPerMin; }

    public int getXpPerMin() { return xpPerMin; }
    public void setXpPerMin(int xpPerMin) { this.xpPerMin = xpPerMin; }

    public int getHeroDamage() { return heroDamage; }
    public void setHeroDamage(int heroDamage) { this.heroDamage = heroDamage; }

    public int getTowerDamage() { return towerDamage; }
    public void setTowerDamage(int towerDamage) { this.towerDamage = towerDamage; }

    public int getHeroHealing() { return heroHealing; }
    public void setHeroHealing(int heroHealing) { this.heroHealing = heroHealing; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
}
