using System.Text.Json.Serialization;

namespace Dota2Analyzer.Core.Models.OpenDota;

public sealed record RecentMatch
{
    [JsonPropertyName("match_id")] public long MatchId { get; init; }
    [JsonPropertyName("player_slot")] public int PlayerSlot { get; init; }
    [JsonPropertyName("radiant_win")] public bool RadiantWin { get; init; }
    [JsonPropertyName("duration")] public int Duration { get; init; }
    [JsonPropertyName("hero_id")] public int HeroId { get; init; }
    [JsonPropertyName("kills")] public int Kills { get; init; }
    [JsonPropertyName("deaths")] public int Deaths { get; init; }
    [JsonPropertyName("assists")] public int Assists { get; init; }
    [JsonPropertyName("last_hits")] public int LastHits { get; init; }
    [JsonPropertyName("denies")] public int Denies { get; init; }
    [JsonPropertyName("gold_per_min")] public int GoldPerMin { get; init; }
    [JsonPropertyName("xp_per_min")] public int XpPerMin { get; init; }
    [JsonPropertyName("hero_damage")] public int HeroDamage { get; init; }
    [JsonPropertyName("tower_damage")] public int TowerDamage { get; init; }
    [JsonPropertyName("hero_healing")] public int HeroHealing { get; init; }
    [JsonPropertyName("level")] public int Level { get; init; }
}
