package com.swampd.superficialtrauma.common.treatment;

import com.swampd.superficialtrauma.common.sound.MedicalActionSound;

public enum TreatmentPreparationType {
    BANDAGE(MedicalActionSound.PICKING_UP_BANDAGE),
    DEBRIDEMENT(MedicalActionSound.START_SURGERY);

    private final MedicalActionSound sound;

    TreatmentPreparationType(MedicalActionSound sound) {
        this.sound = sound;
    }

    public MedicalActionSound sound() {
        return sound;
    }
}
