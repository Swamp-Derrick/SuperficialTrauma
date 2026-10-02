package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.init.ModSounds;
import com.swampd.superficialtrauma.common.sound.MedicalActionSound;
import com.swampd.superficialtrauma.common.sound.MedicalActionSoundChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(
        modid = SuperficialTrauma.MOD_ID,
        value = Dist.CLIENT
)
public final class ClientMedicalActionSounds {
    private static final Map<SoundKey, ActionSound> ACTIVE_SOUNDS = new HashMap<>();

    private ClientMedicalActionSounds() {
    }

    public static void start(
            int actorEntityId,
            MedicalActionSoundChannel channel,
            MedicalActionSound soundType
    ) {
        SoundKey key = new SoundKey(actorEntityId, channel);
        stop(actorEntityId, channel);
        ActionSound sound = new ActionSound(actorEntityId, soundType, key);
        ACTIVE_SOUNDS.put(key, sound);
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    public static void stop(int actorEntityId, MedicalActionSoundChannel channel) {
        ActionSound existing = ACTIVE_SOUNDS.remove(new SoundKey(actorEntityId, channel));
        if (existing != null) {
            Minecraft.getInstance().getSoundManager().stop(existing);
        }
    }

    public static void playOnce(int actorEntityId, MedicalActionSound soundType) {
        Minecraft.getInstance().getSoundManager().play(new ActionSound(actorEntityId, soundType, null));
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft minecraft = Minecraft.getInstance();
        for (ActionSound sound : ACTIVE_SOUNDS.values()) {
            minecraft.getSoundManager().stop(sound);
        }
        ACTIVE_SOUNDS.clear();
    }

    private static SoundEvent soundEvent(MedicalActionSound soundType) {
        return switch (soundType) {
            case PICKING_UP_BANDAGE -> ModSounds.PICKING_UP_BANDAGE.get();
            case CLOTH_WRAPPING -> ModSounds.CLOTH_WRAPPING.get();
            case PACKING -> ModSounds.PACKING.get();
            case LIQUID_POUCH -> ModSounds.LIQUID_POUCH.get();
            case START_SURGERY -> ModSounds.START_SURGERY.get();
            case FLASHLIGHT_CLICK -> ModSounds.FLASHLIGHT_CLICK.get();
            case PAPER_WORK -> ModSounds.PAPER_WORK.get();
            case TOURNIQUET -> ModSounds.TOURNIQUET.get();
            case ICE_BAG -> ModSounds.ICE_BAG.get();
            case TABLETS -> ModSounds.TABLETS.get();
            case VIAL -> ModSounds.VIAL.get();
            case AMPOULE -> ModSounds.AMPOULE.get();
            case PLASTIC_CONTAINER -> ModSounds.PLASTIC_CONTAINER.get();
            case SYRINGE_START -> ModSounds.SYRINGE_START.get();
            case RESUSCITATION_1 -> ModSounds.RESUSCITATION_1.get();
            case RESUSCITATION_2 -> ModSounds.RESUSCITATION_2.get();
            case CPR -> ModSounds.CPR.get();
        };
    }

    private record SoundKey(int actorEntityId, MedicalActionSoundChannel channel) {
    }

    private static final class ActionSound extends AbstractTickableSoundInstance {
        private final int actorEntityId;
        private final MedicalActionSound soundType;
        private final SoundKey key;
        private int age;

        private ActionSound(int actorEntityId, MedicalActionSound soundType, SoundKey key) {
            super(soundEvent(soundType), SoundSource.PLAYERS, RandomSource.create());
            this.actorEntityId = actorEntityId;
            this.soundType = soundType;
            this.key = key;
            looping = false;
            delay = 0;
            volume = 1.0F;
            pitch = 1.0F;
            relative = false;
            updatePosition();
        }

        @Override
        public void tick() {
            age++;
            if (age > soundType.durationTicks() + 2 || !updatePosition()) {
                stop();
                if (key != null) {
                    ACTIVE_SOUNDS.remove(key, this);
                }
            }
        }

        private boolean updatePosition() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return false;
            }
            Entity actor = minecraft.level.getEntity(actorEntityId);
            if (actor == null || actor.isRemoved()) {
                return false;
            }
            x = actor.getX();
            y = actor.getY() + actor.getBbHeight() * 0.6D;
            z = actor.getZ();
            return true;
        }
    }
}
