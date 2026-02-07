using Dota2Analyzer.Core.Analysis;
using Dota2Analyzer.Core.Models.Analysis;
using Xunit;

namespace Dota2Analyzer.Tests;

public sealed class MatchSelectionTests
{
    [Fact]
    public void OnlyPos1_ReturnsDesiredCountWhenEnough()
    {
        var analyses = Enumerable.Range(1, 30)
            .Select(i => new MatchAnalysisResult
            {
                MatchId = i,
                HeroName = "Test",
                Won = true,
                ResultText = "胜利",
                LaneRole = 1,
                IsPosition1 = i <= 25,
                PickRound = "第1轮",
                PickIndex = 1,
                LaneResult = "",
                LaneNetWorthDiff10 = 0,
                LaneOpponentHero = "",
                LaningDetails = [],
                BenchmarkNotes = [],
                PerformanceRating = "",
                Mistakes = [],
                Suggestions = [],
                Statistics = new Dictionary<string, string>(),
                AllyHeroes = [],
                EnemyHeroes = [],
                InventoryTimeline = []
            })
            .ToList();

        var result = MatchSelection.SelectDesired(analyses, 20, true);
        Assert.Equal(20, result.Count);
        Assert.All(result, r => Assert.True(r.IsPosition1));
    }

    [Fact]
    public void OnlyPos1_ReturnsAllWhenInsufficient()
    {
        var analyses = Enumerable.Range(1, 10)
            .Select(i => new MatchAnalysisResult
            {
                MatchId = i,
                HeroName = "Test",
                Won = true,
                ResultText = "胜利",
                LaneRole = 1,
                IsPosition1 = i <= 8,
                PickRound = "第1轮",
                PickIndex = 1,
                LaneResult = "",
                LaneNetWorthDiff10 = 0,
                LaneOpponentHero = "",
                LaningDetails = [],
                BenchmarkNotes = [],
                PerformanceRating = "",
                Mistakes = [],
                Suggestions = [],
                Statistics = new Dictionary<string, string>(),
                AllyHeroes = [],
                EnemyHeroes = [],
                InventoryTimeline = []
            })
            .ToList();

        var result = MatchSelection.SelectDesired(analyses, 20, true);
        Assert.Equal(8, result.Count);
        Assert.All(result, r => Assert.True(r.IsPosition1));
    }
}
