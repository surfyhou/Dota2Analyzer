using Dota2Analyzer.Core.Models.Analysis;
using Dota2Analyzer.Core.Models.OpenDota;
using Dota2Analyzer.Core.Services;
using Microsoft.Extensions.Logging;

namespace Dota2Analyzer.Core.Analysis;

public sealed class MatchAnalyzer
{
    private readonly OpenDotaClient _client;
    private readonly HeroDataCache _heroData;
    private readonly MatchCache _cache;
    private readonly ILogger<MatchAnalyzer> _logger;

    private static readonly HashSet<string> DisableHeroNames = new(StringComparer.OrdinalIgnoreCase)
    {
        "Axe", "Bane", "Beastmaster", "Centaur Warrunner", "Chaos Knight", "Crystal Maiden",
        "Dark Seer", "Doom", "Dragon Knight", "Earth Spirit", "Earthshaker", "Elder Titan",
        "Enigma", "Faceless Void", "Grimstroke", "Invoker", "Kunkka", "Legion Commander",
        "Lion", "Magnus", "Mars", "Medusa", "Mirana", "Nyx Assassin", "Ogre Magi",
        "Pudge", "Puck", "Riki", "Sand King", "Shadow Shaman", "Slardar", "Snapfire",
        "Spirit Breaker", "Storm Spirit", "Sven", "Tidehunter", "Tiny", "Treant Protector",
        "Tusk", "Underlord", "Vengeful Spirit", "Warlock", "Windranger", "Winter Wyvern",
        "Witch Doctor", "Zeus", "Primal Beast", "Ringmaster", "Marci", "Muerta"
    };

    public MatchAnalyzer(OpenDotaClient client, HeroDataCache heroData, MatchCache cache, ILogger<MatchAnalyzer> logger)
    {
        _client = client;
        _heroData = heroData;
        _cache = cache;
        _logger = logger;
    }

    public async Task<List<MatchAnalysisResult>> AnalyzeRecentAsync(long accountId, int limit, bool requestParse, bool onlyPos1, CancellationToken cancellationToken)
    {
        await _heroData.EnsureLoadedAsync(cancellationToken);
        var matches = await _cache.GetRecentMatchesAsync(accountId, TimeSpan.FromMinutes(30), cancellationToken);
        if (matches is null || matches.Count == 0)
        {
            _logger.LogInformation("Recent matches cache miss for {AccountId}, fetching from OpenDota", accountId);
            matches = await _client.GetRecentMatchesAsync(accountId, limit, cancellationToken);
            if (matches.Count > 0)
            {
                await _cache.SaveRecentMatchesAsync(accountId, matches, cancellationToken);
            }
        }
        else
        {
            _logger.LogInformation("Recent matches cache hit for {AccountId} ({Count})", accountId, matches.Count);
        }

        var results = new List<MatchAnalysisResult>();
        foreach (var match in matches)
        {
            var result = await AnalyzeMatchAsync(match, accountId, requestParse, onlyPos1, cancellationToken);
            if (result is not null)
            {
                results.Add(result);
            }
            else
            {
                _logger.LogDebug("Match {MatchId} filtered out (onlyPos1={OnlyPos1})", match.MatchId, onlyPos1);
            }
        }

        return results;
    }

