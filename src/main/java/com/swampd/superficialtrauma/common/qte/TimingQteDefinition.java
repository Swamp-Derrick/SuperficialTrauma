package com.swampd.superficialtrauma.common.qte;

/** Reusable timing and layout rules for a single-pass circular timing QTE. */
public record TimingQteDefinition(
        int leadInTicks,
        int sweepDurationTicks,
        float earliestPerfectStart,
        float latestPerfectStart,
        float perfectArcWidth,
        float successArcWidth,
        int serverResponseGraceTicks
) {
    public TimingQteDefinition {
        if (leadInTicks < 0 || sweepDurationTicks <= 0 || serverResponseGraceTicks < 0) {
            throw new IllegalArgumentException("QTE tick durations must be non-negative");
        }
        if (earliestPerfectStart < 0.0F
                || latestPerfectStart < earliestPerfectStart
                || perfectArcWidth <= 0.0F
                || successArcWidth <= 0.0F
                || latestPerfectStart + perfectArcWidth + successArcWidth >= 1.0F) {
            throw new IllegalArgumentException("QTE arc fractions must fit inside one sweep");
        }
    }
}
