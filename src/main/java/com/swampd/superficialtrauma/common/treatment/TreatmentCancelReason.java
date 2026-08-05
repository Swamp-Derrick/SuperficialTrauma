package com.swampd.superficialtrauma.common.treatment;

public enum TreatmentCancelReason {
    ACTION,
    DAMAGE,
    SPRINTING,
    MOVEMENT,
    INVALID_TARGET,
    ITEM_MISSING,
    WOUND_CHANGED,
    DISCONNECTED;

    public String translationKey() {
        return "message.superficialtrauma.treatment.cancelled." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
