using System.Text.Json;
using Dota2Analyzer.Core.Models.OpenDota;
using Microsoft.Data.Sqlite;
using Microsoft.Extensions.Logging;

namespace Dota2Analyzer.Core.Services;

public sealed class MatchCache
{
    private readonly string _dbPath;
    private readonly JsonSerializerOptions _jsonOptions = new() { PropertyNameCaseInsensitive = true };
    private readonly ILogger<MatchCache> _logger;
    private bool _initialized;

    public MatchCache(ILogger<MatchCache> logger, string? dbPath = null)
    {
        _logger = logger;
        var basePath = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
        var folder = Path.Combine(basePath, "Dota2Analyzer");
        Directory.CreateDirectory(folder);
        _dbPath = dbPath ?? Path.Combine(folder, "match-cache.db");
    }

    public async Task<MatchDetail?> GetMatchDetailAsync(long matchId, TimeSpan? maxAge, CancellationToken cancellationToken)
    {
        await EnsureInitializedAsync(cancellationToken);

        await using var connection = new SqliteConnection($"Data Source={_dbPath}");
        await connection.OpenAsync(cancellationToken);

        var command = connection.CreateCommand();
        command.CommandText = "SELECT json, updated_at FROM match_cache WHERE match_id = $id";
        command.Parameters.AddWithValue("$id", matchId);

        await using var reader = await command.ExecuteReaderAsync(cancellationToken);
        if (!await reader.ReadAsync(cancellationToken))
        {
            _logger.LogDebug("Match cache miss: {MatchId}", matchId);
            return null;
        }

        var json = reader.GetString(0);
        var updatedAt = DateTimeOffset.Parse(reader.GetString(1));
        if (maxAge.HasValue && updatedAt < DateTimeOffset.UtcNow.Subtract(maxAge.Value))
        {
            _logger.LogDebug("Match cache expired: {MatchId} updated {UpdatedAt}", matchId, updatedAt);
            return null;
        }

        _logger.LogDebug("Match cache hit: {MatchId} updated {UpdatedAt}", matchId, updatedAt);
        return JsonSerializer.Deserialize<MatchDetail>(json, _jsonOptions);
    }

    public async Task SaveMatchDetailAsync(long matchId, MatchDetail detail, CancellationToken cancellationToken)
    {
        await EnsureInitializedAsync(cancellationToken);

        var json = JsonSerializer.Serialize(detail, _jsonOptions);
        var now = DateTimeOffset.UtcNow.ToString("O");

        await using var connection = new SqliteConnection($"Data Source={_dbPath}");
        await connection.OpenAsync(cancellationToken);

        var command = connection.CreateCommand();
        command.CommandText = """
            INSERT INTO match_cache(match_id, json, updated_at)
            VALUES ($id, $json, $updated_at)
            ON CONFLICT(match_id) DO UPDATE SET json = $json, updated_at = $updated_at;
            """;
        command.Parameters.AddWithValue("$id", matchId);
        command.Parameters.AddWithValue("$json", json);
        command.Parameters.AddWithValue("$updated_at", now);

        await command.ExecuteNonQueryAsync(cancellationToken);
        _logger.LogDebug("Match cache saved: {MatchId}", matchId);
    }

    public async Task<List<RecentMatch>?> GetRecentMatchesAsync(long accountId, TimeSpan? maxAge, CancellationToken cancellationToken)
    {
        var row = await GetCacheRowAsync("recent_matches_cache", "account_id", accountId, maxAge, cancellationToken);
        if (row is null)
        {
            _logger.LogDebug("Recent matches cache miss: {AccountId}", accountId);
            return null;
        }
        _logger.LogDebug("Recent matches cache hit: {AccountId}", accountId);
        return JsonSerializer.Deserialize<List<RecentMatch>>(row, _jsonOptions);
    }

    public async Task SaveRecentMatchesAsync(long accountId, List<RecentMatch> matches, CancellationToken cancellationToken)
    {
        await SaveCacheRowAsync("recent_matches_cache", "account_id", accountId, matches, cancellationToken);
        _logger.LogDebug("Recent matches cache saved: {AccountId} ({Count})", accountId, matches.Count);
    }

    public async Task<List<Hero>?> GetHeroesAsync(TimeSpan? maxAge, CancellationToken cancellationToken)
    {
        var row = await GetCacheRowAsync("hero_cache", "cache_key", "heroes", maxAge, cancellationToken);
        if (row is null)
        {
            _logger.LogDebug("Hero cache miss");
            return null;
        }
        _logger.LogDebug("Hero cache hit");
        return JsonSerializer.Deserialize<List<Hero>>(row, _jsonOptions);
    }

    public async Task SaveHeroesAsync(List<Hero> heroes, CancellationToken cancellationToken)
    {
        await SaveCacheRowAsync("hero_cache", "cache_key", "heroes", heroes, cancellationToken);
        _logger.LogDebug("Hero cache saved ({Count})", heroes.Count);
    }