    public async Task<MatchAnalysisResult?> AnalyzeMatchAsync(RecentMatch match, long accountId, bool requestParse, bool onlyPos1, CancellationToken cancellationToken)
    {
        await _heroData.EnsureLoadedAsync(cancellationToken);
        if (requestParse)
        {
            _ = await _client.RequestParseAsync(match.MatchId, cancellationToken);
        }

        var detail = await _cache.GetMatchDetailAsync(match.MatchId, TimeSpan.FromDays(7), cancellationToken);
        if (detail is null)
        {
            _logger.LogInformation("Match cache miss for {MatchId}, fetching from OpenDota", match.MatchId);
            detail = await _client.GetMatchDetailAsync(match.MatchId, cancellationToken);
            if (detail is not null)
            {
                await _cache.SaveMatchDetailAsync(match.MatchId, detail, cancellationToken);
            }
        }
        else
        {
            _logger.LogInformation("Match cache hit for {MatchId}", match.MatchId);
        }
        if (detail?.Players is null || detail.Players.Count == 0)
        {
            _logger.LogWarning("Match detail missing or unparsed for {MatchId}", match.MatchId);
            return BuildUnparsedResult(match);
        }

        var accountId32 = unchecked((int)accountId);
        var player = detail.Players.FirstOrDefault(p => p.AccountId == accountId32);
        if (player is null)
        {
            _logger.LogWarning("Player {AccountId} not found in match {MatchId}", accountId, match.MatchId);
            return BuildUnparsedResult(match);
        }

        if (onlyPos1 && !IsLikelyPosition1(player, detail.Players))
        {
            return null;
        }

        var isRadiant = match.PlayerSlot < 128;
        var won = (isRadiant && match.RadiantWin) || (!isRadiant && !match.RadiantWin);

        var (pickRound, pickIndex) = AnalyzePickRound(detail, player, isRadiant);
        var (laneResult, laneDiff, laneOpponentHero, laningDetails, benchmarkNotes, laneContext) = AnalyzeLaning(player, detail, isRadiant);
        var heroBenchmarks = await _client.GetHeroBenchmarksAsync(player.HeroId, cancellationToken);
        var heroBenchmarkNotes = BuildHeroBenchmarkNotes(match, heroBenchmarks);
        benchmarkNotes.AddRange(heroBenchmarkNotes);
        var performance = EvaluatePerformance(match);

        var allyHeroes = detail.Players
            .Where(p => (p.PlayerSlot < 128) == isRadiant)
            .Select(p => _heroData.GetHeroName(p.HeroId))
            .ToList();
        var enemyHeroes = detail.Players
            .Where(p => (p.PlayerSlot < 128) != isRadiant)
            .Select(p => _heroData.GetHeroName(p.HeroId))
            .ToList();

        var (mistakes, suggestions) = DetectMistakes(match, player, enemyHeroes, laneDiff, laneContext);

        return new MatchAnalysisResult
        {
            MatchId = match.MatchId,
            HeroName = _heroData.GetHeroName(match.HeroId),
            Won = won,
            ResultText = won ? "胜利" : "失败",
            PickRound = pickRound,
            PickIndex = pickIndex,
            LaneResult = laneResult,
            LaneNetWorthDiff10 = laneDiff,
            LaneOpponentHero = laneOpponentHero,
            LaningDetails = laningDetails,
            BenchmarkNotes = benchmarkNotes,
            PerformanceRating = performance,
            Mistakes = mistakes,
            Suggestions = suggestions,
            Statistics = new Dictionary<string, string>
            {
                ["KDA"] = $"{match.Kills}/{match.Deaths}/{match.Assists}",
                ["补刀/反补"] = $"{match.LastHits}/{match.Denies}",
                ["GPM/XPM"] = $"{match.GoldPerMin}/{match.XpPerMin}",
                ["时长"] = $"{match.Duration / 60}分钟",
                ["等级"] = match.Level.ToString()
            },
            AllyHeroes = allyHeroes,
            EnemyHeroes = enemyHeroes
        };
    }

    private MatchAnalysisResult BuildUnparsedResult(RecentMatch match)
    {
        var won = (match.PlayerSlot < 128 && match.RadiantWin) || (match.PlayerSlot >= 128 && !match.RadiantWin);
        return new MatchAnalysisResult
        {
            MatchId = match.MatchId,
            HeroName = _heroData.GetHeroName(match.HeroId),
            Won = won,
            ResultText = won ? "胜利" : "失败",
            PickRound = "未知",
            PickIndex = -1,
            LaneResult = "对局尚未解析",
            LaneNetWorthDiff10 = 0,
            LaneOpponentHero = "未知",
            LaningDetails = ["对局尚未解析，无法生成对线期细节"],
            BenchmarkNotes = ["对局尚未解析，无法生成基准对比"],
            PerformanceRating = EvaluatePerformance(match),
            Mistakes = ["对局尚未解析，建议稍后重试"],
            Suggestions = ["OpenDota 需要时间解析回放文件"],
            Statistics = new Dictionary<string, string>
            {
                ["KDA"] = $"{match.Kills}/{match.Deaths}/{match.Assists}",
                ["补刀/反补"] = $"{match.LastHits}/{match.Denies}",
                ["GPM/XPM"] = $"{match.GoldPerMin}/{match.XpPerMin}",
                ["时长"] = $"{match.Duration / 60}分钟",
                ["等级"] = match.Level.ToString()
            },
            AllyHeroes = [],
            EnemyHeroes = []
        };
    }

