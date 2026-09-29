package ir.kootletland.ac.model;

import java.time.Instant;
import java.util.Map;

public record CheckResult(String check,double confidence,double violation,Instant timestamp,Map<String,Object> evidence) {
    public CheckResult { if (confidence<0||confidence>1) throw new IllegalArgumentException("confidence"); if (violation<0) throw new IllegalArgumentException("violation"); evidence=Map.copyOf(evidence); }
}
