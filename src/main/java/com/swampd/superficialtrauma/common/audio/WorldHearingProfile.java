package com.swampd.superficialtrauma.common.audio;

import com.swampd.superficialtrauma.common.voice.DownedVoiceState;

/** Values shared immutably with Minecraft's sound executor; no world access from audio threads. */
public record WorldHearingProfile(float gain, float highFrequencyGain, float echo) {
    public static final WorldHearingProfile CLEAR = new WorldHearingProfile(1, 1, 0);
    public static final WorldHearingProfile MUFFLED = new WorldHearingProfile(0.55F, 0.015F, 0.30F);
    public static final WorldHearingProfile ARREST = new WorldHearingProfile(0.22F, 0.003F, 0.30F);

    public static WorldHearingProfile target(DownedVoiceState state, long gameTime) {
        if (!state.downed() || gameTime - state.downedSince() < DownedVoiceState.CLEAR_HEARING_TICKS) return CLEAR;
        return state.listening() == DownedVoiceState.Listening.ARREST ? ARREST : MUFFLED;
    }

    public WorldHearingProfile approach(WorldHearingProfile target) {
        if (close(gain, target.gain) && close(highFrequencyGain, target.highFrequencyGain) && close(echo, target.echo)) return target;
        return new WorldHearingProfile(ease(gain, target.gain), ease(highFrequencyGain, target.highFrequencyGain), ease(echo, target.echo));
    }

    /** Exact linear symptom envelope, not fed back through the downed-state easing. */
    public WorldHearingProfile withConcussion(float strength) {
        float amount = Float.isFinite(strength) ? Math.clamp(strength, 0, 1) : 0;
        if (amount == 0) return this;
        return new WorldHearingProfile(
                Math.min(gain, amount == 1 ? MUFFLED.gain : 1 + (MUFFLED.gain - 1) * amount),
                Math.min(highFrequencyGain, amount == 1 ? MUFFLED.highFrequencyGain : 1 + (MUFFLED.highFrequencyGain - 1) * amount),
                Math.max(echo, MUFFLED.echo * amount));
    }

    private static float ease(float from, float to) { return from + (to - from) * 0.20F; }
    private static boolean close(float left, float right) { return Math.abs(left - right) < 0.0001F; }
}