    private (string Round, int PickIndex) AnalyzePickRound(MatchDetail detail, PlayerDetail player, bool isRadiant)
    {
        if (detail.PicksBans is null || detail.PicksBans.Count == 0)
        {
            return ("未知", -1);
        }

        var picks = detail.PicksBans
            .Where(p => p.IsPick)
            .OrderBy(p => p.Order)
            .ToList();

        var team = isRadiant ? 0 : 1;
        var pickIndex = picks.FindIndex(p => p.HeroId == player.HeroId && p.Team == team);
        if (pickIndex < 0)
        {
            return ("未知", -1);
        }

        var round = pickIndex switch
        {
            <= 1 => "第1轮",
            <= 5 => "第2轮",
            _ => "第3轮"
        };

        return (round, pickIndex + 1);
    }

    private (string Result, int NetWorthDiff, string LaneOpponentHero, List<string> LaningDetails, List<string> BenchmarkNotes, LaningContext Context) AnalyzeLaning(PlayerDetail player, MatchDetail detail, bool isRadiant)
    {
        var enemy = FindLaneOpponent(player, detail, isRadiant);
        var playerNetworth10 = GetNetWorthAt(player, 10);
        var enemyNetworth10 = enemy is null ? 0 : GetNetWorthAt(enemy, 10);
        var diff = playerNetworth10 - enemyNetworth10;

        var result = diff switch
        {
            >= 700 => $"线优 (10分钟净值差 +{diff})",
            <= -700 => $"线劣 (10分钟净值差 {diff})",
            _ => $"均势 (10分钟净值差 {diff})"
        };

        var laneOpponentHero = enemy is null ? "未知" : _heroData.GetHeroName(enemy.HeroId);
        var laningDetails = new List<string>();
        var benchmarkNotes = new List<string>();
        var context = BuildLaningContext(player, enemy, detail);

        laningDetails.Add($"对位英雄：{laneOpponentHero}");
        laningDetails.Add($"5分钟：净值差 {FormatDiff(context.NetWorthDiff5)}，补刀差 {FormatDiff(context.LastHitsDiff5)}，经验差 {FormatDiff(context.XpDiff5)}");
        laningDetails.Add($"10分钟：净值差 {FormatDiff(context.NetWorthDiff10)}，补刀差 {FormatDiff(context.LastHitsDiff10)}，经验差 {FormatDiff(context.XpDiff10)}");
        laningDetails.Add($"走势：{context.Trend}");

        var benchmark = GetRoleBenchmarks(player.LaneRole);
        if (benchmark is not null)
        {
            if (context.PlayerLastHits10 < benchmark.LastHits10Target)
            {
                benchmarkNotes.Add($"优秀玩家基准（估算）：10分钟补刀目标 {benchmark.LastHits10Target}，当前 {context.PlayerLastHits10}");
            }
            else
            {
                benchmarkNotes.Add($"优秀玩家基准（估算）：10分钟补刀目标 {benchmark.LastHits10Target}，当前已达标");
            }
        }

        return (result, diff, laneOpponentHero, laningDetails, benchmarkNotes, context);
    }

    private static PlayerDetail? FindLaneOpponent(PlayerDetail player, MatchDetail detail, bool isRadiant)
    {
        var enemies = detail.Players?.Where(p => (p.PlayerSlot < 128) != isRadiant).ToList();
        if (enemies is null || enemies.Count == 0)
        {
            return null;
        }

        if (player.LaneRole.HasValue)
        {
            var desiredRole = player.LaneRole.Value switch
            {
                1 => 3,
                3 => 1,
                2 => 2,
                _ => -1
            };
            if (desiredRole > 0)
            {
                var sameRole = enemies.Where(e => e.LaneRole == desiredRole).OrderByDescending(e => e.GoldPerMin).FirstOrDefault();
                if (sameRole is not null) return sameRole;
            }
        }

        if (player.Lane > 0)
        {
            var sameLane = enemies.Where(e => e.Lane == player.Lane).OrderByDescending(e => e.GoldPerMin).FirstOrDefault();
            if (sameLane is not null) return sameLane;
        }

        return enemies.OrderByDescending(e => e.GoldPerMin).FirstOrDefault();
    }

