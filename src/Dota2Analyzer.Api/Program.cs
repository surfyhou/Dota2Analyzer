using Dota2Analyzer.Api.Models;
using Dota2Analyzer.Core.Analysis;
using Dota2Analyzer.Core.Models.Analysis;
using Dota2Analyzer.Core.Models.OpenDota;
using Dota2Analyzer.Core.Services;
using Microsoft.AspNetCore.Diagnostics;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddEndpointsApiExplorer();
builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(policy =>
        policy.WithOrigins("http://localhost:5173")
            .AllowAnyHeader()
            .AllowAnyMethod());
});

builder.Services.AddHttpClient("opendota", client =>
{
    client.BaseAddress = new Uri("https://api.opendota.com/api/");
    client.Timeout = TimeSpan.FromSeconds(20);
});
builder.Services.AddHttpLogging(options =>
{
    options.LoggingFields = Microsoft.AspNetCore.HttpLogging.HttpLoggingFields.RequestPath
                            | Microsoft.AspNetCore.HttpLogging.HttpLoggingFields.RequestMethod
                            | Microsoft.AspNetCore.HttpLogging.HttpLoggingFields.ResponseStatusCode
                            | Microsoft.AspNetCore.HttpLogging.HttpLoggingFields.Duration;
});

builder.Services.AddSingleton(sp =>
{
    var factory = sp.GetRequiredService<IHttpClientFactory>();
    return new OpenDotaClient(factory.CreateClient("opendota"));
});
builder.Services.AddSingleton<HeroDataCache>();
builder.Services.AddSingleton<MatchCache>();
builder.Services.AddSingleton<MatchAnalyzer>();

var app = builder.Build();

app.UseHttpLogging();
app.UseCors();
app.UseExceptionHandler(errorApp =>
{
    errorApp.Run(async context =>
    {
        var logger = context.RequestServices.GetRequiredService<ILogger<Program>>();
        var exception = context.Features.Get<IExceptionHandlerPathFeature>()?.Error;
        logger.LogError(exception, "Unhandled exception at {Path}", context.Request.Path);

        context.Response.StatusCode = StatusCodes.Status500InternalServerError;
        var message = app.Environment.IsDevelopment()
            ? exception?.Message ?? "未知错误"
            : "服务器异常，请查看日志";
        await context.Response.WriteAsJsonAsync(new { message });
    });
});

app.MapGet("/api/health", () => Results.Ok(new { status = "ok", time = DateTime.UtcNow }));

app.MapGet("/api/players/{accountId:long}/recent", async (
    long accountId,
    int? limit,
    HeroDataCache heroData,
    OpenDotaClient client,
    MatchCache cache,
    ILogger<Program> logger,
    CancellationToken cancellationToken) =>
{
    var take = Math.Clamp(limit ?? 20, 1, 50);
    logger.LogInformation("GET recent matches AccountId={AccountId} Limit={Limit}", accountId, take);
    await heroData.EnsureLoadedAsync(cancellationToken);
    var matches = await cache.GetRecentMatchesAsync(accountId, TimeSpan.FromMinutes(30), cancellationToken);
    if (matches is null || matches.Count == 0)
    {
        matches = await client.GetRecentMatchesAsync(accountId, take, cancellationToken);
        if (matches.Count > 0)
        {
            await cache.SaveRecentMatchesAsync(accountId, matches, cancellationToken);
        }
    }

    var result = matches.Select(match =>
    {
        var isRadiant = match.PlayerSlot < 128;
        var won = (isRadiant && match.RadiantWin) || (!isRadiant && !match.RadiantWin);
        return new RecentMatchDto
        {
            MatchId = match.MatchId,
            HeroName = heroData.GetHeroName(match.HeroId),
            ResultText = won ? "胜利" : "失败",
            Won = won,
            Kda = $"{match.Kills}/{match.Deaths}/{match.Assists}",
            GpmXpm = $"{match.GoldPerMin}/{match.XpPerMin}",
            DurationMinutes = match.Duration / 60
        };
    });

    return Results.Ok(result);
});

