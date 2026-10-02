package com.swampd.superficialtrauma;

import com.swampd.superficialtrauma.client.ClientVoicechatState;
import com.swampd.superficialtrauma.client.ClientBodyState;
import com.swampd.superficialtrauma.common.body.*;
import com.swampd.superficialtrauma.common.voice.*;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Arrays;
import java.util.UUID;
import java.util.function.Consumer;

import static com.swampd.superficialtrauma.VoicechatSmokeChecks.check;

/** No microphone capture or real voice data: synthetic PCM through actual SVC client callbacks. */
public final class VoicechatClientSmoke {
    private static int ticks;
    private static long collapseObservedAt = -1;
    private static int phase;

    public static void tick() throws Exception {
        var mc = Minecraft.getInstance();
        if (++ticks > 900) throw new AssertionError("Voicechat client smoke timed out at phase " + phase);
        int previousPhase = phase;
        switch (phase) {
            case 0 -> {
                check(ModList.get().isLoaded("voicechat"), "Smoke requires supplied SVC runtime JAR");
                clientAudioHooks();
                Screen settings = settings();
                mc.setScreen(settings);
                check(mc.screen == settings, "Healthy player can open SVC settings");
                var saved = ClientBodyState.snapshot().serializeNBT();
                var downed = new BodyState();
                downed.incapacitateFromLastDamage(CollapseReason.HEMORRHAGIC_SHOCK, mc.level.getGameTime());
                ClientBodyState.update(downed.serializeNBT());
                ClientVoicechatState.tick(new ClientTickEvent.Post());
                check(mc.screen == null, "Already-open SVC menu must close on collapse");
                ClientBodyState.update(saved);
                ClientVoicechatState.tick(new ClientTickEvent.Post());
                onServer(player -> {
                    player.setHealth(1);
                    var body = BodyStateCapability.get(player).orElseThrow();
                    long now = player.level().getGameTime();
                    body.incapacitateFromLastDamage(CollapseReason.HEMORRHAGIC_SHOCK, now);
                    body.captureDownedPose(new DownedPoseSnapshot(now, 0, DownedPosture.UNSAFE, DownedFallDirection.FADE_ONLY));
                    ModNetworking.syncBodyState(player);
                    ModNetworking.syncDownedPose(player);
                });
                phase++;
            }
            case 1 -> {
                if (!VoicechatStates.client().downed()) return;
                var state = VoicechatStates.client();
                check(!state.muted() && state.listening() == DownedVoiceState.Listening.CLEAR, "Initial speech and hearing grace");
                mc.setScreen(settings());
                check(!ClientVoicechatState.isVoicechatScreen(mc.screen), "Downed SVC settings opening must be cancelled");
                collapseObservedAt = state.downedSince();
                phase++;
            }
            case 2 -> {
                if (mc.level.getGameTime() - collapseObservedAt < 75) return;
                check(VoicechatStates.client().muted(), "Local microphone must be blocked after three seconds");
                check(VoicechatStates.server(mc.player.getUUID()).muted(), "Server must independently block downed speech");
                check(VoicechatStates.client().listening() == DownedVoiceState.Listening.CLEAR, "Still hear clearly before ten seconds");
                phase++;
            }
            case 3 -> {
                if (mc.level.getGameTime() - collapseObservedAt < 215) return;
                check(VoicechatStates.client().listening() == DownedVoiceState.Listening.MUFFLED, "Muffled after ten seconds");
                onServer(player -> {
                    BodyStateCapability.get(player).orElseThrow().forceCardiacRhythmForDebug(BodyLifeState.CARDIAC_ARREST, player.level().getGameTime());
                    ModNetworking.syncBodyState(player);
                    ModNetworking.syncDownedPose(player);
                });
                phase++;
            }
            case 4 -> {
                if (VoicechatStates.client().listening() != DownedVoiceState.Listening.ARREST) return;
                check(VoicechatStates.client().muted(), "Arrest must not renew grace period");
                onServer(player -> {
                    BodyStateCapability.get(player).orElseThrow().forceRecoverForDebug();
                    player.setHealth(20);
                    ModNetworking.syncBodyState(player);
                    ModNetworking.syncDownedPose(player);
                });
                phase++;
            }
            case 5 -> {
                if (VoicechatStates.client().downed()) return;
                // Wait for the independent downed-pose removal payload too.
                if (com.swampd.superficialtrauma.client.ClientDownedPoses.get(mc.player.getId()).isPresent()) return;
                check(!VoicechatStates.client().muted() && !VoicechatStates.server(mc.player.getUUID()).muted(), "Recovery unmutes both ends");
                Screen settings = settings();
                mc.setScreen(settings);
                check(mc.screen == settings, "Recovery unlocks settings again");
                mc.setScreen(null);
                SuperficialTrauma.LOGGER.info("VOICECHAT CLIENT SMOKE PASSED: real SVC audio callbacks (entity/location/static), microphone cancellation, actual timed downing, menu lock/recovery, arrest");
                mc.stop();
                phase++;
            }
        }
        if (phase != previousPhase) SuperficialTrauma.LOGGER.info("Voicechat client smoke completed phase {} at tick {}", previousPhase, ticks);
    }

