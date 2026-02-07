using Dota2Analyzer.Core.Services;

namespace Dota2Analyzer.Api.Services;

public sealed class ItemImageCache
{
    private readonly HeroDataCache _heroData;
    private readonly HttpClient _httpClient;
    private readonly string _itemImageDir;
    private readonly ILogger<ItemImageCache> _logger;
    private readonly string[] _baseUrls;

    public ItemImageCache(HeroDataCache heroData, ILogger<ItemImageCache> logger, IConfiguration config)
    {
        _heroData = heroData;
        _logger = logger;
        _httpClient = new HttpClient();
        _baseUrls = BuildBaseUrls(config);

        var basePath = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
        var folder = Path.Combine(basePath, "Dota2Analyzer", "assets", "items");
        Directory.CreateDirectory(folder);
        _itemImageDir = folder;
    }

    public async Task<string?> EnsureItemImageAsync(string itemKey, CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(itemKey)) return null;

        var normalized = NormalizeKey(itemKey);
        if (string.IsNullOrWhiteSpace(normalized)) return null;

        var localPath = Path.Combine(_itemImageDir, $"{normalized}.png");
        if (File.Exists(localPath))
        {
            _logger.LogDebug("Item image cache hit {Path}", localPath);
            return localPath;
        }

        await _heroData.EnsureLoadedAsync(cancellationToken);
        if (!_heroData.TryGetItemConstants(normalized, out var constants) || constants is null)
        {
            _logger.LogWarning("Item constants missing for {Key}", normalized);
            return null;
        }

        if (string.IsNullOrWhiteSpace(constants.Img))
        {
            _logger.LogWarning("Item image path missing for {Key}", normalized);
            return null;
        }

        foreach (var url in BuildCandidateUrls(constants.Img))
        {
            _logger.LogInformation("Downloading item image {Url}", url);
            try
            {
                using var response = await _httpClient.GetAsync(url, cancellationToken);
                if (!response.IsSuccessStatusCode)
                {
                    _logger.LogWarning("Item image download failed {StatusCode} for {Url}", response.StatusCode, url);
                    continue;
                }

                await using var stream = await response.Content.ReadAsStreamAsync(cancellationToken);
                await using var fs = File.Create(localPath);
                await stream.CopyToAsync(fs, cancellationToken);
                _logger.LogInformation("Item image saved {Path}", localPath);
                return localPath;
            }
            catch (HttpRequestException ex)
            {
                _logger.LogWarning(ex, "Item image download failed for {Url}", url);
            }
        }

        return null;
    }

    private IEnumerable<string> BuildCandidateUrls(string imgPath)
    {
        if (string.IsNullOrWhiteSpace(imgPath))
        {
            yield break;
        }

        if (imgPath.StartsWith("http", StringComparison.OrdinalIgnoreCase))
        {
            yield return imgPath;
            yield break;
        }

        var trimmed = imgPath.StartsWith("/") ? imgPath : "/" + imgPath;
        foreach (var baseUrl in _baseUrls)
        {
            yield return $"{baseUrl.TrimEnd('/')}{trimmed}";
        }
    }

    private static string NormalizeKey(string key)
    {
        var normalized = key.Trim().ToLowerInvariant();
        if (normalized.StartsWith("item_"))
        {
            normalized = normalized[5..];
        }
        return normalized;
    }

    private static string[] BuildBaseUrls(IConfiguration config)
    {
        var primary = config["Assets:ItemImageBaseUrl"];
        var fallback = config["Assets:ItemImageFallbackBaseUrl"];

        var list = new List<string>();
        if (!string.IsNullOrWhiteSpace(primary)) list.Add(primary);
        if (!string.IsNullOrWhiteSpace(fallback)) list.Add(fallback);

        if (list.Count == 0)
        {
            list.Add("https://cdn.opendota.com");
            list.Add("https://cdn.cloudflare.steamstatic.com");
        }

        return list.Distinct().ToArray();
    }
}
