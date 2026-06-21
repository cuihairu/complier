package io.oddsmaker.jobs.risk;

import java.math.BigDecimal;

public final class RuleConfig {
    private static volatile RuleConfig current;

    public final BigDecimal amountThreshold;
    public final long freqMaxEvents;
    public final BigDecimal velocityMax;
    public final BigDecimal ratioMax;

    static {
        current = new RuleConfig(
                new BigDecimal(System.getProperty("risk.threshold.amount", "100000")),
                Long.parseLong(System.getProperty("risk.frequency.max-events", "1000")),
                new BigDecimal(System.getProperty("risk.velocity.max-amount", "1000000")),
                new BigDecimal(System.getProperty("risk.ratio.max-source-sink", "10"))
        );
    }

    public RuleConfig(BigDecimal amountThreshold, long freqMaxEvents, BigDecimal velocityMax, BigDecimal ratioMax) {
        this.amountThreshold = amountThreshold;
        this.freqMaxEvents = freqMaxEvents;
        this.velocityMax = velocityMax;
        this.ratioMax = ratioMax;
    }

    public static RuleConfig current() { return current; }

    public static void update(RuleConfig rc) {
        if (rc != null) current = rc;
    }
}
