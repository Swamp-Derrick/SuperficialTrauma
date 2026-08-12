package com.swampd.superficialtrauma.common.qte;

/** Client-safe description of one generated QTE. Fractions use half-open intervals. */
public record TimingQteSnapshot(
        int sessionId,
        long cursorStartGameTime,
        int sweepDurationTicks,
        float perfectStart,
        float normalStart,
        float successEnd
) {
    public TimingQteSnapshot {
        if (sessionId <= 0 || sweepDurationTicks <= 0) {
            throw new IllegalArgumentException("Invalid QTE session metadata");
        }
        if (perfectStart < 0.0F
                || normalStart <= perfectStart
                || successEnd <= normalStart
                || successEnd >= 1.0F) {
            throw new IllegalArgumentException("Invalid QTE scoring arcs");
        }
    }

    public float elapsedTicksAt(long gameTime, float partialTick) {
        return gameTime + partialTick - cursorStartGameTime;
    }

    public float progressAt(long gameTime, float partialTick) {
        return elapsedTicksAt(gameTime, partialTick) / sweepDurationTicks;
    }

    public float perfectStartTick() {
        return perfectStart * sweepDurationTicks;
    }

    public float normalStartTick() {
        return normalStart * sweepDurationTicks;
    }

    public float successEndTick() {
        return successEnd * sweepDurationTicks;
    }

    public TimingQteResult classifyPress(float elapsedTicks) {
        float progress = elapsedTicks / sweepDurationTicks;
        if (progress < perfectStart) {
            return TimingQteResult.EARLY_FAILURE;
        }
        if (progress < normalStart) {
            return TimingQteResult.PERFECT;
        }
        if (progress < successEnd) {
            return TimingQteResult.SUCCESS;
        }
        return TimingQteResult.MISSED_FAILURE;
    }
}
