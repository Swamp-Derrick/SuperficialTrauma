package com.swampd.superficialtrauma.common.medication;

import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public record MedicationSession(
        UUID actorId,
        UUID patientId,
        MedicationType type,
        long startedGameTime,
        long endsGameTime,
        Vec3 actorStartPosition,
        Vec3 patientStartPosition
) {
    public boolean isSelfMedication() {
        return actorId.equals(patientId);
    }
}
