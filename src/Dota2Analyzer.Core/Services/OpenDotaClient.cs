using System.Net.Http.Json;
using System.Text.Json;
using Dota2Analyzer.Core.Models.OpenDota;

namespace Dota2Analyzer.Core.Services;

public sealed class OpenDotaClient
{
    private readonly HttpClient _httpClient;
    private readonly JsonSerializerOptions _jsonOptions = new() { PropertyNameCaseInsensitive = true };

    public OpenDotaClient(HttpClient httpClient)
    {
        _httpClient = httpClient;
        if (_httpClient.BaseAddress is null)
        {
            _httpClient.BaseAddress = new Uri("https://api.opendota.com/api/");
        }
    }

    public async Task<List<Hero>> GetHeroesAsync(CancellationToken cancellationToken)
    {
        return await _httpClient.GetFromJsonAsync<List<Hero>>("heroes", _jsonOptions, cancellationToken)
               ?? [];
    }

    public async Task<List<HeroStats>> GetHeroStatsAsync(CancellationToken cancellationToken)
    {
        return await _httpClient.GetFromJsonAsync<List<HeroStats>>("heroStats", _jsonOptions, cancellationToken)
               ?? [];
    }

    public async Task<List<RecentMatch>> GetRecentMatchesAsync(long accountId, int limit, CancellationToken cancellationToken)
    {
        var url = $"players/{accountId}/recentMatches?limit={limit}&lobby_type=7";
        return await _httpClient.GetFromJsonAsync<List<RecentMatch>>(url, _jsonOptions, cancellationToken)
               ?? [];
    }

    public async Task<MatchDetail?> GetMatchDetailAsync(long matchId, CancellationToken cancellationToken)
    {
        return await _httpClient.GetFromJsonAsync<MatchDetail>($"matches/{matchId}", _jsonOptions, cancellationToken);
    }

    public async Task<BenchmarksResponse?> GetHeroBenchmarksAsync(int heroId, CancellationToken cancellationToken)
    {
        return await _httpClient.GetFromJsonAsync<BenchmarksResponse>($"benchmarks?hero_id={heroId}", _jsonOptions, cancellationToken);
    }

    public async Task<bool> RequestParseAsync(long matchId, CancellationToken cancellationToken)
    {
        var response = await _httpClient.PostAsync($"request/{matchId}", null, cancellationToken);
        return response.IsSuccessStatusCode;
    }
}
