package com.swampd.superficialtrauma.client.audio;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.audio.WorldHearingProfile;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC10;

import static org.lwjgl.openal.EXTEfx.*;

/** Native resources owned exclusively by Minecraft's sound-library context, not SVC's context. */
public final class WorldAudioEffects {
    private static long device;
    private static long context;
    private static int filter, effect, slot, sendIndex;
    private static boolean attempted, ready;
    private static WorldHearingProfile lastProfile;

    private WorldAudioEffects() {}

    public static synchronized void initialized(long currentDevice, long currentContext) {
        device = currentDevice;
        context = currentContext;
        filter = effect = slot = 0;
        attempted = ready = false;
        lastProfile = null;
    }

    /** Invoked on the sound executor, immediately when a sound starts and when its hearing profile changes. */
    public static synchronized boolean apply(int source, WorldHearingProfile profile) {
        if (!ensureReady()) return false;
        if (!profile.equals(lastProfile)) {
            alFilterf(filter, AL_LOWPASS_GAINHF, profile.highFrequencyGain());
            alAuxiliaryEffectSlotf(slot, AL_EFFECTSLOT_GAIN, profile.echo());
            lastProfile = profile;
        }
        // OpenAL copies filter parameters into the source; reattach after each eased update.
        AL10.alSourcei(source, AL_DIRECT_FILTER, filter);
        AL11.alSource3i(source, AL_AUXILIARY_SEND_FILTER, slot, sendIndex, filter);
        return true;
    }

    public static synchronized void detach(int source) {
        if (!ready) return;
        AL10.alSourcei(source, AL_DIRECT_FILTER, AL_FILTER_NULL);
        AL11.alSource3i(source, AL_AUXILIARY_SEND_FILTER, AL_EFFECTSLOT_NULL, sendIndex, AL_FILTER_NULL);
    }

    public static synchronized void clearTail() {
        if (ready) {
            alAuxiliaryEffectSlotf(slot, AL_EFFECTSLOT_GAIN, 0);
            lastProfile = null;
        }
    }

    private static boolean ensureReady() {
        if (attempted || context == 0) return ready;
        attempted = true;
        if (!AL.getCapabilities().ALC_EXT_EFX || !ALC10.alcIsExtensionPresent(device, "ALC_EXT_EFX")) {
            SuperficialTrauma.LOGGER.warn("Downed world audio: device lacks EFX; retaining volume attenuation without muffling/echo");
            return false;
        }
        try {
            int sends = ALC10.alcGetInteger(device, ALC_MAX_AUXILIARY_SENDS);
            if (sends < 1) throw new IllegalStateException("No auxiliary sound sends available");
            sendIndex = sends - 1; // Leave the usual first send free for other environmental-audio mods.
            filter = alGenFilters();
            alFilteri(filter, AL_FILTER_TYPE, AL_FILTER_LOWPASS);
            alFilterf(filter, AL_LOWPASS_GAIN, 1);
            effect = alGenEffects();
            alEffecti(effect, AL_EFFECT_TYPE, AL_EFFECT_ECHO);
            alEffectf(effect, AL_ECHO_DELAY, 0.16F);
            alEffectf(effect, AL_ECHO_LRDELAY, 0.13F);
            alEffectf(effect, AL_ECHO_DAMPING, 0.80F);
            alEffectf(effect, AL_ECHO_FEEDBACK, 0.20F);
            alEffectf(effect, AL_ECHO_SPREAD, 0);
            slot = alGenAuxiliaryEffectSlots();
            alAuxiliaryEffectSloti(slot, AL_EFFECTSLOT_EFFECT, effect);
            alAuxiliaryEffectSlotf(slot, AL_EFFECTSLOT_GAIN, 0);
            int error = AL10.alGetError();
            if (error != AL10.AL_NO_ERROR) throw new IllegalStateException("OpenAL EFX error " + error);
            ready = true;
            SuperficialTrauma.LOGGER.info("Downed world audio: low-pass and echo enabled on Minecraft sound context");
        } catch (RuntimeException exception) {
            deleteResources();
            SuperficialTrauma.LOGGER.warn("Downed world audio: unable to initialize EFX; retaining volume attenuation", exception);
        }
        return ready;
    }

    /** Library invokes this AFTER all its channels have been destroyed, BEFORE destroying the context. */
    public static synchronized void beforeContextDestroyed(long currentContext) {
        if (context != currentContext) return;
        deleteResources();
        context = device = 0;
        attempted = false;
        lastProfile = null;
    }

    private static void deleteResources() {
        if (slot != 0) alDeleteAuxiliaryEffectSlots(slot);
        if (effect != 0) alDeleteEffects(effect);
        if (filter != 0) alDeleteFilters(filter);
        filter = effect = slot = 0;
        ready = false;
    }
}
