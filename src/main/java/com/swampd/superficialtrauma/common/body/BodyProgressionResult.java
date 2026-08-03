package com.swampd.superficialtrauma.common.body;

public record BodyProgressionResult(
        boolean changed,
        int progressedWounds,
        int healedWounds,
        int expiredDamageWindows,
        int expiredTransientWoundTags,
        float recoveredBasePain
) {
    private static final BodyProgressionResult UNCHANGED = new BodyProgressionResult(false, 0, 0, 0, 0, 0.0F);

    public static BodyProgressionResult unchanged() {
        return UNCHANGED;
    }
}
