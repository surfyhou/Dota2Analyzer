package com.dota2analyzer.core.analysis;

import com.dota2analyzer.core.model.analysis.*;
import com.dota2analyzer.core.model.opendota.*;
import com.dota2analyzer.core.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MatchAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(MatchAnalyzer.class);

    private final OpenDotaClient client;
    private final HeroDataCache heroData;
    private final MatchCache cache;
    private final boolean cacheOnly;
    private final boolean disableBenchmarks;
    private final boolean avoidExternalWhenCached;

    private static final Set<String> DISABLE_HERO_NAMES = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    static {
        DISABLE_HERO_NAMES.addAll(Arrays.asList(
            "Axe", "Bane", "Beastmaster", "Centaur Warrunner", "Chaos Knight", "Crystal Maiden",
            "Dark Seer", "Doom", "Dragon Knight", "Earth Spirit", "Earthshaker", "Elder Titan",
            "Enigma", "Faceless Void", "Grimstroke", "Invoker", "Kunkka", "Legion Commander",
            "Lion", "Magnus", "Mars", "Medusa", "Mirana", "Nyx Assassin", "Ogre Magi",
            "Pudge", "Puck", "Riki", "Sand King", "Shadow Shaman", "Slardar", "Snapfire",
            "Spirit Breaker", "Storm Spirit", "Sven", "Tidehunter", "Tiny", "Treant Protector",
            "Tusk", "Underlord", "Vengeful Spirit", "Warlock", "Windranger", "Winter Wyvern",
            "Witch Doctor", "Zeus", "Primal Beast", "Ringmaster", "Marci", "Muerta"
        ));
    }

    public MatchAnalyzer(OpenDotaClient client, HeroDataCache heroData, MatchCache cache,
                         boolean cacheOnly, boolean disableBenchmarks, boolean avoidExternalWhenCached) {
        this.client = client;
        this.heroData = heroData;
        this.cache = cache;
        this.cacheOnly = cacheOnly;
        this.disableBenchmarks = disableBenchmarks;
        this.avoidExternalWhenCached = avoidExternalWhenCached;
    }

    public List<MatchAnalysisResult> analyzeRecent(long accountId, int desiredCount, int fetchLimit,
                                                    boolean requestParse, boolean onlyPos1) {
        heroData.ensureLoaded();
        List<RecentMatch> matches = cache.getRecentMatches(accountId, Duration.ofMinutes(30));
        if (matches == null) matches = new ArrayList<>();

        if (cacheOnly) {
            if (matches.isEmpty()) {
                log.warn("Recent matches cache empty and cache-only enabled.");
            }
        } else {
            if (matches.isEmpty()) {
                log.info("Recent matches cache empty. Fetching from OpenDota AccountId={} Target={}", accountId, fetchLimit);
                matches = fetchRecentMatchesWithPaging(accountId, fetchLimit);
                if (!matches.isEmpty()) {
                    cache.saveRecentMatches(accountId, matches);
                }
            } else {
                boolean shouldRefresh = avoidExternalWhenCached
                        ? false
                        : shouldRefreshRecentMatches(accountId, matches);
                if (shouldRefresh) {
                    log.info("Recent matches cache outdated. Fetching from OpenDota AccountId={} Target={}", accountId, fetchLimit);
                    matches = fetchRecentMatchesWithPaging(accountId, fetchLimit);
                    if (!matches.isEmpty()) {
                        cache.saveRecentMatches(accountId, matches);
                    }
                } else {
                    log.info("Recent matches cache is up-to-date for {} ({})", accountId, matches.size());
                }
            }
        }

        matches.sort(Comparator.comparingInt(RecentMatch::getStartTime).reversed());

        List<MatchAnalysisResult> results = new ArrayList<>();
        for (RecentMatch match : matches) {
            MatchAnalysisResult result = analyzeMatch(match, accountId, requestParse, onlyPos1);
            if (result != null) {
                results.add(result);
            } else {
                log.debug("Match {} filtered out (onlyPos1={})", match.getMatchId(), onlyPos1);
            }
        }

        List<MatchAnalysisResult> selected = MatchSelection.selectDesired(results, desiredCount, onlyPos1);
        if (onlyPos1 && selected.size() < desiredCount) {
            log.warn("OnlyPos1 desired {} but only {} found in last {} matches.", desiredCount, selected.size(), matches.size());
        }

        return selected;
    }

    private boolean shouldRefreshRecentMatches(long accountId, List<RecentMatch> cached) {
        try {
            List<RecentMatch> latest = client.getRecentMatches(accountId, 1);
            if (latest.isEmpty()) {
                log.warn("OpenDota returned no recent matches for {}", accountId);
                return false;
            }
            long latestId = latest.get(0).getMatchId();
            return cached.stream().noneMatch(m -> m.getMatchId() == latestId);
        } catch (Exception ex) {
            log.warn("Failed to check latest match. Using cache.", ex);
            return false;
        }
    }

    private List<RecentMatch> fetchRecentMatchesWithPaging(long accountId, int fetchLimit) {
        List<RecentMatch> results = new ArrayList<>();
        int pageSize = 100;
        int offset = 0;

        while (results.size() < fetchLimit) {
            int take = Math.min(pageSize, fetchLimit - results.size());
            List<RecentMatch> batch = client.getPlayerMatches(accountId, take, offset, 7);
            if (batch.isEmpty()) break;
            results.addAll(batch);
            offset += batch.size();
            if (batch.size() < take) break;
        }

        return results.stream()
                .collect(Collectors.toMap(RecentMatch::getMatchId, m -> m, (a, b) -> a))
                .values().stream()
                .collect(Collectors.toList());
    }

    public MatchAnalysisResult analyzeMatch(RecentMatch match, long accountId, boolean requestParse, boolean onlyPos1) {
        heroData.ensureLoaded();
        MatchDetail detail = cache.getMatchDetail(match.getMatchId(), Duration.ofDays(7));
        boolean fromCache = detail != null;
        if (detail == null) {
            if (cacheOnly) {
                log.warn("Match cache miss and cache-only enabled. Skipping OpenDota.");
                return null;
            } else {
                if (requestParse) {
                    client.requestParse(match.getMatchId());
                }
                log.info("Match cache miss for {}, fetching from OpenDota", match.getMatchId());
                detail = client.getMatchDetail(match.getMatchId());
                if (detail != null) {
                    cache.saveMatchDetail(match.getMatchId(), detail);
                }
            }
        } else {
            log.info("Match cache hit for {}", match.getMatchId());
        }

        if (detail == null || detail.getPlayers() == null || detail.getPlayers().isEmpty()) {
            log.warn("Match detail missing or unparsed for {}", match.getMatchId());
            return onlyPos1 ? null : buildUnparsedResult(match);
        }

        int accountId32 = (int) accountId;
        PlayerDetail player = detail.getPlayers().stream()
                .filter(p -> p.getAccountId() != null && p.getAccountId() == accountId32)
                .findFirst().orElse(null);
        if (player == null) {
            log.warn("Player {} not found in match {}", accountId, match.getMatchId());
            return onlyPos1 ? null : buildUnparsedResult(match);
        }

        boolean isPos1 = isLikelyPosition1(player, detail.getPlayers(), match.getDuration());
        if (onlyPos1 && !isPos1) {
            return null;
        }

        boolean isRadiant = match.getPlayerSlot() < 128;
        boolean won = (isRadiant && match.isRadiantWin()) || (!isRadiant && !match.isRadiantWin());

        String[] pickResult = analyzePickRound(detail, player, isRadiant);
        String pickRound = pickResult[0];
        int pickIndex = Integer.parseInt(pickResult[1]);

        LaningResult laning = analyzeLaning(player, detail, isRadiant);

        List<String> benchmarkNotes = new ArrayList<>(laning.benchmarkNotes);
        if (!disableBenchmarks && !cacheOnly && (!avoidExternalWhenCached || !fromCache)) {
            BenchmarksResponse heroBenchmarks = cache.getBenchmark(player.getHeroId(), Duration.ofDays(1));
            if (heroBenchmarks == null) {
                heroBenchmarks = client.getHeroBenchmarks(player.getHeroId());
                if (heroBenchmarks != null) {
                    cache.saveBenchmark(player.getHeroId(), heroBenchmarks);
                }
            }
            List<String> heroBenchmarkNotes = buildHeroBenchmarkNotes(match, heroBenchmarks);
            benchmarkNotes.addAll(heroBenchmarkNotes);
        }

        String performance = evaluatePerformance(match);

        List<String> allyHeroes = detail.getPlayers().stream()
                .filter(p -> (p.getPlayerSlot() < 128) == isRadiant)
                .map(p -> heroData.getHeroName(p.getHeroId()))
                .collect(Collectors.toList());
        List<Integer> allyHeroIds = detail.getPlayers().stream()
                .filter(p -> (p.getPlayerSlot() < 128) == isRadiant)
                .map(PlayerDetail::getHeroId)
                .collect(Collectors.toList());
        List<String> enemyHeroes = detail.getPlayers().stream()
                .filter(p -> (p.getPlayerSlot() < 128) != isRadiant)
                .map(p -> heroData.getHeroName(p.getHeroId()))
                .collect(Collectors.toList());
        List<Integer> enemyHeroIds = detail.getPlayers().stream()
                .filter(p -> (p.getPlayerSlot() < 128) != isRadiant)
                .map(PlayerDetail::getHeroId)
                .collect(Collectors.toList());

        int teamTowerDamage = detail.getPlayers().stream()
                .filter(p -> (p.getPlayerSlot() < 128) == isRadiant)
                .mapToInt(PlayerDetail::getTowerDamage)
                .sum();

        MistakesResult mistakes = detectMistakes(match, player, enemyHeroes, laning.netWorthDiff, laning.context, teamTowerDamage);
        List<InventorySnapshot> inventoryTimeline = buildInventoryTimeline(player, match.getDuration(), heroData);

        MatchAnalysisResult result = new MatchAnalysisResult();
        result.setMatchId(match.getMatchId());
        result.setHeroId(match.getHeroId());
        result.setHeroName(heroData.getHeroName(match.getHeroId()));
        result.setWon(won);
        result.setResultText(won ? "胜利" : "失败");
        result.setLaneRole(player.getLaneRole() != null ? player.getLaneRole() : -1);
        result.setPosition1(isPos1);
        result.setPickRound(pickRound);
        result.setPickIndex(pickIndex);
        result.setLaneResult(laning.result);
        result.setLaneNetWorthDiff10(laning.netWorthDiff);
        result.setLaneOpponentHero(laning.laneOpponentHero);
        result.setLaneOpponentHeroId(laning.laneOpponentHeroId);
        result.setLaneAllyHeroes(laning.laneAllyHeroes);
        result.setLaneEnemyHeroes(laning.laneEnemyHeroes);
        result.setLaneAllyHeroIds(laning.laneAllyHeroIds);
        result.setLaneEnemyHeroIds(laning.laneEnemyHeroIds);
        result.setLaneMatchup(laning.laneMatchup);
        result.setLaneKills(laning.laneKills);
        result.setLaneDeaths(laning.laneDeaths);
        result.setPlayerDenies10(laning.playerDenies10);
        result.setEnemyDenies10(laning.enemyDenies10);
        result.setLaningDetails(laning.laningDetails);
        result.setBenchmarkNotes(benchmarkNotes);
        result.setPerformanceRating(performance);
        result.setMistakes(mistakes.mistakes);
        result.setSuggestions(mistakes.suggestions);

        Map<String, String> stats = new LinkedHashMap<>();
        stats.put("KDA", match.getKills() + "/" + match.getDeaths() + "/" + match.getAssists());
        stats.put("补刀/反补", match.getLastHits() + "/" + match.getDenies());
        stats.put("GPM/XPM", match.getGoldPerMin() + "/" + match.getXpPerMin());
        stats.put("时长", (match.getDuration() / 60) + "分钟");
        stats.put("等级", String.valueOf(match.getLevel()));
        result.setStatistics(stats);

        result.setAllyHeroes(allyHeroes);
        result.setAllyHeroIds(allyHeroIds);
        result.setEnemyHeroes(enemyHeroes);
        result.setEnemyHeroIds(enemyHeroIds);
        result.setInventoryTimeline(inventoryTimeline);

        return result;
    }

    private MatchAnalysisResult buildUnparsedResult(RecentMatch match) {
        boolean won = (match.getPlayerSlot() < 128 && match.isRadiantWin())
                || (match.getPlayerSlot() >= 128 && !match.isRadiantWin());

        MatchAnalysisResult result = new MatchAnalysisResult();
        result.setMatchId(match.getMatchId());
        result.setHeroId(match.getHeroId());
        result.setHeroName(heroData.getHeroName(match.getHeroId()));
        result.setWon(won);
        result.setResultText(won ? "胜利" : "失败");
        result.setLaneRole(-1);
        result.setPosition1(false);
        result.setPickRound("未知");
        result.setPickIndex(-1);
        result.setLaneResult("对局尚未解析");
        result.setLaneNetWorthDiff10(0);
        result.setLaneOpponentHero("未知");
        result.setLaningDetails(List.of("对局尚未解析，无法生成对线期细节"));
        result.setBenchmarkNotes(List.of("对局尚未解析，无法生成基准对比"));
        result.setPerformanceRating(evaluatePerformance(match));
        result.setMistakes(List.of("对局尚未解析，建议稍后重试"));
        result.setSuggestions(List.of("OpenDota 需要时间解析回放文件"));

        Map<String, String> stats = new LinkedHashMap<>();
        stats.put("KDA", match.getKills() + "/" + match.getDeaths() + "/" + match.getAssists());
        stats.put("补刀/反补", match.getLastHits() + "/" + match.getDenies());
        stats.put("GPM/XPM", match.getGoldPerMin() + "/" + match.getXpPerMin());
        stats.put("时长", (match.getDuration() / 60) + "分钟");
        stats.put("等级", String.valueOf(match.getLevel()));
        result.setStatistics(stats);

        result.setAllyHeroes(new ArrayList<>());
        result.setAllyHeroIds(new ArrayList<>());
        result.setEnemyHeroes(new ArrayList<>());
        result.setEnemyHeroIds(new ArrayList<>());
        result.setLaneAllyHeroIds(new ArrayList<>());
        result.setLaneEnemyHeroIds(new ArrayList<>());
        result.setInventoryTimeline(new ArrayList<>());

        return result;
    }

    private String[] analyzePickRound(MatchDetail detail, PlayerDetail player, boolean isRadiant) {
        if (detail.getPicksBans() == null || detail.getPicksBans().isEmpty()) {
            return new String[]{"未知", "-1"};
        }

        List<PickBan> picks = detail.getPicksBans().stream()
                .filter(PickBan::isPick)
                .sorted(Comparator.comparingInt(PickBan::getOrder))
                .collect(Collectors.toList());

        int team = isRadiant ? 0 : 1;
        int pickIndex = -1;
        for (int i = 0; i < picks.size(); i++) {
            if (picks.get(i).getHeroId() == player.getHeroId() && picks.get(i).getTeam() == team) {
                pickIndex = i;
                break;
            }
        }

        if (pickIndex < 0) {
            return new String[]{"未知", "-1"};
        }

        String round;
        if (pickIndex <= 1) round = "第1轮";
        else if (pickIndex <= 5) round = "第2轮";
        else round = "第3轮";

        return new String[]{round, String.valueOf(pickIndex + 1)};
    }

    // --- Laning Analysis ---

    private static class LaningResult {
        String result;
        int netWorthDiff;
        String laneOpponentHero;
        int laneOpponentHeroId;
        List<String> laningDetails;
        List<String> benchmarkNotes;
        LaningContext context;
        List<String> laneAllyHeroes;
        List<String> laneEnemyHeroes;
        List<Integer> laneAllyHeroIds;
        List<Integer> laneEnemyHeroIds;
        String laneMatchup;
        int laneKills;
        int laneDeaths;
        int playerDenies10;
        int enemyDenies10;
    }

    private static class LaningContext {
        int netWorthDiff5;
        int netWorthDiff10;
        int lastHitsDiff5;
        int lastHitsDiff10;
        int xpDiff5;
        int xpDiff10;
        int playerLastHits10;
        String trend;
    }

    private static class LaneParticipants {
        List<PlayerDetail> allies;
        List<PlayerDetail> enemies;

        LaneParticipants(List<PlayerDetail> allies, List<PlayerDetail> enemies) {
            this.allies = allies;
            this.enemies = enemies;
        }
    }

    private LaningResult analyzeLaning(PlayerDetail player, MatchDetail detail, boolean isRadiant) {
        LaneParticipants participants = findLaneParticipants(player, detail, isRadiant);
        PlayerDetail primaryEnemy = participants.enemies.stream()
                .max(Comparator.comparingInt(PlayerDetail::getGoldPerMin))
                .orElse(null);

        int playerNetworth10 = getNetWorthAt(player, 10);
        int enemyNetworth10 = primaryEnemy == null ? 0 : getNetWorthAt(primaryEnemy, 10);
        int diff = playerNetworth10 - enemyNetworth10;

        String resultStr;
        if (diff >= 700) resultStr = "线优 (10分钟净值差 +" + diff + ")";
        else if (diff <= -700) resultStr = "线劣 (10分钟净值差 " + diff + ")";
        else resultStr = "均势 (10分钟净值差 " + diff + ")";

        String laneOpponentHero = primaryEnemy == null ? "未知" : heroData.getHeroName(primaryEnemy.getHeroId());
        int laneOpponentHeroId = primaryEnemy == null ? 0 : primaryEnemy.getHeroId();

        String playerHeroName = heroData.getHeroName(player.getHeroId());
        List<String> laneAllyHeroes = participants.allies.stream()
                .map(a -> heroData.getHeroName(a.getHeroId()))
                .collect(Collectors.toList());
        List<Integer> laneAllyHeroIds = participants.allies.stream()
                .map(PlayerDetail::getHeroId)
                .collect(Collectors.toList());
        List<String> laneEnemyHeroes = participants.enemies.stream()
                .map(e -> heroData.getHeroName(e.getHeroId()))
                .collect(Collectors.toList());
        List<Integer> laneEnemyHeroIds = participants.enemies.stream()
                .map(PlayerDetail::getHeroId)
                .collect(Collectors.toList());

        String allySide = Stream.concat(Stream.of(playerHeroName), laneAllyHeroes.stream())
                .collect(Collectors.joining(" + "));
        String enemySide = laneEnemyHeroes.isEmpty() ? "未知" : String.join(" + ", laneEnemyHeroes);
        String laneMatchup = allySide + " vs " + enemySide;

        // Lane kills/deaths (before 10 min = 600s)
        int laneKills = countLaneKills(player, participants.enemies, 600);
        for (PlayerDetail ally : participants.allies) {
            laneKills += countLaneKills(ally, participants.enemies, 600);
        }
        int laneDeaths = countLaneDeaths(player, participants.enemies, 600);
        for (PlayerDetail ally : participants.allies) {
            laneDeaths += countLaneDeaths(ally, participants.enemies, 600);
        }

        // Denies comparison at 10 min
        int playerDenies10 = getDeniesAt(player, 10);
        for (PlayerDetail ally : participants.allies) {
            playerDenies10 += getDeniesAt(ally, 10);
        }
        int enemyDenies10 = 0;
        for (PlayerDetail enemy : participants.enemies) {
            enemyDenies10 += getDeniesAt(enemy, 10);
        }

        List<String> laningDetails = new ArrayList<>();
        List<String> benchmarkNotes = new ArrayList<>();
        LaningContext context = buildLaningContext(player, primaryEnemy, detail);

        laningDetails.add("对线组合：" + laneMatchup);
        laningDetails.add("5分钟：净值差 " + formatDiff(context.netWorthDiff5) + "，补刀差 " + formatDiff(context.lastHitsDiff5) + "，经验差 " + formatDiff(context.xpDiff5));
        laningDetails.add("10分钟：净值差 " + formatDiff(context.netWorthDiff10) + "，补刀差 " + formatDiff(context.lastHitsDiff10) + "，经验差 " + formatDiff(context.xpDiff10));
        laningDetails.add("反补对比（10分钟）：己方 " + playerDenies10 + " vs 对手 " + enemyDenies10);
        laningDetails.add("对线击杀：己方 " + laneKills + " 次击杀，被杀 " + laneDeaths + " 次");
        laningDetails.add("走势：" + context.trend);

        int[] benchmark = getRoleBenchmarks(player.getLaneRole());
        if (benchmark != null) {
            if (context.playerLastHits10 < benchmark[0]) {
                benchmarkNotes.add("优秀玩家基准（估算）：10分钟补刀目标 " + benchmark[0] + "，当前 " + context.playerLastHits10);
            } else {
                benchmarkNotes.add("优秀玩家基准（估算）：10分钟补刀目标 " + benchmark[0] + "，当前已达标");
            }
        }

        LaningResult lr = new LaningResult();
        lr.result = resultStr;
        lr.netWorthDiff = diff;
        lr.laneOpponentHero = laneOpponentHero;
        lr.laneOpponentHeroId = laneOpponentHeroId;
        lr.laningDetails = laningDetails;
        lr.benchmarkNotes = benchmarkNotes;
        lr.context = context;
        lr.laneAllyHeroes = laneAllyHeroes;
        lr.laneEnemyHeroes = laneEnemyHeroes;
        lr.laneAllyHeroIds = laneAllyHeroIds;
        lr.laneEnemyHeroIds = laneEnemyHeroIds;
        lr.laneMatchup = laneMatchup;
        lr.laneKills = laneKills;
        lr.laneDeaths = laneDeaths;
        lr.playerDenies10 = playerDenies10;
        lr.enemyDenies10 = enemyDenies10;
        return lr;
    }

    private static LaneParticipants findLaneParticipants(PlayerDetail player, MatchDetail detail, boolean isRadiant) {
        List<PlayerDetail> allPlayers = detail.getPlayers() != null ? detail.getPlayers() : new ArrayList<>();
        List<PlayerDetail> allies = allPlayers.stream()
                .filter(p -> (p.getPlayerSlot() < 128) == isRadiant && p.getPlayerSlot() != player.getPlayerSlot())
                .collect(Collectors.toList());
        List<PlayerDetail> enemies = allPlayers.stream()
                .filter(p -> (p.getPlayerSlot() < 128) != isRadiant)
                .collect(Collectors.toList());

        if (player.getLane() > 0) {
            List<PlayerDetail> laneAllies = allies.stream().filter(a -> a.getLane() == player.getLane()).collect(Collectors.toList());
            List<PlayerDetail> laneEnemies = enemies.stream().filter(e -> e.getLane() == player.getLane()).collect(Collectors.toList());
            if (!laneEnemies.isEmpty()) {
                return new LaneParticipants(laneAllies, laneEnemies);
            }
        }

        // Fallback: highest GPM enemy
        PlayerDetail fallbackEnemy = enemies.stream()
                .max(Comparator.comparingInt(PlayerDetail::getGoldPerMin))
                .orElse(null);
        return new LaneParticipants(new ArrayList<>(),
                fallbackEnemy != null ? List.of(fallbackEnemy) : new ArrayList<>());
    }

    private static int getDeniesAt(PlayerDetail player, int minute) {
        if (player.getDeniesT() != null && !player.getDeniesT().isEmpty()) {
            int index = Math.min(Math.max(minute, 0), player.getDeniesT().size() - 1);
            return player.getDeniesT().get(index);
        }
        return 0;
    }

    private int countLaneKills(PlayerDetail player, List<PlayerDetail> laneEnemies, int maxTime) {
        if (player.getKillsLog() == null || player.getKillsLog().isEmpty()) return 0;

        Set<String> enemyKeys = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (PlayerDetail enemy : laneEnemies) {
            String key = heroData.getHeroKey(enemy.getHeroId());
            if (key != null && !key.isEmpty()) {
                enemyKeys.add(key);
                enemyKeys.add("npc_dota_hero_" + key);
            }
        }

        return (int) player.getKillsLog().stream()
                .filter(k -> k.getTime() <= maxTime && enemyKeys.contains(k.getKey()))
                .count();
    }

    private int countLaneDeaths(PlayerDetail player, List<PlayerDetail> laneEnemies, int maxTime) {
        String playerKey = heroData.getHeroKey(player.getHeroId());
        if (playerKey == null || playerKey.isEmpty()) return 0;

        Set<String> playerKeys = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        playerKeys.add(playerKey);
        playerKeys.add("npc_dota_hero_" + playerKey);

        int deaths = 0;
        for (PlayerDetail enemy : laneEnemies) {
            if (enemy.getKillsLog() == null) continue;
            deaths += (int) enemy.getKillsLog().stream()
                    .filter(k -> k.getTime() <= maxTime && playerKeys.contains(k.getKey()))
                    .count();
        }
        return deaths;
    }

    private static int getNetWorthAt(PlayerDetail player, int minute) {
        if (player.getGoldT() != null && !player.getGoldT().isEmpty()) {
            int index = Math.min(Math.max(minute, 0), player.getGoldT().size() - 1);
            return player.getGoldT().get(index);
        }
        return (int) Math.round((double) player.getGoldPerMin() * minute);
    }

    private static boolean isLikelyPosition1(PlayerDetail player, List<PlayerDetail> teamPlayers, int durationSeconds) {
        List<int[]> team = teamPlayers.stream()
                .map(p -> new int[]{
                        p.getAccountId() != null ? p.getAccountId() : 0,
                        p.getPlayerSlot(),
                        p.getGoldPerMin(),
                        p.getLastHits()
                })
                .collect(Collectors.toList());
        return PositionClassifier.isPosition1(
                player.getLaneRole() != null ? player.getLaneRole() : -1,
                player.getPlayerSlot(),
                durationSeconds,
                player.getGoldPerMin(),
                player.getLastHits(),
                player.getAccountId() != null ? player.getAccountId() : 0,
                team);
    }

    private static String evaluatePerformance(RecentMatch match) {
        double kda = match.getDeaths() > 0
                ? (double) (match.getKills() + match.getAssists()) / match.getDeaths()
                : match.getKills() + match.getAssists();
        int minutes = Math.max(1, match.getDuration() / 60);
        double csPerMin = (double) match.getLastHits() / minutes;

        int score = 0;
        if (kda >= 3) score += 2;
        else if (kda >= 2) score += 1;

        if (csPerMin >= 6) score += 2;
        else if (csPerMin >= 4) score += 1;

        if (match.getHeroDamage() > match.getDuration() * 350) score += 1;

        if (score >= 4) return "⭐⭐⭐ 出色表现";
        if (score >= 2) return "⭐⭐ 良好表现";
        return "⭐ 需要改进";
    }

    // --- Mistakes Detection ---

    private static class MistakesResult {
        List<String> mistakes;
        List<String> suggestions;

        MistakesResult(List<String> mistakes, List<String> suggestions) {
            this.mistakes = mistakes;
            this.suggestions = suggestions;
        }
    }

    private MistakesResult detectMistakes(RecentMatch match, PlayerDetail player, List<String> enemyHeroes,
                                           int laneDiff, LaningContext laneContext, int teamTowerDamage) {
        List<String> mistakes = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        int minutes = Math.max(1, match.getDuration() / 60);
        double csPerMin = (double) match.getLastHits() / minutes;
        boolean isSupport = isSupportLike(player, match);

        if (laneDiff <= -700 && match.getGoldPerMin() < 450) {
            mistakes.add("对线劣势后经济持续落后");
            suggestions.add("10分钟后更早换线/打野/推对面优势路，避免死守己方优势路");
        }

        if (laneContext.netWorthDiff5 <= -350 && laneContext.netWorthDiff10 <= -700) {
            mistakes.add("对线前5分钟被压制（5分钟净值差 " + laneContext.netWorthDiff5 + "）");
            suggestions.add("开局补给与站位更保守，争取拉野或呼叫支援稳住线权");
        }

        if (laneContext.netWorthDiff5 >= 400 && laneContext.netWorthDiff10 <= -300) {
            mistakes.add("5-10分钟对线优势被反超");
            suggestions.add("领先时减少冒进，注意敌方支援与TP反打");
        }

        if (laneContext.lastHitsDiff10 <= -8 && laneContext.xpDiff10 <= -400) {
            mistakes.add("对线期补刀与经验同步落后");
            suggestions.add("优先保障补刀与经验线，必要时通过拉野与换线止损");
        }

        if (match.getDeaths() >= 6 && csPerMin < 4) {
            mistakes.add("阵亡过多导致关键发育期断档");
            suggestions.add("优先保命与稳定打钱，再寻找安全参战窗口");
        }

        if (match.getHeroDamage() < match.getDuration() * 300) {
            mistakes.add("参团输出偏低");
            suggestions.add("中期关注关键团战时机，避免长时间脱节");
        }

        if (shouldFlagLowPushContribution(match, player, isSupport, teamTowerDamage)) {
            mistakes.add("推进贡献不足");
            suggestions.add("中后期主动推动线权和塔压力，扩大优势或扳回节奏");
        }

        Integer bkbTime = getItemPurchaseTime(player, "black_king_bar");
        long disableCount = enemyHeroes.stream().filter(DISABLE_HERO_NAMES::contains).count();
        if (bkbTime != null && bkbTime > 1500 && disableCount >= 2) {
            mistakes.add("BKB 出得偏晚（" + (bkbTime / 60) + "分）");
            suggestions.add("对面控制较多时尽量提前做出 BKB");
        }

        if (mistakes.isEmpty()) {
            mistakes.add("暂无明显关键错误");
            suggestions.add("继续保持当前节奏与决策");
        }

        return new MistakesResult(mistakes, suggestions);
    }

    private LaningContext buildLaningContext(PlayerDetail player, PlayerDetail enemy, MatchDetail detail) {
        LaningContext ctx = new LaningContext();
        if (enemy == null) {
            ctx.playerLastHits10 = getLastHitsAt(player, 10, detail.getDuration());
            ctx.trend = "未知";
            return ctx;
        }

        ctx.netWorthDiff5 = getNetWorthAt(player, 5) - getNetWorthAt(enemy, 5);
        ctx.netWorthDiff10 = getNetWorthAt(player, 10) - getNetWorthAt(enemy, 10);
        ctx.lastHitsDiff5 = getLastHitsAt(player, 5, detail.getDuration()) - getLastHitsAt(enemy, 5, detail.getDuration());
        ctx.lastHitsDiff10 = getLastHitsAt(player, 10, detail.getDuration()) - getLastHitsAt(enemy, 10, detail.getDuration());
        ctx.xpDiff5 = getXpAt(player, 5, detail.getDuration()) - getXpAt(enemy, 5, detail.getDuration());
        ctx.xpDiff10 = getXpAt(player, 10, detail.getDuration()) - getXpAt(enemy, 10, detail.getDuration());
        ctx.playerLastHits10 = getLastHitsAt(player, 10, detail.getDuration());
        ctx.trend = describeLaningTrend(ctx.netWorthDiff5, ctx.netWorthDiff10);

        return ctx;
    }

    private static int getLastHitsAt(PlayerDetail player, int minute, int durationSeconds) {
        if (player.getLastHitsT() != null && !player.getLastHitsT().isEmpty()) {
            int index = Math.min(Math.max(minute, 0), player.getLastHitsT().size() - 1);
            return player.getLastHitsT().get(index);
        }
        int minutes = Math.max(1, durationSeconds / 60);
        return (int) Math.round((double) player.getLastHits() / minutes * minute);
    }

    private static int getXpAt(PlayerDetail player, int minute, int durationSeconds) {
        if (player.getXpT() != null && !player.getXpT().isEmpty()) {
            int index = Math.min(Math.max(minute, 0), player.getXpT().size() - 1);
            return player.getXpT().get(index);
        }
        return (int) Math.round((double) player.getXpPerMin() * minute);
    }

    private static String describeLaningTrend(int netDiff5, int netDiff10) {
        if (netDiff5 >= 400 && netDiff10 <= -300) return "5-10分钟被反超";
        if (netDiff5 <= -400 && netDiff10 >= 200) return "5-10分钟回稳/反超";
        if (netDiff10 >= 700) return "持续线优";
        if (netDiff10 <= -700) return "持续线劣";
        return "基本均势";
    }

    private static String formatDiff(int value) {
        return value >= 0 ? "+" + value : String.valueOf(value);
    }

    private static int[] getRoleBenchmarks(Integer laneRole) {
        if (laneRole == null) return null;
        switch (laneRole) {
            case 1: return new int[]{45};
            case 2: return new int[]{50};
            case 3: return new int[]{35};
            case 4: return new int[]{15};
            case 5: return new int[]{15};
            default: return null;
        }
    }

    private static boolean isSupportLike(PlayerDetail player, RecentMatch match) {
        if (player.getLaneRole() != null && (player.getLaneRole() == 4 || player.getLaneRole() == 5)) return true;

        int minutes = Math.max(1, match.getDuration() / 60);
        double csPerMin = (double) match.getLastHits() / minutes;
        int total = match.getKills() + match.getAssists();
        double assistShare = total == 0 ? 0 : (double) match.getAssists() / total;

        return match.getGoldPerMin() < 420 && csPerMin < 3.5 && assistShare >= 0.6;
    }

    private static boolean shouldFlagLowPushContribution(RecentMatch match, PlayerDetail player,
                                                          boolean isSupport, int teamTowerDamage) {
        if (match.getDuration() < 25 * 60) return false;
        if (teamTowerDamage <= 0 || teamTowerDamage < 2500) return false;

        double share = teamTowerDamage <= 0 ? 0 : (double) player.getTowerDamage() / teamTowerDamage;
        int role = player.getLaneRole() != null ? player.getLaneRole() : 0;
        double threshold;
        switch (role) {
            case 1: threshold = 0.12; break;
            case 2: threshold = 0.10; break;
            case 3: threshold = 0.08; break;
            case 4: case 5: threshold = 0.05; break;
            default: threshold = isSupport ? 0.05 : 0.08; break;
        }

        if (share >= threshold) return false;

        int minutes = Math.max(1, match.getDuration() / 60);
        double csPerMin = (double) match.getLastHits() / minutes;

        if (isSupport) {
            boolean heroDamageLow = match.getHeroDamage() < match.getDuration() * 200;
            boolean assistsLow = match.getAssists() < Math.max(6, minutes / 2);
            return heroDamageLow && assistsLow;
        }

        if (match.getGoldPerMin() < 420 && csPerMin < 4) {
            return false;
        }

        return true;
    }

    // --- Benchmarks ---

    private static List<String> buildHeroBenchmarkNotes(RecentMatch match, BenchmarksResponse benchmarks) {
        List<String> notes = new ArrayList<>();
        if (benchmarks == null || benchmarks.getResult() == null || benchmarks.getResult().isEmpty()) {
            return notes;
        }

        int minutes = Math.max(1, match.getDuration() / 60);
        double kpm = (double) match.getKills() / minutes;
        double lhpm = (double) match.getLastHits() / minutes;
        double hdpm = (double) match.getHeroDamage() / minutes;
        double hhpm = (double) match.getHeroHealing() / minutes;

        Object[][] comparisons = {
            {"GPM", (double) match.getGoldPerMin(), "gold_per_min"},
            {"XPM", (double) match.getXpPerMin(), "xp_per_min"},
            {"击杀/分钟", kpm, "kills_per_min"},
            {"补刀/分钟", lhpm, "last_hits_per_min"},
            {"输出/分钟", hdpm, "hero_damage_per_min"},
            {"治疗/分钟", hhpm, "hero_healing_per_min"},
            {"塔伤", (double) match.getTowerDamage(), "tower_damage"}
        };

        for (Object[] comp : comparisons) {
            String label = (String) comp[0];
            double value = (Double) comp[1];
            String key = (String) comp[2];

            List<BenchmarkEntry> entries = benchmarks.getResult().get(key);
            if (entries == null || entries.isEmpty()) continue;

            Double percentile = estimatePercentile(value, entries);
            Double p80 = getPercentileValue(entries, 0.8);
            Double p50 = getPercentileValue(entries, 0.5);

            String percentileText = percentile == null ? "未知" : String.format("%.0f%%", percentile);
            String p80Text = p80 == null ? "未知" : String.format("%.0f", p80);
            String p50Text = p50 == null ? "未知" : String.format("%.0f", p50);

            notes.add(String.format("%s：%.0f（英雄分位约 %s，50%%≈%s，优秀(80%%)≈%s）",
                    label, value, percentileText, p50Text, p80Text));
        }

        return notes;
    }

    private static Double estimatePercentile(double value, List<BenchmarkEntry> entries) {
        if (entries.isEmpty()) return null;
        List<BenchmarkEntry> sorted = entries.stream()
                .sorted(Comparator.comparingDouble(BenchmarkEntry::getValue))
                .collect(Collectors.toList());
        BenchmarkEntry match = null;
        for (BenchmarkEntry entry : sorted) {
            if (value >= entry.getValue()) {
                match = entry;
            } else {
                break;
            }
        }
        return match == null ? sorted.get(0).getPercentile() * 100 : match.getPercentile() * 100;
    }

    private static Double getPercentileValue(List<BenchmarkEntry> entries, double percentile) {
        if (entries.isEmpty()) return null;
        return entries.stream()
                .min(Comparator.comparingDouble(e -> Math.abs(e.getPercentile() - percentile)))
                .map(BenchmarkEntry::getValue)
                .orElse(null);
    }

    private static Integer getItemPurchaseTime(PlayerDetail player, String itemKey) {
        if (player.getPurchaseLog() == null) return null;
        return player.getPurchaseLog().stream()
                .filter(p -> itemKey.equalsIgnoreCase(p.getKey()))
                .min(Comparator.comparingInt(PurchaseLogEntry::getTime))
                .map(PurchaseLogEntry::getTime)
                .orElse(null);
    }

    // --- Inventory Timeline ---

    public static List<InventorySnapshot> buildInventoryTimeline(PlayerDetail player, int durationSeconds, HeroDataCache heroData) {
        if (player.getPurchaseLog() == null || player.getPurchaseLog().isEmpty()) {
            List<InventoryItem> items = buildInventoryFromSlots(player, heroData);
            if (items.isEmpty()) {
                return new ArrayList<>();
            }
            return List.of(new InventorySnapshot(Math.max(0, durationSeconds), items));
        }

        List<PurchaseLogEntry> purchases = player.getPurchaseLog().stream()
                .sorted(Comparator.comparingInt(PurchaseLogEntry::getTime))
                .collect(Collectors.toList());

        List<InventorySnapshot> timeline = new ArrayList<>();
        List<InventoryItem> inventory = new ArrayList<>();
        List<Integer> checkpoints = buildTimeCheckpoints(durationSeconds, 60);

        int index = 0;
        for (int time : checkpoints) {
            while (index < purchases.size() && purchases.get(index).getTime() <= time) {
                applyPurchase(inventory, purchases.get(index).getKey(), heroData);
                index++;
            }
            timeline.add(new InventorySnapshot(time, new ArrayList<>(inventory)));
        }

        return timeline;
    }

    private static List<Integer> buildTimeCheckpoints(int durationSeconds, int stepSeconds) {
        List<Integer> checkpoints = new ArrayList<>();
        int last = Math.max(0, durationSeconds);
        for (int t = 0; t <= last; t += stepSeconds) {
            checkpoints.add(t);
        }
        if (checkpoints.isEmpty() || checkpoints.get(checkpoints.size() - 1) != last) {
            checkpoints.add(last);
        }
        return checkpoints;
    }

    private static void applyPurchase(List<InventoryItem> inventory, String key, HeroDataCache heroData) {
        if (key == null || key.isBlank()) return;

        String lower = key.toLowerCase();
        if (lower.equals("recipe") || lower.startsWith("recipe_")) return;
        if (lower.startsWith("ward_") || lower.startsWith("smoke") || lower.startsWith("dust") || lower.startsWith("tpscroll")) return;

        InventoryItem item = buildInventoryItem(lower, heroData);
        if (item == null) return;

        removeComponentsForItem(inventory, item.getKey(), heroData);

        boolean exists = inventory.stream().anyMatch(i -> i.getKey().equals(item.getKey()));
        if (!exists) {
            inventory.add(item);
        }

        if (inventory.size() > 9) {
            inventory.remove(0);
        }
    }

    private static List<InventoryItem> buildInventoryFromSlots(PlayerDetail player, HeroDataCache heroData) {
        int[] itemIds = {
                player.getItem0(), player.getItem1(), player.getItem2(),
                player.getItem3(), player.getItem4(), player.getItem5(),
                player.getBackpack0(), player.getBackpack1(), player.getBackpack2(),
                player.getItemNeutral()
        };

        List<InventoryItem> items = new ArrayList<>();
        for (int itemId : itemIds) {
            if (itemId <= 0) continue;

            String[] keyOut = {""};
            if (heroData.tryGetItemKeyById(itemId, keyOut) && !keyOut[0].isBlank()) {
                InventoryItem item = buildInventoryItem(keyOut[0], heroData);
                if (item != null && items.stream().noneMatch(i -> i.getKey().equals(item.getKey()))) {
                    items.add(item);
                }
                continue;
            }

            String fallbackKey = "item_" + itemId;
            if (items.stream().noneMatch(i -> i.getKey().equals(fallbackKey))) {
                items.add(new InventoryItem(fallbackKey, "物品" + itemId, ""));
            }
        }

        return items;
    }

    private static InventoryItem buildInventoryItem(String key, HeroDataCache heroData) {
        String normalized = normalizeItemKey(key);
        if (normalized == null || normalized.isBlank()) return null;

        String name = formatItemName(normalized);
        String img = "";
        ItemConstants constants = heroData.getItemConstants(normalized);
        if (constants != null) {
            if (constants.getDisplayName() != null && !constants.getDisplayName().isBlank()) {
                name = constants.getDisplayName();
            }
            if (constants.getImg() != null && !constants.getImg().isBlank()) {
                img = "https://cdn.opendota.com" + constants.getImg();
            }
        }

        return new InventoryItem(normalized, name, img);
    }

    private static void removeComponentsForItem(List<InventoryItem> inventory, String itemKey, HeroDataCache heroData) {
        ItemConstants constants = heroData.getItemConstants(itemKey);
        if (constants == null || constants.getComponents() == null) return;

        for (String component : constants.getComponents()) {
            if (component == null || component.isBlank()) continue;
            String normalized = normalizeItemKey(component);
            if (normalized == null || normalized.isBlank()) continue;
            if (normalized.equals("recipe") || normalized.startsWith("recipe_")) continue;

            for (int i = 0; i < inventory.size(); i++) {
                if (inventory.get(i).getKey().equals(normalized)) {
                    inventory.remove(i);
                    break;
                }
            }
        }
    }

    private static String normalizeItemKey(String key) {
        if (key.startsWith("item_")) return key.substring(5);
        return key;
    }

    private static String formatItemName(String key) {
        if (key == null || key.isBlank()) return "未知物品";
        String[] parts = key.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(parts[i].charAt(0)));
            sb.append(parts[i].substring(1));
        }
        return sb.toString();
    }
}
