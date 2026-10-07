package com.swampd.superficialtrauma.common.qte;

/** Bounded latency validation in monotonic real time, independent of TPS or client/server world-clock skew. */
public final class TimingQteServerWindow {
    private static final long READY_TIMEOUT_NANOS = 5_000_000_000L;
    private static final long MIN_DRIFT_NANOS = 500_000_000L;
    private static final long MAX_DRIFT_NANOS = 2_000_000_000L;
    private final TimingQteSnapshot snapshot;
    private final int leadInTicks;
    private final long issuedNanos;
    private final long minimumGraceNanos;
    private boolean ready;
    private long readyNanos;
    private long driftNanos;

    public TimingQteServerWindow(TimingQteSnapshot snapshot, int leadInTicks, int graceTicks, long issuedNanos) {
        this.snapshot = snapshot;
        this.leadInTicks = leadInTicks;
        this.issuedNanos = issuedNanos;
        this.minimumGraceNanos = graceTicks * TimingQteTimeline.NANOS_PER_TICK;
    }

    public boolean ready(long nowNanos) {
        if (ready || nowNanos - issuedNanos < 0 || nowNanos - issuedNanos > READY_TIMEOUT_NANOS) return false;
        ready = true;
        readyNanos = nowNanos;
        // Includes transport plus initial client scheduling. Bound compensation, never clamp a hit to another arc.
        driftNanos = Math.min(MAX_DRIFT_NANOS, Math.max(minimumGraceNanos,
                Math.max(MIN_DRIFT_NANOS, (nowNanos - issuedNanos) * 2 + 250_000_000L)));
        return true;
    }

    public boolean expired(long nowNanos) {
        if (!ready) return nowNanos - issuedNanos > READY_TIMEOUT_NANOS;
        return nowNanos - readyNanos > (leadInTicks + snapshot.successEndTick())
                * TimingQteTimeline.NANOS_PER_TICK + driftNanos;
    }

    public boolean accepts(float elapsedTicks, long nowNanos) {
        if (!ready || !Float.isFinite(elapsedTicks) || elapsedTicks < -leadInTicks
                || elapsedTicks > snapshot.sweepDurationTicks() + 5 || expired(nowNanos)) return false;
        double reportedAge = (leadInTicks + (double) elapsedTicks) * TimingQteTimeline.NANOS_PER_TICK;
        long observedAge = nowNanos - readyNanos;
        return observedAge >= 0 && Math.abs(observedAge - reportedAge) <= driftNanos;
    }
}