    private static Screen settings() throws Exception {
        return (Screen) Class.forName("de.maxhenkel.voicechat.gui.VoiceChatSettingsScreen").getConstructor().newInstance();
    }

    private static void clientAudioHooks() throws Exception {
        var manager = Class.forName("de.maxhenkel.voicechat.plugins.ClientPluginManager");
        Object instance = manager.getMethod("instance").invoke(null);
        short[] audio = new short[48_000];
        for (int i = 0; i < audio.length; i++) audio[i] = (short) (10_000 * Math.sin(2 * Math.PI * 4_000 * i / 48_000));
        var original = VoicechatStates.client();
        try {
            var microphone = manager.getMethod("onClientSound", short[].class, boolean.class);
            VoicechatStates.publishClient(DownedVoiceState.NORMAL);
            check(microphone.invoke(instance, audio, false) == audio, "Normal microphone passthrough");
            VoicechatStates.publishClient(DownedVoiceState.evaluate(BodyLifeState.INCAPACITATED, 0, 60));
            check(microphone.invoke(instance, audio, false) == null, "Muted normal speech");
            check(microphone.invoke(instance, audio, true) == null, "Muted whisper speech");
            var stationary = manager.getMethod("onReceiveStaticClientSound", UUID.class, short[].class);
            check(stationary.invoke(instance, UUID.randomUUID(), audio) == audio, "Clear receive during first ten seconds");
            VoicechatStates.publishClient(DownedVoiceState.evaluate(BodyLifeState.INCAPACITATED, 0, 200));
            check(!Arrays.equals(audio, (short[]) stationary.invoke(instance, UUID.randomUUID(), audio)), "Static/group audio filtered");
            var entity = manager.getMethod("onReceiveEntityClientSound", UUID.class, UUID.class, short[].class, boolean.class, float.class);
            check(!Arrays.equals(audio, (short[]) entity.invoke(instance, UUID.randomUUID(), UUID.randomUUID(), audio, false, 48F)), "Entity audio filtered");
            var location = manager.getMethod("onReceiveLocationalClientSound", UUID.class, short[].class, Vec3.class, float.class);
            check(!Arrays.equals(audio, (short[]) location.invoke(instance, UUID.randomUUID(), audio, Vec3.ZERO, 48F)), "Locational audio filtered");
        } finally { VoicechatStates.publishClient(original); }
    }

    private static void onServer(Consumer<ServerPlayer> action) {
        var mc = Minecraft.getInstance();
        UUID id = mc.player.getUUID();
        mc.getSingleplayerServer().execute(() -> action.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(id)));
    }
}