app.MapPost("/api/players/{accountId:long}/analyze-recent", async (
    long accountId,
    int? limit,
    bool? requestParse,
    bool? onlyPos1,
    MatchAnalyzer analyzer,
    ILogger<Program> logger,
    CancellationToken cancellationToken) =>
{
    var take = Math.Clamp(limit ?? 20, 1, 50);
    var parse = requestParse ?? true;
    var onlyCarry = onlyPos1 ?? true;
    logger.LogInformation("POST analyze recent AccountId={AccountId} Limit={Limit} OnlyPos1={OnlyPos1} Parse={Parse}", accountId, take, onlyCarry, parse);

    var analyses = await analyzer.AnalyzeRecentAsync(accountId, take, parse, onlyCarry, cancellationToken);
    var summary = BuildSummary(analyses);

    return Results.Ok(new AnalysisResponse
    {
        Summary = summary,
        Matches = analyses
    });
});

app.MapGet("/api/matches/{matchId:long}/analyze", async (
    long matchId,
    long accountId,
    bool? requestParse,
    MatchAnalyzer analyzer,
    OpenDotaClient client,
    MatchCache cache,
    ILogger<Program> logger,
    CancellationToken cancellationToken) =>
{
    var parse = requestParse ?? true;
    logger.LogInformation("GET analyze match MatchId={MatchId} AccountId={AccountId} Parse={Parse}", matchId, accountId, parse);
    if (parse)
    {
        _ = await client.RequestParseAsync(matchId, cancellationToken);
    }

    var detail = await cache.GetMatchDetailAsync(matchId, TimeSpan.FromDays(7), cancellationToken);
    if (detail is null)
    {
        detail = await client.GetMatchDetailAsync(matchId, cancellationToken);
        if (detail is not null)
        {
            await cache.SaveMatchDetailAsync(matchId, detail, cancellationToken);
        }
    }
    if (detail?.Players is null)
    {
        logger.LogWarning("Match not parsed or not found MatchId={MatchId}", matchId);
        return Results.NotFound(new { message = "对局尚未解析或不存在" });
    }

    var accountId32 = unchecked((int)accountId);
    var player = detail.Players.FirstOrDefault(p => p.AccountId == accountId32);
    if (player is null)
    {
        logger.LogWarning("Account {AccountId} not found in match {MatchId}", accountId, matchId);
        return Results.NotFound(new { message = "未在对局中找到该玩家" });
    }

    var recentMatch = new RecentMatch
    {
        MatchId = detail.MatchId,
        PlayerSlot = player.PlayerSlot,
        RadiantWin = detail.RadiantWin,
        Duration = detail.Duration,
        HeroId = player.HeroId,
        Kills = player.Kills,
        Deaths = player.Deaths,
        Assists = player.Assists,
        LastHits = player.LastHits,
        Denies = player.Denies,
        GoldPerMin = player.GoldPerMin,
        XpPerMin = player.XpPerMin,
        HeroDamage = player.HeroDamage,
        TowerDamage = player.TowerDamage,
        HeroHealing = 0,
        Level = player.Level
    };

    var analysis = await analyzer.AnalyzeMatchAsync(recentMatch, accountId, parse, false, cancellationToken);
    if (analysis is null)
    {
        logger.LogWarning("Analysis failed MatchId={MatchId} AccountId={AccountId}", matchId, accountId);
        return Results.NotFound(new { message = "无法生成分析" });
    }

    return Results.Ok(analysis);
});

app.Run();

static AnalysisSummary BuildSummary(List<MatchAnalysisResult> analyses)
{
    var total = analyses.Count;
    var wins = analyses.Count(a => a.Won);
    var parsed = analyses.Count(a => !a.LaneResult.Contains("尚未解析"));
    var unparsed = total - parsed;
    var winRate = total == 0 ? 0 : (double)wins / total * 100;

    return new AnalysisSummary
    {
        TotalMatches = total,
        Wins = wins,
        WinRate = winRate,
        ParsedMatches = parsed,
        UnparsedMatches = unparsed
    };
}
