package com.swampd.superficialtrauma;

import com.swampd.superficialtrauma.common.body.*;
import com.swampd.superficialtrauma.common.voice.*;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.events.Event;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Calls the real supplied SVC dispatcher, proving discovery as well as our event registration. */
public final class VoicechatSmokeChecks {
    public static void server(ServerPlayer player) throws ReflectiveOperationException {
        UUID id = player.getUUID();
        try {
            for (long elapsed : new long[]{0, 59, 60, 199, 200}) {
                VoicechatStates.publishServer(id, DownedVoiceState.evaluate(BodyLifeState.INCAPACITATED, 0, elapsed));
                check(microphoneCancelled(id) == (elapsed >= 60), "Microphone grace boundary " + elapsed);
                check(!microphoneCancelled(UUID.randomUUID()), "Healthy bystander must not be muted");
            }
            var body = BodyStateCapability.get(player).orElseThrow();
            long now = player.serverLevel().getGameTime();
            body.incapacitateFromLastDamage(CollapseReason.HEMORRHAGIC_SHOCK, now - 201);
            body.captureDownedPose(new DownedPoseSnapshot(now - 201, 0, DownedPosture.UNSAFE, DownedFallDirection.FADE_ONLY));
            VoicechatStates.forgetServer(id); // Simulate a rejoin: the saved pose must not renew the grace period.
            ServerVoicechatState.refresh(player);
            check(microphoneCancelled(id), "Saved downed pose must still mute after rejoin");
            check(VoicechatStates.server(id).listening() == DownedVoiceState.Listening.MUFFLED, "Saved ten-second pose");
            body.forceRecoverForDebug();
            ServerVoicechatState.refresh(player);
            check(!microphoneCancelled(id), "Recovery must permit speech again");
        } finally { VoicechatStates.forgetServer(id); }
        SuperficialTrauma.LOGGER.info("VOICECHAT SERVER HOOKS PASSED: discovered plugin, exact 3s boundary, independent players, saved pose, recovery");
    }

    public static boolean microphoneCancelled(UUID id) throws ReflectiveOperationException {
        var apiPlayer = proxy(de.maxhenkel.voicechat.api.ServerPlayer.class, (name, args) -> switch (name) {
            case "getUuid" -> id;
            case "getName" -> "VoiceSmoke";
            default -> null;
        });
        var connection = proxy(VoicechatConnection.class, (name, args) -> switch (name) {
            case "getPlayer" -> apiPlayer;
            case "isConnected", "isInstalled" -> true;
            case "isDisabled", "isInGroup" -> false;
            default -> null;
        });
        AtomicBoolean cancelled = new AtomicBoolean();
        var event = proxy(MicrophonePacketEvent.class, (name, args) -> switch (name) {
            case "getSenderConnection" -> connection;
            case "isCancellable" -> true;
            case "isCancelled" -> cancelled.get();
            case "cancel" -> { cancelled.set(true); yield true; }
            default -> null;
        });
        var manager = Class.forName("de.maxhenkel.voicechat.plugins.PluginManager");
        manager.getMethod("dispatchEvent", Class.class, Event.class)
                .invoke(manager.getMethod("instance").invoke(null), MicrophonePacketEvent.class, event);
        return cancelled.get();
    }

    private interface Handler { Object call(String name, Object[] args); }
    private static <T> T proxy(Class<T> type, Handler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (self, method, args) -> {
            return switch (method.getName()) {
                case "toString" -> "Smoke " + type.getSimpleName();
                case "hashCode" -> System.identityHashCode(self);
                case "equals" -> self == args[0];
                default -> handler.call(method.getName(), args);
            };
        }));
    }
    public static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
