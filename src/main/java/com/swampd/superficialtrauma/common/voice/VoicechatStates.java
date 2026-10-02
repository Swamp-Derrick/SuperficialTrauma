package com.swampd.superficialtrauma.common.voice;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Audio callbacks never read Minecraft worlds or mutable BodyState objects. */
public final class VoicechatStates {
    private static final ConcurrentMap<UUID, DownedVoiceState> SERVER = new ConcurrentHashMap<>();
    private static volatile DownedVoiceState client = DownedVoiceState.NORMAL;

    private VoicechatStates() {}

    public static DownedVoiceState server(UUID player) { return SERVER.getOrDefault(player, DownedVoiceState.NORMAL); }
    public static DownedVoiceState client() { return client; }
    public static void publishServer(UUID player, DownedVoiceState state) {
        if (state.downed()) SERVER.put(player, state);
        else SERVER.remove(player);
    }
    public static void publishClient(DownedVoiceState state) { client = state; }
    public static void forgetServer(UUID player) { SERVER.remove(player); }
    public static void clearServer() { SERVER.clear(); }
}
