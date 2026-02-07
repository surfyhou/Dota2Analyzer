using Dota2Analyzer.Core.Analysis;
using Xunit;

namespace Dota2Analyzer.Tests;

public sealed class PositionClassifierTests
{
    [Fact]
    public void SupportLaneRole_IsNotPosition1()
    {
        var team = new (int AccountId, int PlayerSlot, int GoldPerMin, int LastHits)[]
        {
            (1, 0, 520, 220),
            (2, 1, 320, 40),
            (3, 2, 400, 90),
            (4, 3, 280, 20),
            (5, 4, 260, 10)
        };

        var result = PositionClassifier.IsPosition1(5, 4, 2400, 260, 10, 5, team);
        Assert.False(result);
    }

    [Fact]
    public void TopGpmAndLastHits_IsPosition1()
    {
        var team = new (int AccountId, int PlayerSlot, int GoldPerMin, int LastHits)[]
        {
            (10, 0, 650, 320),
            (11, 1, 420, 120),
            (12, 2, 380, 90),
            (13, 3, 300, 30),
            (14, 4, 280, 18)
        };

        var result = PositionClassifier.IsPosition1(3, 0, 2400, 650, 320, 10, team);
        Assert.True(result);
    }

    [Fact]
    public void SupportLikeStats_IsNotPosition1()
    {
        var team = new (int AccountId, int PlayerSlot, int GoldPerMin, int LastHits)[]
        {
            (21, 0, 500, 200),
            (22, 1, 420, 130),
            (23, 2, 360, 50),
            (24, 3, 280, 20),
            (25, 4, 260, 15)
        };

        var result = PositionClassifier.IsPosition1(1, 4, 2400, 260, 15, 25, team);
        Assert.False(result);
    }
}
