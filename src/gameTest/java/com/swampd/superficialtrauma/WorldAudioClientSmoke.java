package com.swampd.superficialtrauma;

import com.mojang.blaze3d.audio.Channel;
import com.swampd.superficialtrauma.client.audio.ClientWorldHearing;
import com.swampd.superficialtrauma.client.audio.WorldAudioEffects;
import com.swampd.superficialtrauma.common.audio.WorldHearingProfile;
import com.swampd.superficialtrauma.common.body.*;
import com.swampd.superficialtrauma.common.voice.VoicechatStates;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;
import net.neoforged.neoforge.client.event.sound.PlayStreamingSourceEvent;
import net.neoforged.neoforge.client.event.sound.SoundEvent;
import org.lwjgl.openal.AL10;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static org.lwjgl.openal.EXTEfx.*;

/** Development-only real OpenAL/context test; very quiet, synthetic fixtures, no microphone. */
@EventBusSubscriber(modid = SuperficialTrauma.MOD_ID, value = Dist.CLIENT)
public final class WorldAudioClientSmoke {
    private static final Map<ProbeSound, Channel> PROBES = new ConcurrentHashMap<>();
    private static final Map<ProbeSound, Float> BASE = new ConcurrentHashMap<>();
    private static CompletableFuture<Void> pending;
    private static int ticks, phase, waitUntil, reloads;

    public static void tick() throws Exception {
        var mc = Minecraft.getInstance();
        if (++ticks > 1_200) throw new AssertionError("World audio smoke timeout at phase " + phase);
        if (pending != null) {
            if (!pending.isDone()) return;
            pending.join();
            pending = null;
        }
        if (ticks < waitUntil) return;
        int previous = phase;
        switch (phase) {
            case 0 -> { playProbes(); phase++; }
            case 1 -> {
                if (PROBES.size() < 4) return;
                pending = checkAudio(WorldHearingProfile.CLEAR, true);
                phase++;
            }
            case 2 -> {
                onServer(player -> {
                    player.setHealth(1);
                    var body = BodyStateCapability.get(player).orElseThrow();
                    long now = player.level().getGameTime();
                    body.incapacitateFromLastDamage(CollapseReason.HEMORRHAGIC_SHOCK, now);
                    body.captureDownedPose(new DownedPoseSnapshot(now, 0, DownedPosture.UNSAFE, DownedFallDirection.FADE_ONLY));
                });
                phase++; waitUntil = ticks + 80;
            }
            case 3 -> {
                check(VoicechatStates.client().downed(), "Player must be downed");
                check(ClientWorldHearing.profile().equals(WorldHearingProfile.CLEAR), "World sounds clear before ten seconds");
                pending = checkAudio(WorldHearingProfile.CLEAR, false);
                phase++; waitUntil = ticks + 170;
            }
            case 4 -> {
                pending = checkAudio(WorldHearingProfile.MUFFLED, false);
                phase++;
            }
            case 5 -> {
                // A newly started sound must receive the existing impairment immediately.
                var late = new ProbeSound("minecraft:block.grass.step", SoundSource.PLAYERS, false, true);
                float base = BASE.entrySet().stream().filter(e -> e.getKey().getLocation().getPath().equals("block.grass.step"))
                        .findFirst().orElseThrow().getValue();
                BASE.put(late, base);
                mc.getSoundManager().play(late);
                phase++; waitUntil = ticks + 5;
            }
            case 6 -> {
                check(PROBES.size() == 5, "New external sound must start");
                pending = checkAudio(WorldHearingProfile.MUFFLED, false);
                onServer(player -> BodyStateCapability.get(player).orElseThrow()
                        .forceCardiacRhythmForDebug(BodyLifeState.CARDIAC_ARREST, player.level().getGameTime()));
                phase++; waitUntil = ticks + 60;
            }
            case 7 -> { pending = checkAudio(WorldHearingProfile.ARREST, false); phase++; }
            case 8 -> {
                engine().reload(); // Destroy/recreate actual OpenAL context while hearing is impaired.
                reloads++;
                PROBES.clear(); BASE.clear();
                playProbes();
                phase++; waitUntil = ticks + 40;
            }
            case 9 -> {
                if (PROBES.size() < 4) return;
                pending = checkAudio(WorldHearingProfile.ARREST, true);
                phase = reloads < 3 ? 8 : 10;
            }
            case 10 -> {
                // Updating a category / ticking source must not progressively compound attenuation.
                ChannelAccess access = (ChannelAccess) field(engine(), "channelAccess");
                pending = new CompletableFuture<>();
                access.executeOnChannels(channels -> {
                    try {
                        for (var entry : PROBES.entrySet()) {
                            for (int repeat = 0; repeat < 5; repeat++) entry.getValue().setVolume(BASE.get(entry.getKey()));
                        }
                        pending.complete(null);
                    } catch (Throwable error) { pending.completeExceptionally(error); }
                });
                phase++;
            }
            case 11 -> { pending = checkAudio(WorldHearingProfile.ARREST, false); phase++; }
            case 12 -> {
                onServer(player -> { BodyStateCapability.get(player).orElseThrow().forceRecoverForDebug(); player.setHealth(20); });
                phase++; waitUntil = ticks + 70;
            }
            case 13 -> { pending = checkAudio(WorldHearingProfile.CLEAR, false); phase++; }
            case 14 -> {
                check(ClientWorldHearing.profile().equals(WorldHearingProfile.CLEAR), "Recovery ends at identity");
                SuperficialTrauma.LOGGER.info("WORLD AUDIO CLIENT SMOKE PASSED: existing/new static and streaming sounds, medical/UI exemptions, ten-second gate, arrest, gain updates, context reload, recovery, native EFX without AL errors");
                mc.stop(); phase++;
            }
        }
        if (phase != previous) SuperficialTrauma.LOGGER.info("World audio smoke completed phase {} at tick {}", previous, ticks);
    }

