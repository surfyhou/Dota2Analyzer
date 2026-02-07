using System.Collections.Concurrent;
using Dota2Analyzer.Core.Services;

namespace Dota2Analyzer.Api.Services;

public sealed class PreloadService
{
    private readonly OpenDotaClient _client;
    private readonly MatchCache _cache;
    private readonly HeroImageCache _heroImages;
    private readonly ILogger<PreloadService> _logger;
    private readonly ConcurrentDictionary<long, PreloadStatus> _status = new();
    private readonly SemaphoreSlim _gate = new(1, 1);

    public PreloadService(OpenDotaClient client, MatchCache cache, HeroImageCache heroImages, ILogger<PreloadService> logger)
    {
        _client = client;
        _cache = cache;
        _heroImages = heroImages;
        _logger = logger;
    }

    public PreloadStatus GetStatus(long accountId)
    {
        return _status.GetOrAdd(accountId, _ => new PreloadStatus { AccountId = accountId });
    }

    public async Task<PreloadStatus> StartAsync(long accountId, int count, CancellationToken cancellationToken)
    {
        await _gate.WaitAsync(cancellationToken);
        try
        {
            var status = _status.GetOrAdd(accountId, _ => new PreloadStatus { AccountId = accountId });
            if (status.IsRunning)
            {
                return status;
            }

            status.IsRunning = true;
            status.Total = count;
            status.Completed = 0;
            status.Failed = 0;
            status.LastUpdated = DateTimeOffset.UtcNow;
            status.Message = "正在拉取对局列表";
            status.Matches = [];

            _ = Task.Run(() => RunPreloadAsync(status, count, cancellationToken), CancellationToken.None);
            return status;
        }
        finally
        {
            _gate.Release();
        }
    }

    private async Task RunPreloadAsync(PreloadStatus status, int count, CancellationToken cancellationToken)
    {
        try
        {
            var matches = await FetchMatchesAsync(status.AccountId, count, cancellationToken);
            if (matches.Count > 0)
            {
                await _cache.SaveRecentMatchesAsync(status.AccountId, matches, cancellationToken);
            }
            status.Total = matches.Count;
            status.Message = "开始拉取对局详情";

            foreach (var match in matches)
            {
                try
                {
                    var cached = await _cache.GetMatchDetailAsync(match.MatchId, TimeSpan.FromDays(7), cancellationToken);
                    var detail = cached;
                    if (detail is null)
                    {
                        detail = await _client.GetMatchDetailAsync(match.MatchId, cancellationToken);
                        if (detail is not null)
                        {
                            await _cache.SaveMatchDetailAsync(match.MatchId, detail, cancellationToken);
                        }
                    }

                    if (detail?.Players is not null)
                    {
                        var radiant = detail.Players.Where(p => p.PlayerSlot < 128).Select(p => p.HeroId).ToList();
                        var dire = detail.Players.Where(p => p.PlayerSlot >= 128).Select(p => p.HeroId).ToList();

                        var row = new PreloadMatchRow
                        {
                            MatchId = match.MatchId,
                            RadiantWin = detail.RadiantWin,
                            Duration = detail.Duration,
                            StartTime = match.StartTime,
                            RadiantHeroes = radiant,
                            DireHeroes = dire
                        };
                        status.Matches.Add(row);

                        foreach (var heroId in radiant.Concat(dire))
                        {
                            _ = await _heroImages.EnsureHeroImageAsync(heroId, cancellationToken);
                        }
                    }

                    status.Completed++;
                }
                catch (Exception ex)
                {
                    status.Failed++;
                    _logger.LogWarning(ex, "Preload match failed {MatchId}", match.MatchId);
                }

                status.LastUpdated = DateTimeOffset.UtcNow;
                status.Message = $"已完成 {status.Completed}/{status.Total}";

                await Task.Delay(250, cancellationToken);
            }

            status.Message = "完成";
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Preload failed AccountId={AccountId}", status.AccountId);
            status.Message = "失败";
        }
        finally
        {
            status.IsRunning = false;
            status.LastUpdated = DateTimeOffset.UtcNow;
        }
    }

    private async Task<List<Dota2Analyzer.Core.Models.OpenDota.RecentMatch>> FetchMatchesAsync(long accountId, int count, CancellationToken cancellationToken)
    {
        var results = new List<Dota2Analyzer.Core.Models.OpenDota.RecentMatch>();
        var pageSize = 100;
        var offset = 0;

        while (results.Count < count)
        {
            var take = Math.Min(pageSize, count - results.Count);
            var batch = await _client.GetPlayerMatchesAsync(accountId, take, offset, 7, cancellationToken);
            if (batch.Count == 0) break;

            results.AddRange(batch);
            offset += batch.Count;
            if (batch.Count < take) break;
        }

        return results
            .GroupBy(m => m.MatchId)
            .Select(g => g.First())
            .OrderByDescending(m => m.StartTime)
            .ToList();
    }
}

public sealed class PreloadStatus
{
    public long AccountId { get; init; }
    public int Total { get; set; }
    public int Completed { get; set; }
    public int Failed { get; set; }
    public bool IsRunning { get; set; }
    public string Message { get; set; } = "未开始";
    public DateTimeOffset LastUpdated { get; set; } = DateTimeOffset.UtcNow;
    public List<PreloadMatchRow> Matches { get; set; } = [];
}

public sealed class PreloadMatchRow
{
    public long MatchId { get; init; }
    public bool RadiantWin { get; init; }
    public int Duration { get; init; }
    public int StartTime { get; init; }
    public List<int> RadiantHeroes { get; init; } = [];
    public List<int> DireHeroes { get; init; } = [];
}
