using Dota2Analyzer.Core.Models.Analysis;

namespace Dota2Analyzer.Core.Analysis;

internal static class MatchSelection
{
    public static List<MatchAnalysisResult> SelectDesired(List<MatchAnalysisResult> analyses, int desiredCount, bool onlyPos1)
    {
        if (desiredCount <= 0) return [];

        if (onlyPos1)
        {
            return analyses.Where(a => a.IsPosition1).Take(desiredCount).ToList();
        }

        return analyses.Take(desiredCount).ToList();
    }
}
