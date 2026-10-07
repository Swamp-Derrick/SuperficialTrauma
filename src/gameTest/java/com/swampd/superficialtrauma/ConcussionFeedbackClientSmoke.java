package com.swampd.superficialtrauma;

import com.swampd.superficialtrauma.client.ClientBodyState;
import com.swampd.superficialtrauma.client.audio.ClientWorldHearing;
import com.swampd.superficialtrauma.common.audio.WorldHearingProfile;
import com.swampd.superficialtrauma.common.body.*;
import com.swampd.superficialtrauma.common.voice.VoicechatStates;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.client.event.sound.PlayStreamingSourceEvent;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Real client, real SVC dispatcher and native OpenAL; no microphone capture or other people. */
@EventBusSubscriber(modid = SuperficialTrauma.MOD_ID, value = Dist.CLIENT)
public final class ConcussionFeedbackClientSmoke {
    private static int ticks, phase, tinnitusRequests, tinnitusDecoded;
    private static long firstStart;
    private static float previousStrength = 1;
    private static CompletableFuture<Void> pending;

    public static void tick() throws Exception {
        var mc = Minecraft.getInstance();
        if (++ticks > 1400) throw new AssertionError("Concussion feedback timeout at " + phase);
        if (pending != null) { if (!pending.isDone()) return; pending.join(); pending = null; }
        var serious = ClientBodyState.snapshot().seriousTrauma();
        long age = mc.level.getGameTime() - serious.concussionStartedAt();
        int before = phase;
        switch (phase) {
            case 0 -> { WorldAudioClientSmoke.playProbes(); phase++; }
            case 1 -> {
                if (WorldAudioClientSmoke.probeCount() < 4) return;
                pending = WorldAudioClientSmoke.checkAudio(WorldHearingProfile.CLEAR, true); phase++;
            }
            case 2 -> {
                pending = server(player -> {
                    var body = BodyStateCapability.get(player).orElseThrow();
                    long now = player.level().getGameTime();
                    body.configureSeriousTrauma(true, now); body.seriousTrauma().addDowningConcussion(now);
                }); phase++;
            }
            case 3 -> {
                if (!serious.hasConcussion() || age < 25) return;
                firstStart = serious.concussionStartedAt();
                check(tinnitusRequests == 1 && tinnitusDecoded == 1, "One tinnitus onset, real OGG streaming decode");
                check(VoicechatStates.clientConcussionHearing() == 1, "Full initial hearing effect");
                check(ClientWorldHearing.profile().equals(WorldHearingProfile.MUFFLED), "World concussion profile");
                check(!VoicechatStates.client().muted() && !VoicechatStates.client().downed(), "Concussion is not mute/downed");
                audioHooks(true);
                Screen settings = (Screen) Class.forName("de.maxhenkel.voicechat.gui.VoiceChatSettingsScreen").getConstructor().newInstance();
                mc.setScreen(settings); check(mc.screen == settings, "Concussion permits actual SVC settings"); mc.setScreen(null);
                pending = WorldAudioClientSmoke.checkAudio(WorldHearingProfile.MUFFLED, false); phase++;
            }
            case 4 -> {
                if (age < 200) return;
                pending = server(player -> {
                    check(!VoicechatSmokeChecks.microphoneCancelled(player.getUUID()), "Server SVC microphone stays allowed");
                    BodyStateCapability.get(player).orElseThrow().recordExternalInjury(2, player.level().getGameTime(), false);
                }); phase++;
            }
            case 5 -> {
                check(serious.concussionStartedAt() == firstStart, "External injury keeps episode start");
                check(serious.concussionEndsAt() > firstStart + 3600, "Injury refreshes only healing deadline");
                if (age < 390) return;
                check(VoicechatStates.clientConcussionHearing() == 1, "Full strength still at 19.5 seconds");
                check(tinnitusRequests == 1, "No repeated tinnitus on refresh"); phase++;
            }
            case 6 -> {
                float current = VoicechatStates.clientConcussionHearing();
                check(current <= previousStrength, "Fade monotonically decreases"); previousStrength = current;
                if (age < 500) return;
                check(Math.abs(current - .5F) < .03F, "Half strength at 25 seconds, despite later injury");
                var profile = ClientWorldHearing.profile();
                check(Math.abs(profile.gain() - .775F) < .015F && Math.abs(profile.echo() - .15F) < .015F,
                        "World gain and echo fade linearly");
                SuperficialTrauma.LOGGER.info("CONCUSSION FADE age={} strength={} profile={}", age, current, profile); phase++;
            }
            case 7 -> {
                if (age < 605) return;
                check(serious.hasConcussion(), "Concussion itself continues after hearing effect");
                check(VoicechatStates.clientConcussionHearing() == 0, "Hearing envelope ends at 30 seconds");
                audioHooks(false);
                pending = WorldAudioClientSmoke.checkAudio(WorldHearingProfile.CLEAR, false); phase++;
            }
            case 8 -> {
                check(tinnitusRequests == 1, "Exactly one tinnitus during original episode");
                pending = server(player -> BodyStateCapability.get(player).orElseThrow().seriousTrauma().clear()); phase++;
            }
            case 9 -> {
                if (serious.hasConcussion()) return;
                pending = server(player -> BodyStateCapability.get(player).orElseThrow().seriousTrauma().addDowningConcussion(player.level().getGameTime())); phase++;
            }
            case 10 -> {
                if (!serious.hasConcussion() || age < 25) return;
                check(tinnitusRequests == 2 && tinnitusDecoded == 2, "A genuinely new episode plays one new tinnitus");
                check(VoicechatStates.clientConcussionHearing() == 1, "New episode restarts hearing");
                SuperficialTrauma.LOGGER.info("CONCUSSION FEEDBACK CLIENT SMOKE PASSED: 20s hold/10s fade, refreshed healing deadline, once-per-episode decoded tinnitus, static/streaming external sounds, UI/medical exemptions, actual SVC incoming callbacks, normal/whisper and server microphone allowed, SVC settings allowed");
                mc.stop(); phase++;
            }
        }
        if (before != phase) SuperficialTrauma.LOGGER.info("Concussion feedback phase {} completed at tick {}", before, ticks);
    }

