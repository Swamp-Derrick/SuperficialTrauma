package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.client.audio.ClientWorldHearing;
import com.swampd.superficialtrauma.common.body.DownedPoseSnapshot;
import com.swampd.superficialtrauma.common.audio.ConcussionHearingEnvelope;
import com.swampd.superficialtrauma.common.voice.DownedVoiceState;
import com.swampd.superficialtrauma.common.voice.VoicechatStates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = SuperficialTrauma.MOD_ID, value = Dist.CLIENT)
public final class ClientVoicechatState {
    private ClientVoicechatState() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !ClientBodyState.hasReceivedSnapshot()) {
            VoicechatStates.publishClient(DownedVoiceState.NORMAL);
            VoicechatStates.publishConcussionHearing(0);
            ClientWorldHearing.reset();
            return;
        }
        var body = ClientBodyState.snapshot();
        long now = mc.level.getGameTime();
        var previous = VoicechatStates.client();
        long since = body.downedPoseSnapshot().map(DownedPoseSnapshot::downedGameTime)
                .orElse(previous.downed() ? previous.downedSince() : now);
        var state = DownedVoiceState.update(body.lifeState(), since, now, previous);
        VoicechatStates.publishClient(state);
        if (!mc.isPaused()) {
            var concussion = body.seriousTrauma();
            float amount = ConcussionHearingEnvelope.strength(concussion.hasConcussion(), concussion.concussionStartedAt(), now);
            VoicechatStates.publishConcussionHearing(amount);
            ClientWorldHearing.update(state, now, amount);
        }
        if (state.downed() && isVoicechatScreen(mc.screen)) mc.setScreen(null);
    }

    @SubscribeEvent
    public static void opening(ScreenEvent.Opening event) {
        var mc = Minecraft.getInstance();
        boolean downed = mc.player != null && (VoicechatStates.client().downed()
                || (ClientBodyState.hasReceivedSnapshot() && !ClientBodyState.snapshot().canAct())
                || ClientDownedPoses.get(mc.player.getId()).isPresent());
        if (downed && isVoicechatScreen(event.getNewScreen())) event.setCanceled(true);
    }

    public static boolean isVoicechatScreen(Screen screen) {
        for (Class<?> type = screen == null ? null : screen.getClass(); type != null; type = type.getSuperclass()) {
            if (type.getName().startsWith("de.maxhenkel.voicechat.gui.")) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        VoicechatStates.publishClient(DownedVoiceState.NORMAL);
        VoicechatStates.publishConcussionHearing(0);
        ClientWorldHearing.reset();
    }
}
