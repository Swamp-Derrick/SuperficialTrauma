package com.swampd.superficialtrauma.common.sound;

import java.util.Locale;

public enum MedicalActionSound {
    PICKING_UP_BANDAGE(20),
    CLOTH_WRAPPING(32),
    PACKING(40),
    LIQUID_POUCH(20),
    START_SURGERY(40),
    FLASHLIGHT_CLICK(7),
    PAPER_WORK(80),
    TOURNIQUET(40),
    ICE_BAG(20),
    TABLETS(8),
    VIAL(20),
    AMPOULE(20),
    PLASTIC_CONTAINER(27),
    SYRINGE_START(20),
    RESUSCITATION_1(20),
    RESUSCITATION_2(20),
    CPR(6);

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
