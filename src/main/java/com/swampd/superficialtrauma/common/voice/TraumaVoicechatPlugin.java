package com.swampd.superficialtrauma.common.voice;

import com.swampd.superficialtrauma.SuperficialTrauma;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.*;

/** Discovered by SVC only. No always-loaded class depends on the optional API. */
@ForgeVoicechatPlugin
public final class TraumaVoicechatPlugin implements VoicechatPlugin {
    private static final VoicechatAudioProcessor AUDIO = new VoicechatAudioProcessor();

    @Override public String getPluginId() { return SuperficialTrauma.MOD_ID; }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, event -> {
            var sender = event.getSenderConnection();
            if (sender != null && sender.getPlayer() != null
                    && VoicechatStates.server(sender.getPlayer().getUuid()).muted()) {
                event.cancel(); // Before SVC routes to either proximity or group chat.
            }
        }, 100);
        registration.registerEvent(ClientSoundEvent.class, event -> {
            if (VoicechatStates.client().muted()) event.cancel();
        }, 100);
        // SVC dispatches exact event types, not the ClientReceiveSoundEvent parent.
        registration.registerEvent(ClientReceiveSoundEvent.EntitySound.class, event -> receive(event, "entity"));
        registration.registerEvent(ClientReceiveSoundEvent.LocationalSound.class, event -> receive(event, "location"));
        registration.registerEvent(ClientReceiveSoundEvent.StaticSound.class, event -> receive(event, "static"));
        registration.registerEvent(ClientVoicechatConnectionEvent.class, event -> AUDIO.clear());
        SuperficialTrauma.LOGGER.info("Simple Voice Chat trauma integration registered (3s speech grace, 10s clear hearing)");
    }

    private static void receive(ClientReceiveSoundEvent event, String kind) {
        event.setRawAudio(AUDIO.process(event.getId(), kind, event.getRawAudio(),
                VoicechatStates.client().listening(), VoicechatStates.clientConcussionHearing()));
    }
}
