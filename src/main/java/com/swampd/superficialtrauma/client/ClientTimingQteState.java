package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.common.qte.TimingQteResult;
import com.swampd.superficialtrauma.common.qte.TimingQteSnapshot;
import com.swampd.superficialtrauma.common.init.ModSounds;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

public final class ClientTimingQteState {
    private static final long FAILURE_FLASH_DURATION_MILLIS = 550L;
    private static TimingQteSnapshot active;
    private static boolean submitted;
    private static long failureFlashStartedAtMillis;

    private ClientTimingQteState() {
    }

    public static void start(TimingQteSnapshot snapshot) {
        active = snapshot;
        submitted = false;
        failureFlashStartedAtMillis = 0L;
    }

    public static void resolve(int sessionId, TimingQteResult result) {
        if (active == null || active.sessionId() != sessionId) {
            return;
        }
        active = null;
        submitted = false;
        if (result == TimingQteResult.CANCELLED) {
            return;
        }
        playResultSound(result);
        if (result.failed()) {
            failureFlashStartedAtMillis = System.currentTimeMillis();
        }
    }

    public static TimingQteSnapshot active() {
        return active;
    }

    public static boolean submitted() {
        return submitted;
    }

    public static float failureFlashStrength() {
        if (failureFlashStartedAtMillis <= 0L) {
            return 0.0F;
        }
        long elapsedMillis = System.currentTimeMillis() - failureFlashStartedAtMillis;
        if (elapsedMillis >= FAILURE_FLASH_DURATION_MILLIS) {
            failureFlashStartedAtMillis = 0L;
            return 0.0F;
        }
        float remaining = 1.0F - elapsedMillis / (float) FAILURE_FLASH_DURATION_MILLIS;
        return remaining * remaining;
    }

    public static float progress(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (active == null || minecraft.level == null) {
            return 0.0F;
        }
        return active.progressAt(minecraft.level.getGameTime(), partialTick);
    }

    public static boolean press() {
        if (active == null || submitted) {
            return false;
        }
        submit(true);
        return true;
    }

    public static void tick() {
        if (active == null || submitted) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        float elapsedTicks = active.elapsedTicksAt(minecraft.level.getGameTime(), 0.0F);
        if (elapsedTicks >= active.successEndTick()) {
            submit(false);
        }
    }

    public static void clear() {
        active = null;
        submitted = false;
        failureFlashStartedAtMillis = 0L;
    }

    private static void submit(boolean pressed) {
        Minecraft minecraft = Minecraft.getInstance();
        if (active == null || minecraft.level == null) {
            return;
        }
        submitted = true;
        float elapsedTicks = active.elapsedTicksAt(minecraft.level.getGameTime(), 0.0F);
        ModNetworking.submitTimingQte(active.sessionId(), elapsedTicks, pressed);
    }

    private static void playResultSound(TimingQteResult result) {
        SoundEvent sound = switch (result) {
            case PERFECT -> ModSounds.QTE_PERFECT.get();
            case SUCCESS -> ModSounds.QTE_SUCCESS.get();
            case EARLY_FAILURE, MISSED_FAILURE -> ModSounds.QTE_FAILED.get();
            case CANCELLED -> null;
        };
        if (sound != null) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(
                    sound,
                    1.0F,
                    1.0F
            ));
        }
    }
}
