namespace Dota2Analyzer.Core.Analysis;

internal static class PositionClassifier
{
    public static bool IsPosition1(int laneRole, int playerSlot, int durationSeconds, int goldPerMin, int lastHits, int accountId, IEnumerable<(int AccountId, int PlayerSlot, int GoldPerMin, int LastHits)> team)
    {
        if (laneRole is 4 or 5) return false;

        var teamList = team.Where(p => (p.PlayerSlot < 128) == (playerSlot < 128)).ToList();
        var gpmRank = teamList.OrderByDescending(p => p.GoldPerMin)
            .Select((p, i) => new { p.AccountId, Index = i })
            .FirstOrDefault(x => x.AccountId == accountId)?.Index ?? int.MaxValue;

        var lhRank = teamList.OrderByDescending(p => p.LastHits)
            .Select((p, i) => new { p.AccountId, Index = i })
            .FirstOrDefault(x => x.AccountId == accountId)?.Index ?? int.MaxValue;

        var minutes = Math.Max(1, durationSeconds / 60);
        var csPerMin = lastHits / Math.Max(1.0, minutes);

        var supportLike = goldPerMin < 380 && csPerMin < 3.0;
        if (supportLike) return false;

        if (gpmRank == 0 && lhRank == 0) return true;
        if (gpmRank == 0 && goldPerMin >= 480 && csPerMin >= 4.0) return true;
        if (gpmRank <= 1 && lhRank <= 1 && goldPerMin >= 450) return true;

        return false;
    }
}
