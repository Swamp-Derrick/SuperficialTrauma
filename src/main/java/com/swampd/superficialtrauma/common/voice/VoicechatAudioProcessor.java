package com.swampd.superficialtrauma.common.voice;

import java.util.LinkedHashMap;
import java.util.UUID;

/** Processes only SVC's 48 kHz mono PCM, before its existing distance/volume controls. */
public final class VoicechatAudioProcessor {
    public static final int SAMPLE_RATE = 48_000;
    public static final int ECHO_DELAY_SAMPLES = SAMPLE_RATE * 160 / 1_000;
    private static final int SECOND_ECHO_SAMPLES = SAMPLE_RATE * 290 / 1_000;
    private static final int MAX_CHANNELS = 128;
    private static final long IDLE_RESET_NANOS = 2_000_000_000L;
    // About 95% of a transition is completed in 0.75 seconds of audio.
    private static final double EASING = 1.0 - Math.exp(-1.0 / (SAMPLE_RATE * 0.25));
    private final LinkedHashMap<Key, Channel> channels = new LinkedHashMap<>(16, 0.75F, true);

    public synchronized short[] process(UUID id, String kind, short[] input, DownedVoiceState.Listening target) {
        if (input == null) return null;
        Key key = new Key(id, kind);
        if (input.length == 0) {
            channels.remove(key);
            return input; // Preserve SVC's end-of-transmission marker.
        }
        long now = System.nanoTime();
        Channel channel = channels.get(key);
        if (channel != null && now - channel.lastAudio > IDLE_RESET_NANOS) {
            channels.remove(key);
            channel = null;
        }
        if (channel == null) {
            if (channels.size() >= MAX_CHANNELS) channels.remove(channels.keySet().iterator().next());
            channel = new Channel(target);
            channels.put(key, channel);
        }
        channel.lastAudio = now;
        if (target == DownedVoiceState.Listening.CLEAR && channel.isClear()) {
            return input; // Keep lightweight continuity, but never modify clear audio.
        }
        if (channel.delay == null) channel.delay = new float[SECOND_ECHO_SAMPLES + 1];
        short[] output = new short[input.length];
        double targetAlpha = 1.0 - Math.exp(-2.0 * Math.PI * target.cutoffHz / SAMPLE_RATE);
        for (int index = 0; index < input.length; index++) {
            channel.gain += EASING * (target.gain - channel.gain);
            channel.muffle += EASING * (target.muffle - channel.muffle);
            channel.echo += EASING * (target.echo - channel.echo);
            channel.alpha += EASING * (targetAlpha - channel.alpha);
            double sample = input[index];
            channel.low1 += channel.alpha * (sample - channel.low1);
            channel.low2 += channel.alpha * (channel.low1 - channel.low2);
            channel.low3 += channel.alpha * (channel.low2 - channel.low3);
            channel.low4 += channel.alpha * (channel.low3 - channel.low4);
            double dry = sample + channel.muffle * (channel.low4 - sample);
            int first = (channel.cursor + channel.delay.length - ECHO_DELAY_SAMPLES) % channel.delay.length;
            int second = (channel.cursor + channel.delay.length - SECOND_ECHO_SAMPLES) % channel.delay.length;
            double echo = channel.delay[first] * 0.30 + channel.delay[second] * 0.12;
            channel.delay[channel.cursor] = (float) dry;
            channel.cursor = (channel.cursor + 1) % channel.delay.length;
            long value = Math.round(channel.gain * (dry + channel.echo * echo));
            output[index] = (short) Math.clamp(value, Short.MIN_VALUE, Short.MAX_VALUE);
        }
        return output;
    }

    public synchronized void clear() { channels.clear(); }
    int channelCount() { return channels.size(); }

    private record Key(UUID id, String kind) {}

    private static final class Channel {
        private float[] delay;
        private int cursor;
        private double low1, low2, low3, low4, gain, muffle, echo, alpha;
        private long lastAudio;

        private Channel(DownedVoiceState.Listening target) {
            // A NEW utterance starts with the current impairment, not a fresh clear-speech grace.
            // An already-playing channel still eases normally when the listening state changes.
            gain = target.gain;
            muffle = target.muffle;
            echo = target.echo;
            alpha = 1.0 - Math.exp(-2.0 * Math.PI * target.cutoffHz / SAMPLE_RATE);
        }

        private boolean isClear() { return Math.abs(1 - gain) < 0.0001 && muffle < 0.0001 && echo < 0.0001; }
    }
}
