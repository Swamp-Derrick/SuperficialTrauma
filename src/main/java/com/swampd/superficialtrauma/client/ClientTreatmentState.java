package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.common.treatment.TreatmentCancelReason;
import com.swampd.superficialtrauma.common.treatment.TreatmentAction;
import com.swampd.superficialtrauma.common.treatment.TreatmentProcedure;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public final class ClientTreatmentState {
    private static ActiveTreatment activeTreatment;

    private ClientTreatmentState() {
    }

    public static void started(
            int patientEntityId,
            UUID woundId,
            TreatmentProcedure procedure,
            TreatmentAction action,
            long endsGameTime
    ) {
        activeTreatment = new ActiveTreatment(patientEntityId, woundId, procedure, action, endsGameTime);
    }

    public static void cancelled(TreatmentCancelReason reason) {
        activeTreatment = null;
        showActionBar(Component.translatable(reason.translationKey()));
    }

    public static void completed(TreatmentProcedure procedure, TreatmentAction action) {
        activeTreatment = null;
    }

    public static boolean isActive() {
        return activeTreatment != null;
    }

    public static ActiveTreatment activeTreatment() {
        return activeTreatment;
    }

    public static float remainingSeconds() {
        Minecraft minecraft = Minecraft.getInstance();
        if (activeTreatment == null || minecraft.level == null) {
            return 0.0F;
        }
        return Math.max(0L, activeTreatment.endsGameTime - minecraft.level.getGameTime()) / 20.0F;
    }

    public static void clear() {
        activeTreatment = null;
    }

    private static void showActionBar(Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(message, true);
        }
    }

    public record ActiveTreatment(
            int patientEntityId,
            UUID woundId,
            TreatmentProcedure procedure,
            TreatmentAction action,
            long endsGameTime
    ) {
    }
}
