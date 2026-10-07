package com.swampd.superficialtrauma.common.audio;

import com.swampd.superficialtrauma.common.body.BodyLifeState;
import com.swampd.superficialtrauma.common.voice.DownedVoiceState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorldHearingTest {
    @Test void worldEffectsStartAfterTenSecondsForEveryDownedStage() {
        for (var life : BodyLifeState.values()) {
            var state = DownedVoiceState.evaluate(life, 100, 299);
            assertSame(WorldHearingProfile.CLEAR, WorldHearingProfile.target(state, 299));
            if (life == BodyLifeState.ACTIVE) continue;
            assertNotEquals(WorldHearingProfile.CLEAR, WorldHearingProfile.target(DownedVoiceState.evaluate(life, 100, 300), 300));
        }
        assertSame(WorldHearingProfile.ARREST, WorldHearingProfile.target(DownedVoiceState.evaluate(BodyLifeState.CARDIAC_ARREST, 100, 400), 400));
    }

    @Test void fadeIsMonotonicAndEventuallyReturnsToIdentity() {
        var profile = WorldHearingProfile.CLEAR;
        for (int i = 0; i < 60; i++) {
            var next = profile.approach(WorldHearingProfile.MUFFLED);
            assertTrue(next.gain() <= profile.gain());
            assertTrue(next.highFrequencyGain() <= profile.highFrequencyGain());
            assertTrue(next.echo() >= profile.echo());
            profile = next;
        }
        assertSame(WorldHearingProfile.MUFFLED, profile);
        for (int i = 0; i < 60; i++) profile = profile.approach(WorldHearingProfile.CLEAR);
        assertSame(WorldHearingProfile.CLEAR, profile);
    }

    @Test void medicalInternalAndUiSoundsAreExempt() {
        for (String path : new String[]{"cpr", "cloth_wrapping", "defibrillator_discharge", "qte_failed", "heartbeat", "heavy_breathing", "vial", "tinnitus"}) {
            assertFalse(external("superficialtrauma:" + path, SoundSource.PLAYERS, false));
        }
        assertFalse(external("minecraft:ui.button.click", SoundSource.MASTER, true));
        assertFalse(external("example:gui.open", SoundSource.PLAYERS, false));
        assertFalse(external("example:menu/click", SoundSource.MASTER, false));
        assertFalse(external("minecraft:item.book.page_turn", SoundSource.MASTER, true));
        assertFalse(external("minecraft:music.overworld.day", SoundSource.MUSIC, true));
    }

    @Test void externalSoundsAreNotExemptMerelyForBeingPlayerOrRelativeAudio() {
        assertTrue(external("minecraft:block.grass.step", SoundSource.PLAYERS, false));
        assertTrue(external("cgm:item.shotgun.fire", SoundSource.PLAYERS, false));
        assertTrue(external("minecraft:entity.generic.explode", SoundSource.HOSTILE, false));
        assertTrue(external("minecraft:weather.rain", SoundSource.WEATHER, false));
        assertTrue(external("minecraft:ambient.underwater.loop", SoundSource.AMBIENT, true));
        assertTrue(external("example:machine.loop", SoundSource.BLOCKS, false));
        assertTrue(external("example:global_alarm", SoundSource.MASTER, false));
    }

    private static boolean external(String id, SoundSource category, boolean relative) {
        return WorldSoundPolicy.isExternal(ResourceLocation.parse(id), category, relative);
    }
}