    private static int GetNetWorthAt(PlayerDetail player, int minute)
    {
        if (player.GoldT is { Count: > 0 })
        {
            var index = Math.Clamp(minute, 0, player.GoldT.Count - 1);
            return player.GoldT[index];
        }

        return (int)Math.Round((double)player.GoldPerMin * minute);
    }

    private static bool IsLikelyPosition1(PlayerDetail player, List<PlayerDetail> teamPlayers)
    {
        var team = teamPlayers.Where(p => (p.PlayerSlot < 128) == (player.PlayerSlot < 128)).ToList();
        var topGpm = team.OrderByDescending(p => p.GoldPerMin).FirstOrDefault();
        var isTopFarm = topGpm is not null && topGpm.AccountId == player.AccountId;

        return player.LaneRole == 1 || (isTopFarm && player.LaneRole != 2);
    }

    private static string EvaluatePerformance(RecentMatch match)
    {
        var kda = match.Deaths > 0 ? (double)(match.Kills + match.Assists) / match.Deaths : match.Kills + match.Assists;
        var minutes = Math.Max(1, match.Duration / 60);
        var csPerMin = (double)match.LastHits / minutes;

        var score = 0;
        if (kda >= 3) score += 2;
        else if (kda >= 2) score += 1;

        if (csPerMin >= 6) score += 2;
        else if (csPerMin >= 4) score += 1;

        if (match.HeroDamage > match.Duration * 350) score += 1;

        return score switch
        {
            >= 4 => "⭐⭐⭐ 出色表现",
            >= 2 => "⭐⭐ 良好表现",
            _ => "⭐ 需要改进"
        };
    }

    private (List<string> Mistakes, List<string> Suggestions) DetectMistakes(RecentMatch match, PlayerDetail player, List<string> enemyHeroes, int laneDiff, LaningContext laneContext)
    {
        var mistakes = new List<string>();
        var suggestions = new List<string>();

        var minutes = Math.Max(1, match.Duration / 60);
        var csPerMin = (double)match.LastHits / minutes;
        var isSupport = IsSupportLike(player, match);

        if (laneDiff <= -700 && match.GoldPerMin < 450)
        {
            mistakes.Add("对线劣势后经济持续落后");
            suggestions.Add("10分钟后更早换线/打野/推对面优势路，避免死守己方优势路");
        }

        if (laneContext.NetWorthDiff5 <= -350 && laneContext.NetWorthDiff10 <= -700)
        {
            mistakes.Add($"对线前5分钟被压制（5分钟净值差 {laneContext.NetWorthDiff5}）");
            suggestions.Add("开局补给与站位更保守，争取拉野或呼叫支援稳住线权");
        }

        if (laneContext.NetWorthDiff5 >= 400 && laneContext.NetWorthDiff10 <= -300)
        {
            mistakes.Add("5-10分钟对线优势被反超");
            suggestions.Add("领先时减少冒进，注意敌方支援与TP反打");
        }

        if (laneContext.LastHitsDiff10 <= -8 && laneContext.XpDiff10 <= -400)
        {
            mistakes.Add("对线期补刀与经验同步落后");
            suggestions.Add("优先保障补刀与经验线，必要时通过拉野与换线止损");
        }

        if (match.Deaths >= 6 && csPerMin < 4)
        {
            mistakes.Add("阵亡过多导致关键发育期断档");
            suggestions.Add("优先保命与稳定打钱，再寻找安全参战窗口");
        }

        if (match.HeroDamage < match.Duration * 300)
        {
            mistakes.Add("参团输出偏低");
            suggestions.Add("中期关注关键团战时机，避免长时间脱节");
        }

        if (ShouldFlagLowPushContribution(match, player, isSupport))
        {
            mistakes.Add("推进贡献不足");
            suggestions.Add("中后期主动推动线权和塔压力，扩大优势或扳回节奏");
        }

        var bkbTime = GetItemPurchaseTime(player, "black_king_bar");
        var disableCount = enemyHeroes.Count(h => DisableHeroNames.Contains(h));
        if (bkbTime is not null && bkbTime > 1500 && disableCount >= 2)
        {
            mistakes.Add($"BKB 出得偏晚（{bkbTime.Value / 60}分）");
            suggestions.Add("对面控制较多时尽量提前做出 BKB");
        }

        if (mistakes.Count == 0)
        {
            mistakes.Add("暂无明显关键错误");
            suggestions.Add("继续保持当前节奏与决策");
        }

        return (mistakes, suggestions);
    }

