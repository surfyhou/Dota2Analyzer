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

public sealed record PurchaseLogEntry
{
    [JsonPropertyName("time")] public int Time { get; init; }
    [JsonPropertyName("key")] public string Key { get; init; } = string.Empty;
}
