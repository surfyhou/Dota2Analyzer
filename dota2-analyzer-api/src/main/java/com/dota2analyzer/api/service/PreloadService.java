package com.dota2analyzer.api.service;

import com.dota2analyzer.api.dto.PreloadMatchRow;
import com.dota2analyzer.api.dto.PreloadStatus;
import com.dota2analyzer.core.model.opendota.MatchDetail;
import com.dota2analyzer.core.model.opendota.PlayerDetail;
import com.dota2analyzer.core.model.opendota.RecentMatch;
import com.dota2analyzer.core.service.MatchCache;
import com.dota2analyzer.core.service.OpenDotaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class PreloadService {

    private static final Logger log = LoggerFactory.getLogger(PreloadService.class);

    private final OpenDotaClient client;
    private final MatchCache cache;
    private final HeroImageCache heroImages;
    private final ConcurrentHashMap<Long, PreloadStatus> statusMap = new ConcurrentHashMap<>();
    private final ReentrantLock gate = new ReentrantLock();

    public PreloadService(OpenDotaClient client, MatchCache cache, HeroImageCache heroImages) {
        this.client = client;
        this.cache = cache;
        this.heroImages = heroImages;
    }

    public PreloadStatus getStatus(long accountId) {
        return statusMap.computeIfAbsent(accountId, id -> new PreloadStatus(id));
    }

    public PreloadStatus start(long accountId, int count) {
        gate.lock();
        try {
            PreloadStatus status = statusMap.computeIfAbsent(accountId, id -> new PreloadStatus(id));
            if (status.isRunning()) {
                return status;
            }

            status.setRunning(true);
            status.setTotal(count);
            status.setCompleted(0);
            status.setFailed(0);
            status.setLastUpdated(OffsetDateTime.now());
            status.setMessage("正在拉取对局列表");
            status.setMatches(new ArrayList<>());

            Thread.startVirtualThread(() -> runPreload(status, count));
            return status;
        } finally {
            gate.unlock();
        }
    }

    private void runPreload(PreloadStatus status, int count) {
        try {
            List<RecentMatch> matches = fetchMatches(status.getAccountId(), count);
            if (!matches.isEmpty()) {
                cache.saveRecentMatches(status.getAccountId(), matches);
            }
            status.setTotal(matches.size());
            status.setMessage("开始拉取对局详情");

            for (RecentMatch match : matches) {
                try {
                    MatchDetail detail = cache.getMatchDetail(match.getMatchId(), Duration.ofDays(7));
                    if (detail == null) {
                        detail = client.getMatchDetail(match.getMatchId());
                        if (detail != null) {
                            cache.saveMatchDetail(match.getMatchId(), detail);
                        }
                    }

                    if (detail != null && detail.getPlayers() != null) {
                        List<Integer> radiant = new ArrayList<>();
                        List<Integer> dire = new ArrayList<>();
                        for (PlayerDetail p : detail.getPlayers()) {
                            if (p.getPlayerSlot() < 128) radiant.add(p.getHeroId());
                            else dire.add(p.getHeroId());
                        }

                        PreloadMatchRow row = new PreloadMatchRow();
                        row.setMatchId(match.getMatchId());
                        row.setRadiantWin(detail.isRadiantWin());
                        row.setDuration(detail.getDuration());
                        row.setStartTime(match.getStartTime());
                        row.setRadiantHeroes(radiant);
                        row.setDireHeroes(dire);
                        status.getMatches().add(row);

                        for (int heroId : radiant) {
                            heroImages.ensureHeroImage(heroId);
                        }
                        for (int heroId : dire) {
                            heroImages.ensureHeroImage(heroId);
                        }
                    }

                    status.setCompleted(status.getCompleted() + 1);
                } catch (Exception ex) {
                    status.setFailed(status.getFailed() + 1);
                    log.warn("Preload match failed {}", match.getMatchId(), ex);
                }

                status.setLastUpdated(OffsetDateTime.now());
                status.setMessage("已完成 " + status.getCompleted() + "/" + status.getTotal());

                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            status.setMessage("完成");
        } catch (Exception ex) {
            log.error("Preload failed AccountId={}", status.getAccountId(), ex);
            status.setMessage("失败");
        } finally {
            status.setRunning(false);
            status.setLastUpdated(OffsetDateTime.now());
        }
    }

    private List<RecentMatch> fetchMatches(long accountId, int count) {
        List<RecentMatch> results = new ArrayList<>();
        int pageSize = 100;
        int offset = 0;

        while (results.size() < count) {
            int take = Math.min(pageSize, count - results.size());
            List<RecentMatch> batch = client.getPlayerMatches(accountId, take, offset, 7);
            if (batch.isEmpty()) break;
            results.addAll(batch);
            offset += batch.size();
            if (batch.size() < take) break;
        }

        return results.stream()
                .collect(Collectors.toMap(RecentMatch::getMatchId, m -> m, (a, b) -> a))
                .values().stream()
                .sorted((a, b) -> Integer.compare(b.getStartTime(), a.getStartTime()))
                .collect(Collectors.toList());
    }
}
