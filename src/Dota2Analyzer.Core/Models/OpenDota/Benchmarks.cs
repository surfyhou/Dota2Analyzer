using System.Text.Json.Serialization;

namespace Dota2Analyzer.Core.Models.OpenDota;

public sealed record BenchmarksResponse
{
    [JsonPropertyName("hero_id")] public int HeroId { get; init; }
    [JsonPropertyName("result")] public Dictionary<string, List<BenchmarkEntry>> Result { get; init; } = [];
}

public sealed record BenchmarkEntry
{
    [JsonPropertyName("percentile")] public double Percentile { get; init; }
    [JsonPropertyName("value")] public double Value { get; init; }
}
