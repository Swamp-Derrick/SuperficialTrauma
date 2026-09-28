package com.swampd.superficialtrauma.common.qte;

/** Shared rules for detailed autopsy and skin-graft surgery. */
public final class MedicalTimingQte {
    public static final float CHANCE_PER_SECOND = 0.13F;
    public static final long ROLL_INTERVAL_TICKS = 20L;
    public static final long COOLDOWN_TICKS = 60L;
    public static final long FINAL_BUFFER_TICKS = 80L;
    public static final TimingQteDefinition DEFINITION = new TimingQteDefinition(
            10, 26, 0.28F, 0.68F, 0.06F, 0.18F, 8);

    private MedicalTimingQte() { }

    public static long adjustedDeadline(long deadline, long now, TimingQteResult result) {
        if (result == TimingQteResult.PERFECT) {
            return Math.max(now, deadline - 40L);
        }
        return result.failed() ? deadline + 100L : deadline;
    }
}
