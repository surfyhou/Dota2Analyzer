namespace Dota2Analyzer.Api.Models;

public sealed record RecentMatchDto
{
    public required long MatchId { get; init; }
    public required string HeroName { get; init; }
    public required string ResultText { get; init; }
    public required bool Won { get; init; }
    public required string Kda { get; init; }
    public required string GpmXpm { get; init; }
    public required int DurationMinutes { get; init; }
}
