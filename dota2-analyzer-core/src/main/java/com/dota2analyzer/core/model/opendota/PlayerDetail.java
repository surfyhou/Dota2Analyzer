package com.dota2analyzer.core.model.opendota;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class PlayerDetail {

    @JsonProperty("account_id")
    private Integer accountId;

    @JsonProperty("player_slot")
    private int playerSlot;

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

    @JsonProperty("level")
    private int level;

    @JsonProperty("lane")
    private int lane;

    @JsonProperty("lane_role")
    private Integer laneRole;

    @JsonProperty("hero_damage")
    private int heroDamage;

    @JsonProperty("tower_damage")
    private int towerDamage;

    @JsonProperty("item_0")
    private int item0;

    @JsonProperty("item_1")
    private int item1;

    @JsonProperty("item_2")
    private int item2;

    @JsonProperty("item_3")
    private int item3;

    @JsonProperty("item_4")
    private int item4;

    @JsonProperty("item_5")
    private int item5;

    @JsonProperty("backpack_0")
    private int backpack0;

    @JsonProperty("backpack_1")
    private int backpack1;

    @JsonProperty("backpack_2")
    private int backpack2;

    @JsonProperty("item_neutral")
    private int itemNeutral;

    @JsonProperty("gold_t")
    private List<Integer> goldT;

    @JsonProperty("lh_t")
    private List<Integer> lastHitsT;

    @JsonProperty("dn_t")
    private List<Integer> deniesT;

    @JsonProperty("xp_t")
    private List<Integer> xpT;

    @JsonProperty("purchase_log")
    private List<PurchaseLogEntry> purchaseLog;

    @JsonProperty("kills_log")
    private List<KillLogEntry> killsLog;

    public PlayerDetail() {}

    public Integer getAccountId() { return accountId; }
    public void setAccountId(Integer accountId) { this.accountId = accountId; }

    public int getPlayerSlot() { return playerSlot; }
    public void setPlayerSlot(int playerSlot) { this.playerSlot = playerSlot; }

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

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getLane() { return lane; }
    public void setLane(int lane) { this.lane = lane; }

    public Integer getLaneRole() { return laneRole; }
    public void setLaneRole(Integer laneRole) { this.laneRole = laneRole; }

    public int getHeroDamage() { return heroDamage; }
    public void setHeroDamage(int heroDamage) { this.heroDamage = heroDamage; }

    public int getTowerDamage() { return towerDamage; }
    public void setTowerDamage(int towerDamage) { this.towerDamage = towerDamage; }

    public int getItem0() { return item0; }
    public void setItem0(int item0) { this.item0 = item0; }

    public int getItem1() { return item1; }
    public void setItem1(int item1) { this.item1 = item1; }

    public int getItem2() { return item2; }
    public void setItem2(int item2) { this.item2 = item2; }

    public int getItem3() { return item3; }
    public void setItem3(int item3) { this.item3 = item3; }

    public int getItem4() { return item4; }
    public void setItem4(int item4) { this.item4 = item4; }

    public int getItem5() { return item5; }
    public void setItem5(int item5) { this.item5 = item5; }

    public int getBackpack0() { return backpack0; }
    public void setBackpack0(int backpack0) { this.backpack0 = backpack0; }

    public int getBackpack1() { return backpack1; }
    public void setBackpack1(int backpack1) { this.backpack1 = backpack1; }

    public int getBackpack2() { return backpack2; }
    public void setBackpack2(int backpack2) { this.backpack2 = backpack2; }

    public int getItemNeutral() { return itemNeutral; }
    public void setItemNeutral(int itemNeutral) { this.itemNeutral = itemNeutral; }

    public List<Integer> getGoldT() { return goldT; }
    public void setGoldT(List<Integer> goldT) { this.goldT = goldT; }

    public List<Integer> getLastHitsT() { return lastHitsT; }
    public void setLastHitsT(List<Integer> lastHitsT) { this.lastHitsT = lastHitsT; }

    public List<Integer> getDeniesT() { return deniesT; }
    public void setDeniesT(List<Integer> deniesT) { this.deniesT = deniesT; }

    public List<Integer> getXpT() { return xpT; }
    public void setXpT(List<Integer> xpT) { this.xpT = xpT; }

    public List<PurchaseLogEntry> getPurchaseLog() { return purchaseLog; }
    public void setPurchaseLog(List<PurchaseLogEntry> purchaseLog) { this.purchaseLog = purchaseLog; }

    public List<KillLogEntry> getKillsLog() { return killsLog; }
    public void setKillsLog(List<KillLogEntry> killsLog) { this.killsLog = killsLog; }
}
