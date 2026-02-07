using System.Net;
using System.Text;
using System.Text.Json;
using Dota2Analyzer.Api.Services;
using Dota2Analyzer.Core.Analysis;
using Dota2Analyzer.Core.Models.OpenDota;
using Dota2Analyzer.Core.Services;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging.Abstractions;
using Xunit;

namespace Dota2Analyzer.Tests;

public sealed class CacheIntegrationTests
{
    [Fact]
    public async Task AnalyzeRecent_UsesCacheOnly_NoOpenDotaCalls()
    {
        var tempDb = Path.Combine(Path.GetTempPath(), Guid.NewGuid() + ".db");
        var cache = new MatchCache(NullLogger<MatchCache>.Instance, tempDb);

        var accountId = 42L;
        var matches = new List<RecentMatch>
        {
            new()
            {
                MatchId = 1001,
                PlayerSlot = 0,
                RadiantWin = true,
                Duration = 1800,
                StartTime = 100,
                HeroId = 1
            },
            new()
            {
                MatchId = 1002,
                PlayerSlot = 0,
                RadiantWin = false,
                Duration = 2000,
                StartTime = 90,
                HeroId = 2
            }
        };
        await cache.SaveRecentMatchesAsync(accountId, matches, CancellationToken.None);

        foreach (var match in matches)
        {
            var detail = new MatchDetail
            {
                MatchId = match.MatchId,
                Duration = match.Duration,
                RadiantWin = match.RadiantWin,
                Players = new List<PlayerDetail>
                {
                    new()
                    {
                        AccountId = (int)accountId,
                        PlayerSlot = 0,
                        HeroId = match.HeroId,
                        Kills = 1,
                        Deaths = 1,
                        Assists = 1,
                        LastHits = 100,
                        Denies = 0,
                        GoldPerMin = 500,
                        XpPerMin = 500,
                        Level = 20,
                        Lane = 1,
                        LaneRole = 1,
                        HeroDamage = 1000,
                        TowerDamage = 100
                    }
                }
            };
            await cache.SaveMatchDetailAsync(match.MatchId, detail, CancellationToken.None);
        }

        var handler = new ThrowingHandler();
        var httpClient = new HttpClient(handler) { BaseAddress = new Uri("https://api.opendota.com/api/") };
        var client = new OpenDotaClient(httpClient);
        var heroData = new HeroDataCache(client, cache, NullLogger<HeroDataCache>.Instance, cacheOnly: true);
        var analyzer = new MatchAnalyzer(client, heroData, cache, NullLogger<MatchAnalyzer>.Instance, cacheOnly: true, disableBenchmarks: true, avoidExternalWhenCached: true);

        var results = await analyzer.AnalyzeRecentAsync(accountId, desiredCount: 2, fetchLimit: 100, requestParse: false, onlyPos1: false, CancellationToken.None);

        Assert.Equal(2, results.Count);
        Assert.Equal(0, handler.CallCount);
    }

    [Fact]
    public async Task Preload_SavesRecentMatchesToCache()
    {
        var tempDb = Path.Combine(Path.GetTempPath(), Guid.NewGuid() + ".db");
        var cache = new MatchCache(NullLogger<MatchCache>.Instance, tempDb);

        var handler = new PreloadHandler();
        var httpClient = new HttpClient(handler) { BaseAddress = new Uri("https://api.opendota.com/api/") };
        var client = new OpenDotaClient(httpClient);
        var heroData = new HeroDataCache(client, cache, NullLogger<HeroDataCache>.Instance, cacheOnly: true);
        var config = new ConfigurationBuilder().AddInMemoryCollection().Build();
        var heroImages = new HeroImageCache(heroData, cache, client, NullLogger<HeroImageCache>.Instance, config);
        var service = new PreloadService(client, cache, heroImages, NullLogger<PreloadService>.Instance);

        var status = await service.StartAsync(123, 2, CancellationToken.None);

        var timeout = DateTime.UtcNow.AddSeconds(5);
        while (status.IsRunning && DateTime.UtcNow < timeout)
        {
            await Task.Delay(100);
        }

        var cached = await cache.GetRecentMatchesAsync(123, TimeSpan.FromDays(1), CancellationToken.None);
        Assert.NotNull(cached);
        Assert.Equal(2, cached!.Count);
    }

