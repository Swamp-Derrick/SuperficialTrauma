package com.swampd.superficialtrauma.common.body;

public record BodyProgressionResult(
        boolean changed,
        int progressedWounds,
        int healedWounds,
        int expiredDamageWindows,
        int expiredTransientWoundTags,
        float recoveredBasePain,
        float bleedingDamage,
        float infectionChange,
        boolean shockWarningStarted,
        boolean shockWarningCancelled,
        boolean becameIncapacitated,
        boolean bloodOxygenChanged,
        boolean becameCardiacArrest,
        boolean becameBrainDead
) {
    private static final BodyProgressionResult UNCHANGED = new BodyProgressionResult(
            false,
            0,
            0,
            0,
            0,
            0.0F,
            0.0F,
            0.0F,
            false,
            false,
            false,
            false,
            false,
            false
    );

    public static BodyProgressionResult unchanged() {
        return UNCHANGED;
    }
}
