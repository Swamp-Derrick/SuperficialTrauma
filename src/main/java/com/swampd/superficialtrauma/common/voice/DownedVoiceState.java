package com.swampd.superficialtrauma.common.voice;

import com.swampd.superficialtrauma.common.body.BodyLifeState;

/** Immutable view shared with SVC's microphone and playback threads. */
public record DownedVoiceState(boolean downed, boolean muted, Listening listening, long downedSince) {
    public static final long SPEECH_GRACE_TICKS = 3 * 20;
    public static final long CLEAR_HEARING_TICKS = 10 * 20;
    public static final DownedVoiceState NORMAL = new DownedVoiceState(false, false, Listening.CLEAR, -1);

    public static DownedVoiceState update(BodyLifeState life, long poseSince, long gameTime, DownedVoiceState previous) {
        // A refreshed pose / rhythm change during the same collapse must not grant three more seconds.
        long since = previous.downed() ? Math.min(previous.downedSince(), poseSince) : poseSince;
        return evaluate(life, since, gameTime);
    }

    public static DownedVoiceState evaluate(BodyLifeState life, long downedSince, long gameTime) {
        if (life == BodyLifeState.ACTIVE) return NORMAL;
        long elapsed = Math.max(0, gameTime - downedSince);
        boolean arrest = life == BodyLifeState.CARDIAC_ARREST
                || life == BodyLifeState.VENTRICULAR_FIBRILLATION || life == BodyLifeState.BRAIN_DEAD;
        Listening listening = arrest ? Listening.ARREST
                : elapsed >= CLEAR_HEARING_TICKS ? Listening.MUFFLED : Listening.CLEAR;
        return new DownedVoiceState(true, elapsed >= SPEECH_GRACE_TICKS, listening, downedSince);
    }

    public enum Listening {
        CLEAR(1.0, 0.0, 0.0, 600),
        MUFFLED(0.55, 1.0, 1.0, 600),
        ARREST(0.22, 1.0, 1.0, 400);

        public final double gain;
        public final double muffle;
        public final double echo;
        public final double cutoffHz;

        Listening(double gain, double muffle, double echo, double cutoffHz) {
            this.gain = gain;
            this.muffle = muffle;
            this.echo = echo;
            this.cutoffHz = cutoffHz;
        }
    }
}
