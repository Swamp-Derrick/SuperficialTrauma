package com.swampd.superficialtrauma.common.sound;

import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class MedicalActionSoundService {
    private MedicalActionSoundService() {
    }

    public static void start(
            ServerPlayer actor,
            ServerPlayer patient,
            MedicalActionSoundChannel channel,
            MedicalActionSound sound
    ) {
        ModNetworking.sendMedicalActionSound(actor, actor, channel, sound, true);
        if (patient != null && patient != actor) {
            ModNetworking.sendMedicalActionSound(actor, patient, channel, sound, true);
        }
    }

    public static void stop(
            ServerPlayer actor,
            ServerPlayer patient,
            MedicalActionSoundChannel channel
    ) {
        ModNetworking.stopMedicalActionSound(actor, actor, channel);
        if (patient != null && patient != actor) {
            ModNetworking.stopMedicalActionSound(actor, patient, channel);
        }
    }

    public static void playOnce(
            ServerPlayer actor,
            ServerPlayer patient,
            MedicalActionSound sound
    ) {
        ModNetworking.playMedicalActionSoundOnce(actor, actor, sound);
        if (patient != null && patient != actor) {
            ModNetworking.playMedicalActionSoundOnce(actor, patient, sound);
        }
    }
}