    private sealed record LaningContext(
        int NetWorthDiff5,
        int NetWorthDiff10,
        int LastHitsDiff5,
        int LastHitsDiff10,
        int XpDiff5,
        int XpDiff10,
        int PlayerLastHits10,
        string Trend
    );

    private LaningContext BuildLaningContext(PlayerDetail player, PlayerDetail? enemy, MatchDetail detail)
    {
        if (enemy is null)
        {
            return new LaningContext(0, 0, 0, 0, 0, 0, GetLastHitsAt(player, 10, detail.Duration), "未知");
        }

        var net5 = GetNetWorthAt(player, 5) - GetNetWorthAt(enemy, 5);
        var net10 = GetNetWorthAt(player, 10) - GetNetWorthAt(enemy, 10);
        var lh5 = GetLastHitsAt(player, 5, detail.Duration) - GetLastHitsAt(enemy, 5, detail.Duration);
        var lh10 = GetLastHitsAt(player, 10, detail.Duration) - GetLastHitsAt(enemy, 10, detail.Duration);
        var xp5 = GetXpAt(player, 5, detail.Duration) - GetXpAt(enemy, 5, detail.Duration);
        var xp10 = GetXpAt(player, 10, detail.Duration) - GetXpAt(enemy, 10, detail.Duration);

        var trend = DescribeLaningTrend(net5, net10);
        var playerLh10 = GetLastHitsAt(player, 10, detail.Duration);

        return new LaningContext(net5, net10, lh5, lh10, xp5, xp10, playerLh10, trend);
    }

    private static int GetLastHitsAt(PlayerDetail player, int minute, int durationSeconds)
    {
        if (player.LastHitsT is { Count: > 0 })
        {
            var index = Math.Clamp(minute, 0, player.LastHitsT.Count - 1);
            return player.LastHitsT[index];
        }

        var minutes = Math.Max(1, durationSeconds / 60);
        return (int)Math.Round((double)player.LastHits / minutes * minute);
    }

    private static int GetXpAt(PlayerDetail player, int minute, int durationSeconds)
    {
        if (player.XpT is { Count: > 0 })
        {
            var index = Math.Clamp(minute, 0, player.XpT.Count - 1);
            return player.XpT[index];
        }

        var minutes = Math.Max(1, durationSeconds / 60);
        return (int)Math.Round((double)player.XpPerMin * minute);
    }

    private static string DescribeLaningTrend(int netDiff5, int netDiff10)
    {
        if (netDiff5 >= 400 && netDiff10 <= -300) return "5-10分钟被反超";
        if (netDiff5 <= -400 && netDiff10 >= 200) return "5-10分钟回稳/反超";
        if (netDiff10 >= 700) return "持续线优";
        if (netDiff10 <= -700) return "持续线劣";
        return "基本均势";
    }

    private static string FormatDiff(int value)
    {
        return value >= 0 ? $"+{value}" : value.ToString();
    }

    private sealed record RoleBenchmark(int LastHits10Target);

    private static RoleBenchmark? GetRoleBenchmarks(int? laneRole)
    {
        return laneRole switch
        {
            1 => new RoleBenchmark(45),
            2 => new RoleBenchmark(50),
            3 => new RoleBenchmark(35),
            4 => new RoleBenchmark(15),
            5 => new RoleBenchmark(15),
            _ => null
        };
    }

