using Dota2Analyzer.Core.Models.OpenDota;
using Microsoft.Extensions.Logging;

namespace Dota2Analyzer.Core.Services;

public sealed class HeroDataCache
{
    private readonly OpenDotaClient _client;
    private readonly MatchCache _cache;
    private readonly ILogger<HeroDataCache> _logger;
    private readonly bool _cacheOnly;
    private readonly SemaphoreSlim _gate = new(1, 1);
    private Dictionary<int, string> _heroNames = [];
    private Dictionary<int, string> _heroKeys = [];
    private Dictionary<int, double> _heroWinRates = [];
    private Dictionary<string, ItemConstants> _itemConstants = new(StringComparer.OrdinalIgnoreCase);
    private Dictionary<int, string> _itemIdToKey = [];
    private bool _loaded;

    public HeroDataCache(OpenDotaClient client, MatchCache cache, ILogger<HeroDataCache> logger, bool cacheOnly)
    {
        _client = client;
        _cache = cache;
        _logger = logger;
        _cacheOnly = cacheOnly;
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
                if (_cacheOnly)
                {
                    _logger.LogWarning("Hero cache empty and cache-only enabled. Skipping OpenDota.");
                    heroes = [];
                }
                else
                {
                    _logger.LogInformation("Hero cache empty, fetching from OpenDota");
                    heroes = await _client.GetHeroesAsync(cancellationToken);
                    if (heroes.Count > 0)
                    {
                        await _cache.SaveHeroesAsync(heroes, cancellationToken);
                    }
                }
            }
            else if (heroes.Any(h => string.IsNullOrWhiteSpace(h.Name)))
            {
                if (_cacheOnly)
                {
                    _logger.LogWarning("Hero cache missing key names and cache-only enabled. Hero image lookup may fail.");
                }
                else
                {
                    _logger.LogInformation("Hero cache missing key names, refreshing from OpenDota");
                    var refreshed = await _client.GetHeroesAsync(cancellationToken);
                    if (refreshed.Count > 0)
                    {
                        heroes = refreshed;
                        await _cache.SaveHeroesAsync(heroes, cancellationToken);
                    }
                }
            }

            _heroNames = heroes.ToDictionary(h => h.Id, h => h.LocalizedName);
            _heroKeys = heroes.ToDictionary(h => h.Id, h => h.Name);

            var stats = await _cache.GetHeroStatsAsync(TimeSpan.FromDays(30), cancellationToken);
            if (stats is null || stats.Count == 0)
            {
                if (_cacheOnly)
                {
                    _logger.LogWarning("Hero stats cache empty and cache-only enabled. Skipping OpenDota.");
                    stats = [];
                }
                else
                {
                    _logger.LogInformation("Hero stats cache empty, fetching from OpenDota");
                    stats = await _client.GetHeroStatsAsync(cancellationToken);
                    if (stats.Count > 0)
                    {
                        await _cache.SaveHeroStatsAsync(stats, cancellationToken);
                    }
                }
            }

            stats ??= [];

            _heroWinRates = stats
                .Where(s => s.ProPick > 0)
                .ToDictionary(s => s.Id, s => (double)s.ProWin / s.ProPick);

            var items = await _cache.GetItemConstantsAsync(TimeSpan.FromDays(30), cancellationToken);
            if (items is null || items.Count == 0)
            {
                if (_cacheOnly)
                {
                    _logger.LogWarning("Item constants cache empty and cache-only enabled. Skipping OpenDota.");
                    items = new Dictionary<string, ItemConstants>(StringComparer.OrdinalIgnoreCase);
                }
                else
                {
                    _logger.LogInformation("Item constants cache empty, fetching from OpenDota");
                    items = await _client.GetItemConstantsAsync(cancellationToken);
                    if (items.Count > 0)
                    {
                        await _cache.SaveItemConstantsAsync(items, cancellationToken);
                    }
                }
            }
            else if (items.Values.All(i => i?.Id is null))
            {
                if (_cacheOnly)
                {
                    _logger.LogWarning("Item constants cache missing ids and cache-only enabled. Item lookup by id may fail.");
                }
                else
                {
                    _logger.LogInformation("Item constants cache missing ids, refreshing from OpenDota");
                    var refreshedItems = await _client.GetItemConstantsAsync(cancellationToken);
                    if (refreshedItems.Count > 0)
                    {
                        items = refreshedItems;
                        await _cache.SaveItemConstantsAsync(items, cancellationToken);
                    }
                }
            }
            else if (items.Values.All(i => i?.Components is null))
            {
                if (_cacheOnly)
                {
                    _logger.LogWarning("Item constants cache missing components and cache-only enabled. Item compose removal may fail.");
                }
                else
                {
                    _logger.LogInformation("Item constants cache missing components, refreshing from OpenDota");
                    var refreshedItems = await _client.GetItemConstantsAsync(cancellationToken);
                    if (refreshedItems.Count > 0)
                    {
                        items = refreshedItems;
                        await _cache.SaveItemConstantsAsync(items, cancellationToken);
                    }
                }
            }

            items ??= new Dictionary<string, ItemConstants>(StringComparer.OrdinalIgnoreCase);
            _itemConstants = new Dictionary<string, ItemConstants>(items, StringComparer.OrdinalIgnoreCase);
            _itemIdToKey = _itemConstants
                .Select(kvp => (Key: kvp.Key, Id: kvp.Value?.Id))
                .Where(kvp => kvp.Id is > 0 && !string.IsNullOrWhiteSpace(kvp.Key))
                .GroupBy(kvp => kvp.Id!.Value)
                .ToDictionary(g => g.Key, g => g.First().Key!);

            _loaded = true;
            _logger.LogInformation("Hero data loaded. Heroes={HeroCount} Stats={StatCount} Items={ItemCount}", _heroNames.Count, _heroWinRates.Count, _itemConstants.Count);
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

    public string GetHeroKey(int heroId)
    {
        if (_heroKeys.TryGetValue(heroId, out var key) && !string.IsNullOrWhiteSpace(key))
        {
            return key;
        }
        return string.Empty;
    }

    public bool TryGetWinRate(int heroId, out double winRate)
    {
        return _heroWinRates.TryGetValue(heroId, out winRate);
    }

    public bool TryGetItemConstants(string key, out ItemConstants? constants)
    {
        return _itemConstants.TryGetValue(key, out constants);
    }

    public bool TryGetItemKeyById(int itemId, out string key)
    {
        if (itemId <= 0)
        {
            key = string.Empty;
            return false;
        }

        if (_itemIdToKey.TryGetValue(itemId, out var resolved) && !string.IsNullOrWhiteSpace(resolved))
        {
            key = resolved;
            return true;
        }

        key = string.Empty;
        return false;
    }
}
