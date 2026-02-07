namespace Dota2Analyzer.Core.Models.Analysis;

public sealed record MatchAnalysisResult
{
    public required long MatchId { get; init; }
    public required string HeroName { get; init; }
    public required bool Won { get; init; }
    public required string ResultText { get; init; }
    public required string PickRound { get; init; }
    public required int PickIndex { get; init; }
    public required string LaneResult { get; init; }
    public required int LaneNetWorthDiff10 { get; init; }
    public required string LaneOpponentHero { get; init; }
    public required List<string> LaningDetails { get; init; }
    public required List<string> BenchmarkNotes { get; init; }
    public required string PerformanceRating { get; init; }
    public required List<string> Mistakes { get; init; }
    public required List<string> Suggestions { get; init; }
    public required Dictionary<string, string> Statistics { get; init; }
    public required List<string> AllyHeroes { get; init; }
    public required List<string> EnemyHeroes { get; init; }
}

public sealed record AnalysisSummary
{
    public required int TotalMatches { get; init; }
    public required int Wins { get; init; }
    public required double WinRate { get; init; }
    public required int ParsedMatches { get; init; }
    public required int UnparsedMatches { get; init; }
}

public sealed record AnalysisResponse
{
    public required AnalysisSummary Summary { get; init; }
    public required List<MatchAnalysisResult> Matches { get; init; }
}
