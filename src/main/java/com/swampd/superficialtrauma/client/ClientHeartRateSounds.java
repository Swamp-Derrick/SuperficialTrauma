package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.common.body.BodyLifeState;
import com.swampd.superficialtrauma.common.body.BodyState;
import com.swampd.superficialtrauma.common.init.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class ClientHeartRateSounds {
    private static int previousTachycardiaLevel;
    private static int ticksUntilNextBeat;
    private static BreathingSound breathingSound;

    private ClientHeartRateSounds() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.level == null
                || !ClientBodyState.hasReceivedSnapshot()) {
            clear();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }

        int level = audibleHeartRateLevel(ClientBodyState.snapshot());
        if (level < 0) {
            resetHeartbeatCadence();
            ensureBreathing(minecraft, Math.abs(level));
            return;
        }

        stopBreathing(minecraft);
        if (level == 0) {
            resetHeartbeatCadence();
            return;
        }

        if (level != previousTachycardiaLevel) {
            previousTachycardiaLevel = level;
            ticksUntilNextBeat = 0;
        }
        if (ticksUntilNextBeat > 0) {
            ticksUntilNextBeat--;
            return;
        }

        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                ModSounds.HEARTBEAT.get(),
                heartbeatPitch(level),
                heartbeatVolume(level)
        ));
        ticksUntilNextBeat = heartbeatIntervalTicks(level) - 1;
    }

    public static void clear() {
        resetHeartbeatCadence();
        stopBreathing(Minecraft.getInstance());
    }

    private static int audibleHeartRateLevel(BodyState state) {
        BodyLifeState lifeState = state.lifeState();
        if (lifeState == BodyLifeState.CARDIAC_ARREST
                || lifeState == BodyLifeState.VENTRICULAR_FIBRILLATION
                || lifeState == BodyLifeState.BRAIN_DEAD) {
            return 0;
        }
        return state.effectiveHeartRateLevel();
    }

    private static void ensureBreathing(Minecraft minecraft, int severity) {
        if (breathingSound == null || breathingSound.isStopped()) {
            breathingSound = new BreathingSound(severity);
            minecraft.getSoundManager().play(breathingSound);
            return;
        }
        breathingSound.updateSeverity(severity);
    }

    private static void stopBreathing(Minecraft minecraft) {
        if (breathingSound == null) {
            return;
        }
        minecraft.getSoundManager().stop(breathingSound);
        breathingSound = null;
    }

    private static int heartbeatIntervalTicks(int level) {
        return switch (level) {
            case 1 -> 18;
            case 2 -> 15;
            default -> 12;
        };
    }

    private static float heartbeatPitch(int level) {
        return switch (level) {
            case 1 -> 0.95F;
            case 2 -> 1.03F;
            default -> 1.12F;
        };
    }

    private static float heartbeatVolume(int level) {
        return switch (level) {
            case 1 -> 0.45F;
            case 2 -> 0.65F;
            default -> 0.85F;
        };
    }

    private static void resetHeartbeatCadence() {
        previousTachycardiaLevel = 0;
        ticksUntilNextBeat = 0;
    }

    private static final class BreathingSound extends AbstractTickableSoundInstance {
        private BreathingSound(int severity) {
            super(ModSounds.HEAVY_BREATHING.get(), SoundSource.PLAYERS, RandomSource.create());
            looping = true;
            delay = 0;
            relative = true;
            x = 0.0D;
            y = 0.0D;
            z = 0.0D;
            updateSeverity(severity);
        }

        @Override
        public void tick() {
            if (Minecraft.getInstance().player == null) {
                stop();
            }
        }

        private void updateSeverity(int severity) {
            int clampedSeverity = Math.max(1, Math.min(3, severity));
            volume = switch (clampedSeverity) {
                case 1 -> 0.38F;
                case 2 -> 0.58F;
                default -> 0.78F;
            };
            pitch = switch (clampedSeverity) {
                case 1 -> 1.0F;
                case 2 -> 0.94F;
                default -> 0.88F;
            };
        }
    }
}
