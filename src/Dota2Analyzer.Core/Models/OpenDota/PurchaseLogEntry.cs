using System.Text.Json.Serialization;

namespace Dota2Analyzer.Core.Models.OpenDota;

public sealed record PurchaseLogEntry
{
    [JsonPropertyName("time")] public int Time { get; init; }
    [JsonPropertyName("key")] public string Key { get; init; } = string.Empty;
}
