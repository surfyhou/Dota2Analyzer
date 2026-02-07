using Dota2Analyzer.Core.Services;

namespace Dota2Analyzer.Api.Services;

public sealed class HeroImageCache
{
    private readonly HeroDataCache _heroData;
    private readonly MatchCache _cache;
    private readonly OpenDotaClient _client;
    private readonly HttpClient _httpClient;
    private readonly string _heroImageDir;
    private readonly ILogger<HeroImageCache> _logger;
    private readonly string[] _baseUrls;

    public HeroImageCache(HeroDataCache heroData, MatchCache cache, OpenDotaClient client, ILogger<HeroImageCache> logger, IConfiguration config)
    {
        _heroData = heroData;
        _cache = cache;
        _client = client;
        _logger = logger;
        _httpClient = new HttpClient();
        _baseUrls = BuildBaseUrls(config);

        var basePath = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
        var folder = Path.Combine(basePath, "Dota2Analyzer", "assets", "heroes");
        Directory.CreateDirectory(folder);
        _heroImageDir = folder;
    }

    public async Task<string?> EnsureHeroImageAsync(int heroId, CancellationToken cancellationToken)
    {
        _logger.LogDebug("EnsureHeroImageAsync heroId={HeroId}", heroId);
        var key = await ResolveHeroKeyAsync(heroId, cancellationToken);
        if (string.IsNullOrWhiteSpace(key)) return null;

        var heroKey = key.Replace("npc_dota_hero_", string.Empty, StringComparison.OrdinalIgnoreCase);
        var fileName = $"{heroKey}_full.png";
        var localPath = Path.Combine(_heroImageDir, fileName);
        if (File.Exists(localPath))
        {
            _logger.LogDebug("Hero image cache hit {Path}", localPath);
            return localPath;
        }

        foreach (var baseUrl in _baseUrls)
        {
            var url = $"{baseUrl.TrimEnd('/')}/{heroKey}_full.png";
            _logger.LogInformation("Downloading hero image {Url}", url);
            try
            {
                using var response = await _httpClient.GetAsync(url, cancellationToken);
                if (!response.IsSuccessStatusCode)
                {
                    _logger.LogWarning("Hero image download failed {StatusCode} for {Url}", response.StatusCode, url);
                    continue;
                }

                await using var stream = await response.Content.ReadAsStreamAsync(cancellationToken);
                await using var fs = File.Create(localPath);
                await stream.CopyToAsync(fs, cancellationToken);
                _logger.LogInformation("Hero image saved {Path}", localPath);
                return localPath;
            }
            catch (HttpRequestException ex)
            {
                _logger.LogWarning(ex, "Hero image download failed for {Url}", url);
            }
        }

        return null;
    }

    public string GetHeroImagePath(int heroId)
    {
        var key = _heroData.GetHeroKey(heroId);
        if (string.IsNullOrWhiteSpace(key)) return string.Empty;
        var heroKey = key.Replace("npc_dota_hero_", string.Empty, StringComparison.OrdinalIgnoreCase);
        return Path.Combine(_heroImageDir, $"{heroKey}_full.png");
    }

    private async Task<string> ResolveHeroKeyAsync(int heroId, CancellationToken cancellationToken)
    {
        await _heroData.EnsureLoadedAsync(cancellationToken);
        var key = _heroData.GetHeroKey(heroId);
        if (!string.IsNullOrWhiteSpace(key))
        {
            _logger.LogDebug("Hero key from cache {HeroId}={Key}", heroId, key);
            return key;
        }

        var heroes = await _cache.GetHeroesAsync(TimeSpan.FromDays(30), cancellationToken);
        if (heroes is null || heroes.Count == 0)
        {
            heroes = await _client.GetHeroesAsync(cancellationToken);
            if (heroes.Count > 0)
            {
                await _cache.SaveHeroesAsync(heroes, cancellationToken);
            }
        }

        var resolved = heroes.FirstOrDefault(h => h.Id == heroId)?.Name ?? string.Empty;
        if (string.IsNullOrWhiteSpace(resolved))
        {
            _logger.LogWarning("Hero key missing in cached list, refreshing from OpenDota for heroId={HeroId}", heroId);
            var refreshed = await _client.GetHeroesAsync(cancellationToken);
            if (refreshed.Count > 0)
            {
                await _cache.SaveHeroesAsync(refreshed, cancellationToken);
                resolved = refreshed.FirstOrDefault(h => h.Id == heroId)?.Name ?? string.Empty;
            }
        }

        _logger.LogDebug("Hero key resolved via fallback {HeroId}={Key}", heroId, resolved);
        return resolved;
    }

    private static string[] BuildBaseUrls(IConfiguration config)
    {
        var primary = config["Assets:HeroImageBaseUrl"];
        var fallback = config["Assets:HeroImageFallbackBaseUrl"];

        var list = new List<string>();
        if (!string.IsNullOrWhiteSpace(primary)) list.Add(primary);
        if (!string.IsNullOrWhiteSpace(fallback)) list.Add(fallback);

        if (list.Count == 0)
        {
            list.Add("https://cdn.opendota.com/apps/dota2/images/heroes");
            list.Add("https://cdn.cloudflare.steamstatic.com/apps/dota2/images/heroes");
        }

        return list.Distinct().ToArray();
    }
}
