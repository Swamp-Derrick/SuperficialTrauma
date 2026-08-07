package com.swampd.superficialtrauma.common.entity;

public final class EmptyCorpseLifecycle {
    public static final long NOT_EMPTY = -1L;

    private EmptyCorpseLifecycle() {
    }

    public static Progression advance(
            boolean removalEnabled,
            boolean empty,
            long emptySinceGameTime,
            long currentGameTime,
            long lifetimeTicks
    ) {
        if (!removalEnabled || !empty) {
            return new Progression(NOT_EMPTY, false);
        }

        long normalizedGameTime = Math.max(0L, currentGameTime);
        long normalizedLifetime = Math.max(1L, lifetimeTicks);
        if (emptySinceGameTime < 0L || emptySinceGameTime > normalizedGameTime) {
            return new Progression(normalizedGameTime, false);
        }
        return new Progression(
                emptySinceGameTime,
                normalizedGameTime - emptySinceGameTime >= normalizedLifetime
        );
    }

    public record Progression(long emptySinceGameTime, boolean shouldRemove) {
    }
}
