using Dota2Analyzer.Core.Models.OpenDota;
using Microsoft.Extensions.Logging;

namespace Dota2Analyzer.Core.Services;

public sealed class HeroDataCache
{
    private readonly OpenDotaClient _client;
    private readonly MatchCache _cache;
    private readonly ILogger<HeroDataCache> _logger;
    private readonly SemaphoreSlim _gate = new(1, 1);
    private Dictionary<int, string> _heroNames = [];
    private Dictionary<int, double> _heroWinRates = [];
    private bool _loaded;

    public HeroDataCache(OpenDotaClient client, MatchCache cache, ILogger<HeroDataCache> logger)
    {
        _client = client;
        _cache = cache;
        _logger = logger;
    }

    public async Task EnsureLoadedAsync(CancellationToken cancellationToken)
    {
        if (_loaded) return;

        await _gate.WaitAsync(cancellationToken);
        try
        {
            if (_loaded) return;

            var heroes = await _cache.GetHeroesAsync(TimeSpan.FromDays(30), cancellationToken);
            if (heroes is null || heroes.Count == 0)
            {
                _logger.LogInformation("Hero cache empty, fetching from OpenDota");
                heroes = await _client.GetHeroesAsync(cancellationToken);
                if (heroes.Count > 0)
                {
                    await _cache.SaveHeroesAsync(heroes, cancellationToken);
                }
            }

            _heroNames = heroes.ToDictionary(h => h.Id, h => h.LocalizedName);

            var stats = await _cache.GetHeroStatsAsync(TimeSpan.FromDays(30), cancellationToken);
            if (stats is null || stats.Count == 0)
            {
                _logger.LogInformation("Hero stats cache empty, fetching from OpenDota");
                stats = await _client.GetHeroStatsAsync(cancellationToken);
                if (stats.Count > 0)
                {
                    await _cache.SaveHeroStatsAsync(stats, cancellationToken);
                }
            }

            _heroWinRates = stats
                .Where(s => s.ProPick > 0)
                .ToDictionary(s => s.Id, s => (double)s.ProWin / s.ProPick);

            _loaded = true;
            _logger.LogInformation("Hero data loaded. Heroes={HeroCount} Stats={StatCount}", _heroNames.Count, _heroWinRates.Count);
        }
        finally
        {
            _gate.Release();
        }
    }

    public string GetHeroName(int heroId)
    {
        return _heroNames.TryGetValue(heroId, out var name) ? name : $"英雄{heroId}";
    }

    public bool TryGetWinRate(int heroId, out double winRate)
    {
        return _heroWinRates.TryGetValue(heroId, out winRate);
    }
}