    static int probeCount() { return PROBES.size(); }
    static void playProbes() {
        var sounds = new ProbeSound[]{
                new ProbeSound("minecraft:block.grass.step", SoundSource.PLAYERS, false, true),
                new ProbeSound("minecraft:music_disc.13", SoundSource.RECORDS, false, true),
                new ProbeSound("superficialtrauma:heartbeat", SoundSource.PLAYERS, false, false),
                new ProbeSound("minecraft:ui.button.click", SoundSource.MASTER, true, false)};
        for (var sound : sounds) Minecraft.getInstance().getSoundManager().play(sound);
    }

    static CompletableFuture<Void> checkAudio(WorldHearingProfile expected, boolean captureBase) throws Exception {
        CompletableFuture<Void> result = new CompletableFuture<>();
        ChannelAccess access = (ChannelAccess) field(engine(), "channelAccess");
        access.executeOnChannels(channels -> {
            try {
                for (var entry : PROBES.entrySet()) {
                    var probe = entry.getKey();
                    var channel = entry.getValue();
                    int source = (int) field(channel, "source");
                    float actual = AL10.alGetSourcef(source, AL10.AL_GAIN);
                    float ratio = probe.external ? expected.gain() : 1;
                    if (captureBase) BASE.put(probe, actual / ratio);
                    check(BASE.get(probe) > 0, "Fixture is audible to the sound engine");
                    check(Math.abs(actual - BASE.get(probe) * ratio) < 0.000002F, "Incorrect gain for " + probe.getLocation() + ": " + actual);
                    boolean filtered = (boolean) field(channel, "superficialTrauma$filtered");
                    check(filtered == (probe.external && expected != WorldHearingProfile.CLEAR), "Incorrect EFX attachment for " + probe.getLocation());
                }
                if (expected != WorldHearingProfile.CLEAR) {
                    int filter = (int) staticField(WorldAudioEffects.class, "filter");
                    int slot = (int) staticField(WorldAudioEffects.class, "slot");
                    check(Math.abs(alGetFilterf(filter, AL_LOWPASS_GAINHF) - expected.highFrequencyGain()) < 0.0002F, "Native high-frequency gain");
                    check(Math.abs(alGetAuxiliaryEffectSlotf(slot, AL_EFFECTSLOT_GAIN) - expected.echo()) < 0.0002F, "Native echo slot gain");
                }
                check(AL10.alGetError() == AL10.AL_NO_ERROR, "No OpenAL error during hearing effects");
                result.complete(null);
            } catch (Throwable error) { result.completeExceptionally(error); }
        });
        return result;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST) public static void sound(PlaySoundSourceEvent event) { capture(event); }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void stream(PlayStreamingSourceEvent event) { capture(event); }
    private static void capture(SoundEvent.SoundSourceEvent event) {
        if (event.getSound() instanceof ProbeSound probe) PROBES.put(probe, event.getChannel());
    }

    private static SoundEngine engine() throws Exception { return (SoundEngine) field(Minecraft.getInstance().getSoundManager(), "soundEngine"); }
    private static Object field(Object owner, String name) throws Exception {
        var field = owner.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(owner);
    }
    private static Object staticField(Class<?> owner, String name) throws Exception {
        var field = owner.getDeclaredField(name); field.setAccessible(true); return field.get(null);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void onServer(Consumer<ServerPlayer> action) {
        var mc = Minecraft.getInstance(); var id = mc.player.getUUID();
        mc.getSingleplayerServer().execute(() -> {
            var player = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
            action.accept(player);
            ModNetworking.syncBodyState(player); ModNetworking.syncDownedPose(player);
        });
    }

    private static final class ProbeSound extends AbstractTickableSoundInstance {
        private final boolean external;
        private ProbeSound(String id, SoundSource category, boolean relative, boolean external) {
            super(net.minecraft.sounds.SoundEvent.createVariableRangeEvent(ResourceLocation.parse(id)), category, RandomSource.create());
            this.external = external; this.relative = relative;
            volume = 0.005F; looping = true; delay = 0;
            var player = Minecraft.getInstance().player;
            x = player.getX(); y = player.getY(); z = player.getZ();
        }
        @Override public void tick() {}
    }
}
