package com.swampd.superficialtrauma.client.audio;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.audio.WorldHearingProfile;
import com.swampd.superficialtrauma.common.audio.WorldSoundPolicy;
import com.swampd.superficialtrauma.common.voice.DownedVoiceState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;
import net.neoforged.neoforge.client.event.sound.PlayStreamingSourceEvent;
import net.neoforged.neoforge.client.event.sound.SoundEvent;

@EventBusSubscriber(modid = SuperficialTrauma.MOD_ID, value = Dist.CLIENT)
public final class ClientWorldHearing {
    private static volatile WorldHearingProfile profile = WorldHearingProfile.CLEAR;

    private ClientWorldHearing() {}
    public static WorldHearingProfile profile() { return profile; }
    public static void update(DownedVoiceState state, long gameTime) { profile = profile.approach(WorldHearingProfile.target(state, gameTime)); }
    public static void reset() { profile = WorldHearingProfile.CLEAR; }

    @SubscribeEvent public static void staticSound(PlaySoundSourceEvent event) { classify(event); }
    @SubscribeEvent public static void streamingSound(PlayStreamingSourceEvent event) { classify(event); }

    private static void classify(SoundEvent.SoundSourceEvent event) {
        var sound = event.getSound();
        ((WorldAudioChannel) event.getChannel()).superficialTrauma$setExternal(
                WorldSoundPolicy.isExternal(sound.getLocation(), sound.getSource(), sound.isRelative()));
    }
}
