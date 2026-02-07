using System.Text.Json.Serialization;

namespace Dota2Analyzer.Core.Models.OpenDota;

public sealed record Hero
{
    [JsonPropertyName("id")] public int Id { get; init; }
    [JsonPropertyName("localized_name")] public string LocalizedName { get; init; } = string.Empty;
}

public sealed record HeroStats
{
    [JsonPropertyName("id")] public int Id { get; init; }
    [JsonPropertyName("pro_win")] public int ProWin { get; init; }
    [JsonPropertyName("pro_pick")] public int ProPick { get; init; }
}
