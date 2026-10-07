package com.swampd.superficialtrauma.common.voice;

import com.swampd.superficialtrauma.common.body.BodyLifeState;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DownedVoiceTest {
    private static final UUID SPEAKER = UUID.randomUUID();

    @Test void concussionUsesSameMuffleAndEchoAsDownedAndDoesNotStackWithArrest() {
        short[] input = sine(3000, 48000);
        assertArrayEquals(new VoicechatAudioProcessor().process(SPEAKER, "entity", input, DownedVoiceState.Listening.MUFFLED),
                new VoicechatAudioProcessor().process(SPEAKER, "entity", input, DownedVoiceState.Listening.CLEAR, 1));
        assertArrayEquals(new VoicechatAudioProcessor().process(SPEAKER, "entity", input, DownedVoiceState.Listening.ARREST),
                new VoicechatAudioProcessor().process(SPEAKER, "entity", input, DownedVoiceState.Listening.ARREST, 1));
        double full = rms(new VoicechatAudioProcessor().process(SPEAKER, "entity", input, DownedVoiceState.Listening.CLEAR, 1), 24000);
        double half = rms(new VoicechatAudioProcessor().process(SPEAKER, "entity", input, DownedVoiceState.Listening.CLEAR, .5F), 24000);
        assertTrue(half > full * 10 && half < rms(input, 24000));
    }

    @Test void thirtySecondEndIsBitExactAndOldEchoDoesNotReturnInAnotherEpisode() {
        var processor = new VoicechatAudioProcessor();
        processor.process(SPEAKER, "entity", sine(220, 48000), DownedVoiceState.Listening.CLEAR, 1);
        short[] clear = sine(440, 960);
        assertSame(clear, processor.process(SPEAKER, "entity", clear, DownedVoiceState.Listening.CLEAR, 0));
        assertEquals(0, max(processor.process(SPEAKER, "entity", new short[16000], DownedVoiceState.Listening.CLEAR, 1), 0, 16000));
    }

    @Test void speechGraceAndHearingHaveIndependentExactBoundaries() {
        var start = DownedVoiceState.evaluate(BodyLifeState.INCAPACITATED, 100, 100);
        assertTrue(start.downed()); assertFalse(start.muted());
        assertEquals(DownedVoiceState.Listening.CLEAR, start.listening());
        assertFalse(DownedVoiceState.evaluate(BodyLifeState.INCAPACITATED, 100, 159).muted());
        assertTrue(DownedVoiceState.evaluate(BodyLifeState.INCAPACITATED, 100, 160).muted());
        assertEquals(DownedVoiceState.Listening.CLEAR, DownedVoiceState.evaluate(BodyLifeState.INCAPACITATED, 100, 299).listening());
        assertEquals(DownedVoiceState.Listening.MUFFLED, DownedVoiceState.evaluate(BodyLifeState.INCAPACITATED, 100, 300).listening());
    }

    @Test void arrestAndFibrillationAreQuieterAndDoNotRenewSpeechGrace() {
        for (var life : new BodyLifeState[] {BodyLifeState.CARDIAC_ARREST, BodyLifeState.VENTRICULAR_FIBRILLATION, BodyLifeState.BRAIN_DEAD}) {
            var state = DownedVoiceState.evaluate(life, 100, 320);
            assertTrue(state.muted());
            assertEquals(DownedVoiceState.Listening.ARREST, state.listening());
        }
        assertTrue(DownedVoiceState.evaluate(BodyLifeState.AWAKENING, 100, 320).muted());
        assertEquals(DownedVoiceState.NORMAL, DownedVoiceState.evaluate(BodyLifeState.ACTIVE, 100, 320));
        assertFalse(DownedVoiceState.evaluate(BodyLifeState.INCAPACITATED, 400, 401).muted());
    }

    @Test void healthyAndFirstTenSecondsAreBitExactPassthrough() {
        short[] samples = sine(440, 2_000);
        assertSame(samples, new VoicechatAudioProcessor().process(SPEAKER, "entity", samples, DownedVoiceState.Listening.CLEAR));
    }

    @Test void refreshedPoseDuringContinuousCollapseDoesNotRenewGrace() {
        var downed = DownedVoiceState.evaluate(BodyLifeState.INCAPACITATED, 100, 320);
        var arrest = DownedVoiceState.update(BodyLifeState.CARDIAC_ARREST, 320, 321, downed);
        assertTrue(arrest.muted());
        assertEquals(100, arrest.downedSince());
        var recovered = DownedVoiceState.update(BodyLifeState.ACTIVE, 330, 330, arrest);
        assertEquals(DownedVoiceState.NORMAL, recovered);
        assertFalse(DownedVoiceState.update(BodyLifeState.INCAPACITATED, 340, 341, recovered).muted());
    }

    @Test void impairmentReducesLevelAndArrestReducesItFurther() {
        short[] samples = sine(220, 96_000);
        double original = rms(samples, 48_000);
        double muffled = rms(new VoicechatAudioProcessor().process(SPEAKER, "entity", samples, DownedVoiceState.Listening.MUFFLED), 48_000);
        double arrest = rms(new VoicechatAudioProcessor().process(SPEAKER, "entity", samples, DownedVoiceState.Listening.ARREST), 48_000);
        assertTrue(muffled < original * 0.8 && muffled > original * 0.25);
        assertTrue(arrest < muffled * 0.6 && arrest > original * 0.04);
    }

    @Test void underwaterFilterStronglySuppressesHighFrequencies() {
        double lowRatio = ratio(220);
        double highRatio = ratio(6_000);
        assertTrue(highRatio < lowRatio * 0.12, "High frequencies should be muffled, not merely turned down");
    }

    @Test void echoPersistsAcrossChunksButDoesNotLeakBetweenSpeakersOrChannelTypes() {
        var processor = new VoicechatAudioProcessor();
        processor.process(SPEAKER, "entity", new short[96_000], DownedVoiceState.Listening.MUFFLED);
        short[] impulse = new short[960]; impulse[0] = 30_000;
        short[] copy = impulse.clone();
        processor.process(SPEAKER, "entity", impulse, DownedVoiceState.Listening.MUFFLED);
        assertArrayEquals(copy, impulse, "Input audio belongs to SVC and must not be modified in-place");
        short[] tail = processor.process(SPEAKER, "entity", new short[16_000], DownedVoiceState.Listening.MUFFLED);
        int delay = VoicechatAudioProcessor.ECHO_DELAY_SAMPLES - 960;
        assertTrue(max(tail, delay, delay + 100) > 10, "An audible delayed copy must exist");
        assertEquals(0, max(tail, delay - 500, delay - 200));
        assertEquals(0, max(processor.process(UUID.randomUUID(), "entity", new short[16_000], DownedVoiceState.Listening.MUFFLED), 0, 16_000));
        assertEquals(0, max(processor.process(SPEAKER, "static", new short[16_000], DownedVoiceState.Listening.MUFFLED), 0, 16_000));
    }

    @Test void processingIsContinuousAcrossPacketBoundaries() {
        short[] input = sine(500, 48_000);
        short[] whole = new VoicechatAudioProcessor().process(SPEAKER, "entity", input, DownedVoiceState.Listening.MUFFLED);
        var chunkedProcessor = new VoicechatAudioProcessor();
        short[] chunked = new short[input.length];
        for (int offset = 0; offset < input.length; offset += 960) {
            var chunk = chunkedProcessor.process(SPEAKER, "entity", Arrays.copyOfRange(input, offset, offset + 960), DownedVoiceState.Listening.MUFFLED);
            System.arraycopy(chunk, 0, chunked, offset, 960);
        }
        assertArrayEquals(whole, chunked);
    }

    @Test void transitionDoesNotInstantlyCutVolumeAndRecoveryReturnsToPassthrough() {
        var processor = new VoicechatAudioProcessor();
        short[] input = new short[96_000]; Arrays.fill(input, (short) 10_000);
        processor.process(SPEAKER, "entity", input, DownedVoiceState.Listening.CLEAR);
        short[] result = processor.process(SPEAKER, "entity", input, DownedVoiceState.Listening.MUFFLED);
        assertTrue(result[0] > 9_900);
        processor.process(SPEAKER, "entity", input, DownedVoiceState.Listening.CLEAR);
        processor.process(SPEAKER, "entity", input, DownedVoiceState.Listening.CLEAR);
        assertSame(input, processor.process(SPEAKER, "entity", input, DownedVoiceState.Listening.CLEAR));
    }

    @Test void newAndRestartedUtterancesAreMuffledFromTheFirstPacket() {
        var processor = new VoicechatAudioProcessor();
        short[] input = sine(3_000, 960);
        double original = rms(input, 200);
        var first = processor.process(SPEAKER, "entity", input, DownedVoiceState.Listening.MUFFLED);
        assertTrue(rms(first, 200) < original * 0.02, "A short new utterance must not get clear audio again");
        processor.process(SPEAKER, "entity", new short[0], DownedVoiceState.Listening.MUFFLED);
        var restarted = processor.process(SPEAKER, "entity", input, DownedVoiceState.Listening.MUFFLED);
        assertArrayEquals(first, restarted);
    }

    @Test void endOfSpeechAndDisconnectClearBuffersAndCacheIsBounded() {
        var processor = new VoicechatAudioProcessor();
        for (int i = 0; i < 150; i++) processor.process(new UUID(0, i), "entity", new short[960], DownedVoiceState.Listening.MUFFLED);
        assertEquals(128, processor.channelCount());
        short[] end = new short[0];
        assertSame(end, processor.process(new UUID(0, 149), "entity", end, DownedVoiceState.Listening.MUFFLED));
        assertEquals(127, processor.channelCount());
        processor.clear(); assertEquals(0, processor.channelCount());
    }

    private static short[] sine(double hz, int length) {
        short[] output = new short[length];
        for (int i = 0; i < length; i++) output[i] = (short) (12_000 * Math.sin(2 * Math.PI * hz * i / VoicechatAudioProcessor.SAMPLE_RATE));
        return output;
    }
    private static double ratio(double hz) {
        var input = sine(hz, 96_000);
        return rms(new VoicechatAudioProcessor().process(SPEAKER, "entity", input, DownedVoiceState.Listening.MUFFLED), 48_000) / rms(input, 48_000);
    }
    private static double rms(short[] values, int start) {
        double sum = 0;
        for (int i = start; i < values.length; i++) sum += (double) values[i] * values[i];
        return Math.sqrt(sum / (values.length - start));
    }
    private static int max(short[] values, int start, int end) {
        int peak = 0;
        for (int i = start; i < end; i++) peak = Math.max(peak, Math.abs((int) values[i]));
        return peak;
    }
}
