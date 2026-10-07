package com.swampd.superficialtrauma.common.audio;

/** An episode's onset time, never its refreshable recovery deadline, owns the hearing effect. */
public final class ConcussionHearingEnvelope {
    public static final long FULL_TICKS = 20L * 20;
    public static final long END_TICKS = 30L * 20;
    private ConcussionHearingEnvelope() {}

    public static float strength(boolean active, long startedAt, long now) {
        if (!active || startedAt < 0 || now < startedAt) return 0;
        long age = now - startedAt;
        if (age < FULL_TICKS) return 1;
        if (age >= END_TICKS) return 0;
        return (END_TICKS - age) / (float) (END_TICKS - FULL_TICKS);
    }

    public static boolean isNewEpisode(boolean receivedBefore, boolean wasConcussed, boolean isConcussed) {
        // Initial login sync and refreshes are not new injury events.
        return receivedBefore && !wasConcussed && isConcussed;
    }
}
