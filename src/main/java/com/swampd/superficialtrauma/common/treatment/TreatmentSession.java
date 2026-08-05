package com.swampd.superficialtrauma.common.treatment;

import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public record TreatmentSession(
        UUID actorId,
        UUID patientId,
        UUID woundId,
        TreatmentProcedure procedure,
        TreatmentAction action,
        long startedGameTime,
        long endsGameTime,
        Vec3 actorStartPosition,
        Vec3 patientStartPosition
) {
    public boolean isSelfTreatment() {
        return actorId.equals(patientId);
    }
}
