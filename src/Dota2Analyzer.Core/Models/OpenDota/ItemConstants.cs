using System.Text.Json.Serialization;

namespace Dota2Analyzer.Core.Models.OpenDota;

public sealed record ItemConstants
{
    [JsonPropertyName("id")] public int? Id { get; init; }
    [JsonPropertyName("img")] public string Img { get; init; } = string.Empty;
    [JsonPropertyName("dname")] public string DisplayName { get; init; } = string.Empty;
    [JsonPropertyName("cost")] public int? Cost { get; init; }
    [JsonPropertyName("qual")] public string Quality { get; init; } = string.Empty;
    [JsonPropertyName("components")] public List<string>? Components { get; init; }
}
