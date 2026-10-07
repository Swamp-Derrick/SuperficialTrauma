package com.swampd.superficialtrauma.common.audio;

import com.swampd.superficialtrauma.common.body.SeriousTraumaState;
import com.swampd.superficialtrauma.common.voice.DownedVoiceState;
import com.swampd.superficialtrauma.common.voice.VoicechatStates;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConcussionHearingTest {
    @Test void twentySecondHoldThenExactlyTenSecondLinearFade() {
        for (int age : new int[]{0, 1, 399, 400}) assertEquals(1, strength(age));
        assertEquals(.75, strength(450));
        assertEquals(.5, strength(500));
        assertEquals(.005, strength(599), 1E-7);
        assertEquals(0, strength(600));
        assertEquals(0, strength(3600));
        assertEquals(0, ConcussionHearingEnvelope.strength(false, 100, 200));
        assertEquals(0, ConcussionHearingEnvelope.strength(true, -1, 200));
        assertEquals(0, ConcussionHearingEnvelope.strength(true, 300, 200));
    }
    @Test void injuryRefreshSaveAndOfflinePauseDoNotRestartTheEnvelope() {
        var state = new SeriousTraumaState(); state.configure(true); state.addDowningConcussion(100);
        state.externalInjury(500);
        assertEquals(100, state.concussionStartedAt());
        assertEquals(4100, state.concussionEndsAt());
        assertEquals(.5, ConcussionHearingEnvelope.strength(state.hasConcussion(), state.concussionStartedAt(), 600));
        state.shiftTimers(1000);
        var copy = new SeriousTraumaState(); copy.load(state.save());
        assertEquals(.5, ConcussionHearingEnvelope.strength(copy.hasConcussion(), copy.concussionStartedAt(), 1600));
        copy.externalInjury(1700);
        assertEquals(0, ConcussionHearingEnvelope.strength(copy.hasConcussion(), copy.concussionStartedAt(), 1700));
    }
    @Test void oneOffFeedbackRequiresANewInjuryNotLoginOrRepeatedSnapshot() {
        assertTrue(ConcussionHearingEnvelope.isNewEpisode(true, false, true));
        assertFalse(ConcussionHearingEnvelope.isNewEpisode(false, false, true));
        assertFalse(ConcussionHearingEnvelope.isNewEpisode(true, true, true));
        assertFalse(ConcussionHearingEnvelope.isNewEpisode(true, true, false));
    }
    @Test void exactWorldProfileFadeAndStrongerDownedStateWinsWithoutDoubleAttenuation() {
        assertEquals(WorldHearingProfile.MUFFLED, WorldHearingProfile.CLEAR.withConcussion(1));
        var half = WorldHearingProfile.CLEAR.withConcussion(.5F);
        assertEquals(.775, half.gain(), 1E-6);
        assertEquals(.5075, half.highFrequencyGain(), 1E-6);
        assertEquals(.15, half.echo(), 1E-6);
        assertSame(WorldHearingProfile.CLEAR, WorldHearingProfile.CLEAR.withConcussion(0));
        assertEquals(WorldHearingProfile.ARREST, WorldHearingProfile.ARREST.withConcussion(1));
        assertEquals(WorldHearingProfile.MUFFLED, WorldHearingProfile.MUFFLED.withConcussion(.5F));
    }
    @Test void hearingDoesNotChangeSpeechOrMenuPermissions() {
        VoicechatStates.publishClient(DownedVoiceState.NORMAL);
        try {
            VoicechatStates.publishConcussionHearing(1);
            assertFalse(VoicechatStates.client().downed());
            assertFalse(VoicechatStates.client().muted());
            assertEquals(DownedVoiceState.Listening.CLEAR, VoicechatStates.client().listening());
        } finally { VoicechatStates.publishConcussionHearing(0); }
    }
    private static float strength(long age) { return ConcussionHearingEnvelope.strength(true, 100, 100 + age); }
}
