package com.swampd.superficialtrauma.common.qte;

/** Local presentation clock. Rendering and input share the exact same sub-tick position. */
public final class TimingQteTimeline {
    public static final long NANOS_PER_TICK = 50_000_000L;
    public static final long MAX_PRESENTED_FRAME_AGE_NANOS = 250_000_000L;
    private final int leadInTicks;
    private boolean started;
    private long startNanos;
    private long presentedNanos;
    private float presentedElapsed;

    public TimingQteTimeline(int leadInTicks) {
        if (leadInTicks < 0 || leadInTicks > 200) throw new IllegalArgumentException("Invalid QTE lead-in");
        this.leadInTicks = leadInTicks;
    }

    public boolean started() { return started; }

    /** Start on the first visible frame, not on packet send time or the world clock. */
    public float present(long nowNanos) {
        if (!started) {
            started = true;
            startNanos = nowNanos;
        }
        presentedElapsed = elapsed(nowNanos);
        presentedNanos = nowNanos;
        return presentedElapsed;
    }

    public float elapsed(long nowNanos) {
        return !started ? -leadInTicks
                : (float) ((nowNanos - startNanos) / (double) NANOS_PER_TICK - leadInTicks);
    }

    public float inputElapsed(long nowNanos) {
        // GLFW input is polled before the next frame. Judge the position the player actually saw.
        return started && nowNanos - presentedNanos <= MAX_PRESENTED_FRAME_AGE_NANOS
                ? presentedElapsed : elapsed(nowNanos);
    }

    public boolean shouldMiss(float successEndTick, long nowNanos) {
        return started && elapsed(nowNanos) >= successEndTick
                && (presentedElapsed >= successEndTick || nowNanos - presentedNanos > MAX_PRESENTED_FRAME_AGE_NANOS);
    }
}
