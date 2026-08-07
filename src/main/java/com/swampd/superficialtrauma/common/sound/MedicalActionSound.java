package com.swampd.superficialtrauma.common.sound;

import java.util.Locale;

public enum MedicalActionSound {
    PICKING_UP_BANDAGE(20),
    CLOTH_WRAPPING(32),
    PACKING(40),
    LIQUID_POUCH(20),
    START_SURGERY(40),
    FLASHLIGHT_CLICK(7),
    PAPER_WORK(80);

    private final int durationTicks;

    MedicalActionSound(int durationTicks) {
        this.durationTicks = durationTicks;
    }

    public int durationTicks() {
        return durationTicks;
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
