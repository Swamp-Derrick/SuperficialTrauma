package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.init.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(
        modid = SuperficialTrauma.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class ClientDefibrillatorSounds {
    private static final Map<Integer, ChargingSound> CHARGING_BY_ACTOR = new HashMap<>();

    private ClientDefibrillatorSounds() {
    }

    public static void setCharging(int actorEntityId, boolean charging) {
        stopCharging(actorEntityId);
        Minecraft minecraft = Minecraft.getInstance();
        if (!charging || minecraft.level == null || minecraft.level.getEntity(actorEntityId) == null) {
            return;
        }
        ChargingSound sound = new ChargingSound(actorEntityId);
        CHARGING_BY_ACTOR.put(actorEntityId, sound);
        minecraft.getSoundManager().play(sound);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft minecraft = Minecraft.getInstance();
        for (ChargingSound sound : CHARGING_BY_ACTOR.values()) {
            minecraft.getSoundManager().stop(sound);
        }
        CHARGING_BY_ACTOR.clear();
    }

    private static void stopCharging(int actorEntityId) {
        ChargingSound existing = CHARGING_BY_ACTOR.remove(actorEntityId);
        if (existing != null) {
            Minecraft.getInstance().getSoundManager().stop(existing);
        }
    }

    private static final class ChargingSound extends AbstractTickableSoundInstance {
        private final int actorEntityId;

        private ChargingSound(int actorEntityId) {
            super(ModSounds.DEFIBRILLATOR_CHARGING.get(), SoundSource.PLAYERS, RandomSource.create());
            this.actorEntityId = actorEntityId;
            looping = true;
            delay = 0;
            volume = 1.0F;
            pitch = 1.0F;
            relative = false;
            updatePosition();
        }

        @Override
        public void tick() {
            if (!updatePosition()) {
                stop();
                CHARGING_BY_ACTOR.remove(actorEntityId, this);
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
