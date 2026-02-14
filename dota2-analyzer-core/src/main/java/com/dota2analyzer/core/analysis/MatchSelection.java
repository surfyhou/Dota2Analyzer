package com.dota2analyzer.core.analysis;

import com.dota2analyzer.core.model.analysis.MatchAnalysisResult;

import java.util.ArrayList;
import java.util.List;

public final class MatchSelection {

    private MatchSelection() {}

    public static List<MatchAnalysisResult> selectDesired(List<MatchAnalysisResult> analyses, int desiredCount, boolean onlyPos1) {
        if (desiredCount <= 0) return new ArrayList<>();

        if (onlyPos1) {
            List<MatchAnalysisResult> result = new ArrayList<>();
            for (MatchAnalysisResult a : analyses) {
                if (a.isPosition1()) {
                    result.add(a);
                    if (result.size() >= desiredCount) break;
                }
            }
            return result;
        }

        return new ArrayList<>(analyses.subList(0, Math.min(desiredCount, analyses.size())));
    }
}
