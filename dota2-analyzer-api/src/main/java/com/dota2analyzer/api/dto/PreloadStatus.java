package com.dota2analyzer.api.dto;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class PreloadStatus {
    private long accountId;
    private int total;
    private int completed;
    private int failed;
    private boolean running;
    private String message = "未开始";
    private OffsetDateTime lastUpdated = OffsetDateTime.now();
    private List<PreloadMatchRow> matches = new ArrayList<>();

    public PreloadStatus() {}

    public PreloadStatus(long accountId) {
        this.accountId = accountId;
    }

    public long getAccountId() { return accountId; }
    public void setAccountId(long accountId) { this.accountId = accountId; }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public int getCompleted() { return completed; }
    public void setCompleted(int completed) { this.completed = completed; }

    public int getFailed() { return failed; }
    public void setFailed(int failed) { this.failed = failed; }

    public boolean isRunning() { return running; }
    public void setRunning(boolean running) { this.running = running; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public OffsetDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(OffsetDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public List<PreloadMatchRow> getMatches() { return matches; }
    public void setMatches(List<PreloadMatchRow> matches) { this.matches = matches; }
}
