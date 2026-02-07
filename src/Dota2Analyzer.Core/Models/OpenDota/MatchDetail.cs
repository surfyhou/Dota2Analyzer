using System.Text.Json.Serialization;

namespace Dota2Analyzer.Core.Models.OpenDota;

public sealed record MatchDetail
{
    [JsonPropertyName("match_id")] public long MatchId { get; init; }
    [JsonPropertyName("duration")] public int Duration { get; init; }
    [JsonPropertyName("radiant_win")] public bool RadiantWin { get; init; }
    [JsonPropertyName("players")] public List<PlayerDetail>? Players { get; init; }
    [JsonPropertyName("picks_bans")] public List<PickBan>? PicksBans { get; init; }
}

public sealed record PlayerDetail
{
    [JsonPropertyName("account_id")] public int? AccountId { get; init; }
    [JsonPropertyName("player_slot")] public int PlayerSlot { get; init; }
    [JsonPropertyName("hero_id")] public int HeroId { get; init; }
    [JsonPropertyName("kills")] public int Kills { get; init; }
    [JsonPropertyName("deaths")] public int Deaths { get; init; }
    [JsonPropertyName("assists")] public int Assists { get; init; }
    [JsonPropertyName("last_hits")] public int LastHits { get; init; }
    [JsonPropertyName("denies")] public int Denies { get; init; }
    [JsonPropertyName("gold_per_min")] public int GoldPerMin { get; init; }
    [JsonPropertyName("xp_per_min")] public int XpPerMin { get; init; }
    [JsonPropertyName("level")] public int Level { get; init; }
    [JsonPropertyName("lane")] public int Lane { get; init; }
    [JsonPropertyName("lane_role")] public int? LaneRole { get; init; }
    [JsonPropertyName("hero_damage")] public int HeroDamage { get; init; }
    [JsonPropertyName("tower_damage")] public int TowerDamage { get; init; }
    [JsonPropertyName("item_0")] public int Item0 { get; init; }
    [JsonPropertyName("item_1")] public int Item1 { get; init; }
    [JsonPropertyName("item_2")] public int Item2 { get; init; }
    [JsonPropertyName("item_3")] public int Item3 { get; init; }
    [JsonPropertyName("item_4")] public int Item4 { get; init; }
    [JsonPropertyName("item_5")] public int Item5 { get; init; }
    [JsonPropertyName("backpack_0")] public int Backpack0 { get; init; }
    [JsonPropertyName("backpack_1")] public int Backpack1 { get; init; }
    [JsonPropertyName("backpack_2")] public int Backpack2 { get; init; }
    [JsonPropertyName("item_neutral")] public int ItemNeutral { get; init; }
    [JsonPropertyName("gold_t")] public List<int>? GoldT { get; init; }
    [JsonPropertyName("lh_t")] public List<int>? LastHitsT { get; init; }
    [JsonPropertyName("dn_t")] public List<int>? DeniesT { get; init; }
    [JsonPropertyName("xp_t")] public List<int>? XpT { get; init; }
    [JsonPropertyName("purchase_log")] public List<PurchaseLogEntry>? PurchaseLog { get; init; }
}

public sealed record PickBan
{
    [JsonPropertyName("is_pick")] public bool IsPick { get; init; }
    [JsonPropertyName("hero_id")] public int HeroId { get; init; }
    [JsonPropertyName("team")] public int Team { get; init; }
    [JsonPropertyName("order")] public int Order { get; init; }
}
