package com.swampd.superficialtrauma.common.treatment;

import net.minecraft.world.phys.Vec3;

public final class TreatmentMovementRules {
    public static final double MOVEMENT_TOLERANCE_SQUARED = 0.01D * 0.01D;

    private TreatmentMovementRules() {
    }

    public static boolean interrupts(
            boolean selfTreatment,
            boolean actorSprinting,
            Vec3 actorStart,
            Vec3 actorCurrent,
            Vec3 patientStart,
            Vec3 patientCurrent
    ) {
        if (selfTreatment) {
            return actorSprinting;
        }
        return actorCurrent.distanceToSqr(actorStart) > MOVEMENT_TOLERANCE_SQUARED
                || patientCurrent.distanceToSqr(patientStart) > MOVEMENT_TOLERANCE_SQUARED;
    }
}
