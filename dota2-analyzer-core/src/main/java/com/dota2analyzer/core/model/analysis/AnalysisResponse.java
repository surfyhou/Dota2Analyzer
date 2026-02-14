package com.dota2analyzer.core.model.analysis;

import java.util.List;

public class AnalysisResponse {
    private AnalysisSummary summary;
    private List<MatchAnalysisResult> matches;

    public AnalysisResponse() {}

    public AnalysisResponse(AnalysisSummary summary, List<MatchAnalysisResult> matches) {
        this.summary = summary;
        this.matches = matches;
    }

    public AnalysisSummary getSummary() { return summary; }
    public void setSummary(AnalysisSummary summary) { this.summary = summary; }

    public List<MatchAnalysisResult> getMatches() { return matches; }
    public void setMatches(List<MatchAnalysisResult> matches) { this.matches = matches; }
}
