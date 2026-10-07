package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.common.qte.TimingQteResult;
import com.swampd.superficialtrauma.common.qte.TimingQteSnapshot;
import com.swampd.superficialtrauma.common.qte.TimingQteTimeline;
import com.swampd.superficialtrauma.common.init.ModSounds;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

public final class ClientTimingQteState {
    private static final long FAILURE_FLASH_DURATION_NANOS = 550_000_000L;
    private static final float RESULT_VOLUME = 0.5F;
    private static TimingQteSnapshot active;
    private static TimingQteTimeline timeline;
    private static TimingQteResult predictedResult;
    private static boolean submitted;
    private static long failureFlashStartedAtNanos;

    private ClientTimingQteState() {
    }

    public static void start(TimingQteSnapshot snapshot, int leadInTicks) {
        if (active != null && active.sessionId() == snapshot.sessionId()) return;
        active = snapshot;
        timeline = new TimingQteTimeline(leadInTicks);
        predictedResult = null;
        submitted = false;
        failureFlashStartedAtNanos = 0L;
    }

    public static void resolve(int sessionId, TimingQteResult result) {
        if (active == null || active.sessionId() != sessionId) {
            return;
        }
        active = null;
        timeline = null;
        submitted = false;
        // Local feedback is immediate; the server acknowledgement must not play it twice.
        if (result != TimingQteResult.CANCELLED && result != predictedResult) feedback(result);
        predictedResult = null;
    }

    public static TimingQteSnapshot active() {
        return active;
    }

    public static boolean submitted() {
        return submitted;
    }

    public static float failureFlashStrength() {
        if (failureFlashStartedAtNanos == 0L) {
            return 0.0F;
        }
        long elapsedNanos = System.nanoTime() - failureFlashStartedAtNanos;
        if (elapsedNanos >= FAILURE_FLASH_DURATION_NANOS) {
            failureFlashStartedAtNanos = 0L;
            return 0.0F;
        }
        float remaining = 1.0F - elapsedNanos / (float) FAILURE_FLASH_DURATION_NANOS;
        return remaining * remaining;
    }

    public static float presentProgress() {
        if (active == null || timeline == null || submitted) {
            return 0.0F;
        }
        boolean firstFrame = !timeline.started();
        float elapsed = timeline.present(System.nanoTime());
        if (firstFrame) ModNetworking.timingQteReady(active.sessionId());
        if (elapsed >= active.successEndTick()) submit(false, elapsed);
        return active == null ? 0.0F : elapsed / active.sweepDurationTicks();
    }

    public static boolean press() {
        if (active == null || timeline == null || submitted || !timeline.started()) {
            return false;
        }
        submit(true, timeline.inputElapsed(System.nanoTime()));
        return true;
    }

    public static void tick() {
        if (active == null || timeline == null || submitted) {
            return;
        }
        long now = System.nanoTime();
        if (timeline.shouldMiss(active.successEndTick(), now)) {
            submit(false, timeline.elapsed(now));
        }
    }

    public static void clear() {
        active = null;
        timeline = null;
        predictedResult = null;
        submitted = false;
        failureFlashStartedAtNanos = 0L;
    }

    private static void submit(boolean pressed, float elapsedTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (active == null || minecraft.level == null) {
            return;
        }
        submitted = true;
        predictedResult = pressed ? active.classifyPress(elapsedTicks) : TimingQteResult.MISSED_FAILURE;
        feedback(predictedResult);
        ModNetworking.submitTimingQte(active.sessionId(), elapsedTicks, pressed);
    }

    private static void feedback(TimingQteResult result) {
        playResultSound(result);
        if (result.failed()) failureFlashStartedAtNanos = System.nanoTime();
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
                    RESULT_VOLUME
            ));
        }
    }
}