    [Fact]
    public async Task AnalyzeRecent_WhenLatestMatchInCache_DoesNotFetchFullList()
    {
        var tempDb = Path.Combine(Path.GetTempPath(), Guid.NewGuid() + ".db");
        var cache = new MatchCache(NullLogger<MatchCache>.Instance, tempDb);

        var accountId = 7L;
        var cachedMatches = new List<RecentMatch>
        {
            new() { MatchId = 3001, PlayerSlot = 0, RadiantWin = true, Duration = 1800, StartTime = 500, HeroId = 1 },
            new() { MatchId = 3000, PlayerSlot = 0, RadiantWin = false, Duration = 1600, StartTime = 400, HeroId = 2 }
        };
        await cache.SaveRecentMatchesAsync(accountId, cachedMatches, CancellationToken.None);

        foreach (var match in cachedMatches)
        {
            var detail = new MatchDetail
            {
                MatchId = match.MatchId,
                Duration = match.Duration,
                RadiantWin = match.RadiantWin,
                Players = new List<PlayerDetail>
                {
                    new()
                    {
                        AccountId = (int)accountId,
                        PlayerSlot = 0,
                        HeroId = match.HeroId,
                        Kills = 1,
                        Deaths = 1,
                        Assists = 1,
                        LastHits = 100,
                        Denies = 0,
                        GoldPerMin = 500,
                        XpPerMin = 500,
                        Level = 20,
                        Lane = 1,
                        LaneRole = 1,
                        HeroDamage = 1000,
                        TowerDamage = 100
                    }
                }
            };
            await cache.SaveMatchDetailAsync(match.MatchId, detail, CancellationToken.None);
        }

        var handler = new LatestMatchHandler();
        var httpClient = new HttpClient(handler) { BaseAddress = new Uri("https://api.opendota.com/api/") };
        var client = new OpenDotaClient(httpClient);
        var heroData = new HeroDataCache(client, cache, NullLogger<HeroDataCache>.Instance, cacheOnly: true);
        var analyzer = new MatchAnalyzer(client, heroData, cache, NullLogger<MatchAnalyzer>.Instance, cacheOnly: false, disableBenchmarks: true, avoidExternalWhenCached: true);

        var results = await analyzer.AnalyzeRecentAsync(accountId, desiredCount: 2, fetchLimit: 200, requestParse: false, onlyPos1: false, CancellationToken.None);

        Assert.Equal(2, results.Count);
        Assert.Equal(0, handler.FullListCalls);
        Assert.Equal(0, handler.RecentOneCalls);
    }

    private sealed class ThrowingHandler : HttpMessageHandler
    {
        public int CallCount { get; private set; }

        protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
        {
            CallCount++;
            throw new InvalidOperationException("HTTP call should not happen in cache-only mode");
        }
    }

    private sealed class PreloadHandler : HttpMessageHandler
    {
        protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
        {
            var path = request.RequestUri!.AbsolutePath;
            if (path.Contains("/players/123/matches"))
            {
                var matches = new List<RecentMatch>
                {
                    new()
                    {
                        MatchId = 2001,
                        PlayerSlot = 0,
                        RadiantWin = true,
                        Duration = 1800,
                        StartTime = 200,
                        HeroId = 1
                    },
                    new()
                    {
                        MatchId = 2002,
                        PlayerSlot = 0,
                        RadiantWin = false,
                        Duration = 1900,
                        StartTime = 150,
                        HeroId = 2
                    }
                };
                return Task.FromResult(Json(matches));
            }

            if (path.Contains("/matches/2001"))
            {
                return Task.FromResult(Json(BuildDetail(2001, true)));
            }

            if (path.Contains("/matches/2002"))
            {
                return Task.FromResult(Json(BuildDetail(2002, false)));
            }

            return Task.FromResult(new HttpResponseMessage(HttpStatusCode.NotFound));
        }

        private static MatchDetail BuildDetail(long matchId, bool radiantWin)
        {
            var players = new List<PlayerDetail>();
            for (var i = 0; i < 10; i++)
            {
                players.Add(new PlayerDetail
                {
                    AccountId = 100 + i,
                    PlayerSlot = i < 5 ? i : 128 + (i - 5),
                    HeroId = i + 1,
                    Kills = 0,
                    Deaths = 0,
                    Assists = 0,
                    LastHits = 10,
                    Denies = 0,
                    GoldPerMin = 300,
                    XpPerMin = 300,
                    Level = 10,
                    Lane = 1,
                    LaneRole = i < 5 ? 1 : 3,
                    HeroDamage = 100,
                    TowerDamage = 50
                });
            }

            return new MatchDetail
            {
                MatchId = matchId,
                Duration = 1800,
                RadiantWin = radiantWin,
                Players = players
            };
        }

        internal static HttpResponseMessage Json<T>(T payload)
        {
            var json = JsonSerializer.Serialize(payload);
            return new HttpResponseMessage(HttpStatusCode.OK)
            {
                Content = new StringContent(json, Encoding.UTF8, "application/json")
            };
        }
    }

    private sealed class LatestMatchHandler : HttpMessageHandler
    {
        public int RecentOneCalls { get; private set; }
        public int FullListCalls { get; private set; }

        protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
        {
            var path = request.RequestUri!.AbsolutePath;
            if (path.Contains("/players/7/recentMatches"))
            {
                RecentOneCalls++;
                var matches = new List<RecentMatch>
                {
                    new() { MatchId = 3001, PlayerSlot = 0, RadiantWin = true, Duration = 1800, StartTime = 500, HeroId = 1 }
                };
                return Task.FromResult(PreloadHandler.Json(matches));
            }

            if (path.Contains("/players/7/matches"))
            {
                FullListCalls++;
                return Task.FromResult(new HttpResponseMessage(HttpStatusCode.InternalServerError));
            }

            return Task.FromResult(new HttpResponseMessage(HttpStatusCode.NotFound));
        }
    }
}
