package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.common.medication.MedicationCancelReason;
import com.swampd.superficialtrauma.common.medication.MedicationType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ClientMedicationState {
    private static ActiveMedication activeMedication;

    private ClientMedicationState() {
    }

    public static void started(int patientEntityId, MedicationType type, long endsGameTime) {
        activeMedication = new ActiveMedication(patientEntityId, type, endsGameTime);
    }

    public static void cancelled(MedicationCancelReason reason) {
        activeMedication = null;
        showActionBar(Component.translatable(reason.translationKey()));
    }

    public static void completed(MedicationType type) {
        activeMedication = null;
    }

    public static boolean isActive() {
        return activeMedication != null;
    }

    public static ActiveMedication activeMedication() {
        return activeMedication;
    }

    public static float remainingSeconds() {
        Minecraft minecraft = Minecraft.getInstance();
        if (activeMedication == null || minecraft.level == null) {
            return 0.0F;
        }
        return Math.max(0L, activeMedication.endsGameTime() - minecraft.level.getGameTime()) / 20.0F;
    }

    public static void clear() {
        activeMedication = null;
    }

    private static void showActionBar(Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(message, true);
        }
    }

    public record ActiveMedication(
            int patientEntityId,
            MedicationType type,
            long endsGameTime
    ) {
    }
}
