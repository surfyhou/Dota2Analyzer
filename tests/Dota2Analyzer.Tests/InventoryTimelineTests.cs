using System.Text;
using Dota2Analyzer.Core.Analysis;
using Dota2Analyzer.Core.Models.OpenDota;
using Dota2Analyzer.Core.Services;
using Microsoft.Extensions.Logging.Abstractions;

namespace Dota2Analyzer.Tests;

public sealed class InventoryTimelineTests
{
    [Fact]
    public async Task BuildInventoryTimeline_RemovesComponentsOnCompose()
    {
        var dbPath = Path.Combine(Path.GetTempPath(), $"match-cache-{Guid.NewGuid():N}.db");
        try
        {
            var cache = new MatchCache(NullLogger<MatchCache>.Instance, dbPath);
            var items = new Dictionary<string, ItemConstants>(StringComparer.OrdinalIgnoreCase)
            {
                ["ogre_axe"] = new ItemConstants { Id = 2, DisplayName = "Ogre Axe", Components = [] },
                ["mithril_hammer"] = new ItemConstants { Id = 3, DisplayName = "Mithril Hammer", Components = [] },
                ["black_king_bar"] = new ItemConstants
                {
                    Id = 116,
                    DisplayName = "Black King Bar",
                    Components = ["item_ogre_axe", "item_mithril_hammer", "recipe_black_king_bar"]
                }
            };

            await cache.SaveItemConstantsAsync(items, CancellationToken.None);

            var heroData = new HeroDataCache(
                new OpenDotaClient(new HttpClient { BaseAddress = new Uri("http://localhost") }),
                cache,
                NullLogger<HeroDataCache>.Instance,
                cacheOnly: true);

            await heroData.EnsureLoadedAsync(CancellationToken.None);

            var player = new PlayerDetail
            {
                PurchaseLog =
                [
                    new PurchaseLogEntry { Time = 60, Key = "ogre_axe" },
                    new PurchaseLogEntry { Time = 120, Key = "mithril_hammer" },
                    new PurchaseLogEntry { Time = 300, Key = "black_king_bar" }
                ]
            };

            var timeline = MatchAnalyzer.BuildInventoryTimeline(player, 600, heroData);
            var last = timeline.Last().Items.Select(i => i.Key).ToList();

            Assert.Contains("black_king_bar", last);
            Assert.DoesNotContain("ogre_axe", last);
            Assert.DoesNotContain("mithril_hammer", last);
        }
        finally
        {
            try
            {
                if (File.Exists(dbPath))
                {
                    File.Delete(dbPath);
                }
            }
            catch
            {
                // best-effort cleanup; sqlite may keep file locked briefly
            }
        }
    }
}