    @SubscribeEvent public static void sound(PlaySoundEvent event) {
        if (Boolean.getBoolean("superficialtrauma.concussionFeedbackSmoke") && event.getSound() != null
                && event.getSound().getLocation().toString().equals("superficialtrauma:tinnitus")) tinnitusRequests++;
    }
    @SubscribeEvent public static void stream(PlayStreamingSourceEvent event) {
        if (Boolean.getBoolean("superficialtrauma.concussionFeedbackSmoke")
                && event.getSound().getLocation().toString().equals("superficialtrauma:tinnitus")) tinnitusDecoded++;
    }
    private static void audioHooks(boolean impaired) throws Exception {
        var type = Class.forName("de.maxhenkel.voicechat.plugins.ClientPluginManager");
        Object manager = type.getMethod("instance").invoke(null);
        short[] samples = new short[48000];
        for (int i = 0; i < samples.length; i++) samples[i] = (short)(10000 * Math.sin(2 * Math.PI * 4000 * i / 48000));
        var microphone = type.getMethod("onClientSound", short[].class, boolean.class);
        check(microphone.invoke(manager, samples, false) == samples, "Normal microphone remains bit-exact");
        check(microphone.invoke(manager, samples, true) == samples, "Whisper remains bit-exact");
        var group = type.getMethod("onReceiveStaticClientSound", UUID.class, short[].class);
        var entity = type.getMethod("onReceiveEntityClientSound", UUID.class, UUID.class, short[].class, boolean.class, float.class);
        var location = type.getMethod("onReceiveLocationalClientSound", UUID.class, short[].class, Vec3.class, float.class);
        for (Object result : new Object[]{group.invoke(manager, UUID.randomUUID(), samples),
                entity.invoke(manager, UUID.randomUUID(), UUID.randomUUID(), samples, false, 48F),
                location.invoke(manager, UUID.randomUUID(), samples, Vec3.ZERO, 48F)})
            check(Arrays.equals(samples, (short[])result) != impaired, "Incoming SVC channel filtered only during envelope");
    }
    private interface ServerAction { void accept(ServerPlayer player) throws Exception; }
    private static CompletableFuture<Void> server(ServerAction action) {
        var future = new CompletableFuture<Void>(); var mc = Minecraft.getInstance(); var id = mc.player.getUUID();
        mc.getSingleplayerServer().execute(() -> {
            try {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(id); action.accept(player);
                ModNetworking.syncBodyState(player); future.complete(null);
            } catch (Throwable failure) { future.completeExceptionally(failure); }
        });
        return future;
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