    public async Task<List<HeroStats>?> GetHeroStatsAsync(TimeSpan? maxAge, CancellationToken cancellationToken)
    {
        var row = await GetCacheRowAsync("hero_cache", "cache_key", "hero_stats", maxAge, cancellationToken);
        if (row is null)
        {
            _logger.LogDebug("Hero stats cache miss");
            return null;
        }
        _logger.LogDebug("Hero stats cache hit");
        return JsonSerializer.Deserialize<List<HeroStats>>(row, _jsonOptions);
    }

    public async Task SaveHeroStatsAsync(List<HeroStats> stats, CancellationToken cancellationToken)
    {
        await SaveCacheRowAsync("hero_cache", "cache_key", "hero_stats", stats, cancellationToken);
        _logger.LogDebug("Hero stats cache saved ({Count})", stats.Count);
    }

    public async Task<Dictionary<string, ItemConstants>?> GetItemConstantsAsync(TimeSpan? maxAge, CancellationToken cancellationToken)
    {
        var row = await GetCacheRowAsync("hero_cache", "cache_key", "item_constants", maxAge, cancellationToken);
        if (row is null)
        {
            _logger.LogDebug("Item constants cache miss");
            return null;
        }
        _logger.LogDebug("Item constants cache hit");
        return JsonSerializer.Deserialize<Dictionary<string, ItemConstants>>(row, _jsonOptions);
    }

    public async Task SaveItemConstantsAsync(Dictionary<string, ItemConstants> items, CancellationToken cancellationToken)
    {
        await SaveCacheRowAsync("hero_cache", "cache_key", "item_constants", items, cancellationToken);
        _logger.LogDebug("Item constants cache saved ({Count})", items.Count);
    }

    private async Task EnsureInitializedAsync(CancellationToken cancellationToken)
    {
        if (_initialized) return;

        await using var connection = new SqliteConnection($"Data Source={_dbPath}");
        await connection.OpenAsync(cancellationToken);

        var command = connection.CreateCommand();
        command.CommandText = """
            CREATE TABLE IF NOT EXISTS match_cache (
                match_id INTEGER PRIMARY KEY,
                json TEXT NOT NULL,
                updated_at TEXT NOT NULL
            );
            CREATE TABLE IF NOT EXISTS recent_matches_cache (
                account_id INTEGER PRIMARY KEY,
                json TEXT NOT NULL,
                updated_at TEXT NOT NULL
            );
            CREATE TABLE IF NOT EXISTS hero_cache (
                cache_key TEXT PRIMARY KEY,
                json TEXT NOT NULL,
                updated_at TEXT NOT NULL
            );
            """;

        await command.ExecuteNonQueryAsync(cancellationToken);
        _initialized = true;
    }

    private async Task<string?> GetCacheRowAsync(string table, string keyColumn, object key, TimeSpan? maxAge, CancellationToken cancellationToken)
    {
        await EnsureInitializedAsync(cancellationToken);

        await using var connection = new SqliteConnection($"Data Source={_dbPath}");
        await connection.OpenAsync(cancellationToken);

        var command = connection.CreateCommand();
        command.CommandText = $"SELECT json, updated_at FROM {table} WHERE {keyColumn} = $key";
        command.Parameters.AddWithValue("$key", key);

        await using var reader = await command.ExecuteReaderAsync(cancellationToken);
        if (!await reader.ReadAsync(cancellationToken))
        {
            return null;
        }

        var json = reader.GetString(0);
        var updatedAt = DateTimeOffset.Parse(reader.GetString(1));
        if (maxAge.HasValue && updatedAt < DateTimeOffset.UtcNow.Subtract(maxAge.Value))
        {
            return null;
        }

        return json;
    }

    private async Task SaveCacheRowAsync<T>(string table, string keyColumn, object key, T payload, CancellationToken cancellationToken)
    {
        await EnsureInitializedAsync(cancellationToken);

        var json = JsonSerializer.Serialize(payload, _jsonOptions);
        var now = DateTimeOffset.UtcNow.ToString("O");

        await using var connection = new SqliteConnection($"Data Source={_dbPath}");
        await connection.OpenAsync(cancellationToken);

        var command = connection.CreateCommand();
        command.CommandText = $"""
            INSERT INTO {table}({keyColumn}, json, updated_at)
            VALUES ($key, $json, $updated_at)
            ON CONFLICT({keyColumn}) DO UPDATE SET json = $json, updated_at = $updated_at;
            """;
        command.Parameters.AddWithValue("$key", key);
        command.Parameters.AddWithValue("$json", json);
        command.Parameters.AddWithValue("$updated_at", now);

        await command.ExecuteNonQueryAsync(cancellationToken);
    }
}
