package com.dota2analyzer.core.analysis;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class PositionClassifier {

    private PositionClassifier() {}

    public static boolean isPosition1(int laneRole, int playerSlot, int durationSeconds,
                                       int goldPerMin, int lastHits, int accountId,
                                       List<int[]> team) {
        // team entries: [accountId, playerSlot, goldPerMin, lastHits]
        if (laneRole == 4 || laneRole == 5) return false;

        boolean isRadiant = playerSlot < 128;
        List<int[]> teamList = team.stream()
                .filter(p -> (p[1] < 128) == isRadiant)
                .collect(Collectors.toList());

        int gpmRank = Integer.MAX_VALUE;
        List<int[]> sortedByGpm = teamList.stream()
                .sorted(Comparator.comparingInt((int[] p) -> p[2]).reversed())
                .collect(Collectors.toList());
        for (int i = 0; i < sortedByGpm.size(); i++) {
            if (sortedByGpm.get(i)[0] == accountId) {
                gpmRank = i;
                break;
            }
        }

        int lhRank = Integer.MAX_VALUE;
        List<int[]> sortedByLh = teamList.stream()
                .sorted(Comparator.comparingInt((int[] p) -> p[3]).reversed())
                .collect(Collectors.toList());
        for (int i = 0; i < sortedByLh.size(); i++) {
            if (sortedByLh.get(i)[0] == accountId) {
                lhRank = i;
                break;
            }
        }

        int minutes = Math.max(1, durationSeconds / 60);
        double csPerMin = (double) lastHits / Math.max(1.0, minutes);

        boolean supportLike = goldPerMin < 380 && csPerMin < 3.0;
        if (supportLike) return false;

        if (gpmRank == 0 && lhRank == 0) return true;
        if (gpmRank == 0 && goldPerMin >= 480 && csPerMin >= 4.0) return true;
        if (gpmRank <= 1 && lhRank <= 1 && goldPerMin >= 450) return true;

        return false;
    }
}
