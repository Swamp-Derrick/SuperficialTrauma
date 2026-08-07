package com.swampd.superficialtrauma.common.medication;

import java.util.Locale;

public enum MedicationCancelReason {
    ACTION,
    DAMAGE,
    SPRINTING,
    MOVEMENT,
    INVALID_TARGET,
    ITEM_MISSING,
    DISCONNECTED;

    public String translationKey() {
        return "message.superficialtrauma.medication.cancelled." + name().toLowerCase(Locale.ROOT);
    }
}
