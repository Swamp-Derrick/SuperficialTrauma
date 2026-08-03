package com.swampd.superficialtrauma.common.body;

public record WoundProgressionResult(
        boolean changed,
        int progressedWounds,
        int healedWounds,
        int expiredDamageWindows
) {
    private static final WoundProgressionResult UNCHANGED = new WoundProgressionResult(false, 0, 0, 0);

    public static WoundProgressionResult unchanged() {
        return UNCHANGED;
    }
}