    private static bool IsSupportLike(PlayerDetail player, RecentMatch match)
    {
        if (player.LaneRole is 4 or 5) return true;

        var minutes = Math.Max(1, match.Duration / 60);
        var csPerMin = (double)match.LastHits / minutes;
        var total = match.Kills + match.Assists;
        var assistShare = total == 0 ? 0 : (double)match.Assists / total;

        return match.GoldPerMin < 420 && csPerMin < 3.5 && assistShare >= 0.6;
    }

    private static bool ShouldFlagLowPushContribution(RecentMatch match, PlayerDetail player, bool isSupport)
    {
        if (match.Duration < 18 * 60) return false;

        var towerDamageLow = match.TowerDamage < match.Duration * 25;
        if (!towerDamageLow) return false;

        if (!isSupport) return true;

        var minutes = Math.Max(1, match.Duration / 60);
        var heroDamageLow = match.HeroDamage < match.Duration * 250;
        var assistsLow = match.Assists < Math.Max(6, minutes / 2);

        return heroDamageLow && assistsLow && match.TowerDamage < match.Duration * 15;
    }

    private static List<string> BuildHeroBenchmarkNotes(RecentMatch match, BenchmarksResponse? benchmarks)
    {
        var notes = new List<string>();
        if (benchmarks?.Result is null || benchmarks.Result.Count == 0)
        {
            return notes;
        }

        var minutes = Math.Max(1, match.Duration / 60);
        var kpm = (double)match.Kills / minutes;
        var lhpm = (double)match.LastHits / minutes;
        var hdpm = (double)match.HeroDamage / minutes;
        var hhpm = (double)match.HeroHealing / minutes;

        var comparisons = new List<(string Label, double Value, string Key)>
        {
            ("GPM", match.GoldPerMin, "gold_per_min"),
            ("XPM", match.XpPerMin, "xp_per_min"),
            ("击杀/分钟", kpm, "kills_per_min"),
            ("补刀/分钟", lhpm, "last_hits_per_min"),
            ("输出/分钟", hdpm, "hero_damage_per_min"),
            ("治疗/分钟", hhpm, "hero_healing_per_min"),
            ("塔伤", match.TowerDamage, "tower_damage")
        };

        foreach (var (label, value, key) in comparisons)
        {
            if (!benchmarks.Result.TryGetValue(key, out var entries) || entries.Count == 0)
            {
                continue;
            }

            var percentile = EstimatePercentile(value, entries);
            var p80 = GetPercentileValue(entries, 0.8);
            var p50 = GetPercentileValue(entries, 0.5);

            var percentileText = percentile is null ? "未知" : $"{percentile.Value:F0}%";
            var p80Text = p80 is null ? "未知" : p80.Value.ToString("F0");
            var p50Text = p50 is null ? "未知" : p50.Value.ToString("F0");

            notes.Add($"{label}：{value:F0}（英雄分位约 {percentileText}，50%≈{p50Text}，优秀(80%)≈{p80Text}）");
        }

        return notes;
    }

    private static double? EstimatePercentile(double value, List<BenchmarkEntry> entries)
    {
        if (entries.Count == 0) return null;
        var sorted = entries.OrderBy(e => e.Value).ToList();
        BenchmarkEntry? match = null;
        foreach (var entry in sorted)
        {
            if (value >= entry.Value)
            {
                match = entry;
            }
            else
            {
                break;
            }
        }

        return match is null ? sorted.First().Percentile * 100 : match.Percentile * 100;
    }

    private static double? GetPercentileValue(List<BenchmarkEntry> entries, double percentile)
    {
        if (entries.Count == 0) return null;
        var match = entries.OrderBy(e => Math.Abs(e.Percentile - percentile)).FirstOrDefault();
        return match?.Value;
    }

    private static int? GetItemPurchaseTime(PlayerDetail player, string itemKey)
    {
        if (player.PurchaseLog is null) return null;

        var entry = player.PurchaseLog
            .Where(p => string.Equals(p.Key, itemKey, StringComparison.OrdinalIgnoreCase))
            .OrderBy(p => p.Time)
            .FirstOrDefault();

        return entry?.Time;
    }
}
