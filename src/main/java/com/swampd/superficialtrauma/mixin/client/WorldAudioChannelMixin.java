package com.swampd.superficialtrauma.mixin.client;

import com.mojang.blaze3d.audio.Channel;
import com.swampd.superficialtrauma.client.audio.ClientWorldHearing;
import com.swampd.superficialtrauma.client.audio.WorldAudioChannel;
import com.swampd.superficialtrauma.client.audio.WorldAudioEffects;
import com.swampd.superficialtrauma.common.audio.WorldHearingProfile;
import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(Channel.class)
public abstract class WorldAudioChannelMixin implements WorldAudioChannel {
    @Shadow @Final private int source;
    @Shadow @Final private AtomicBoolean initialized;
    @Unique private boolean superficialTrauma$external;
    @Unique private boolean superficialTrauma$filtered;
    @Unique private float superficialTrauma$baseVolume = 1;
    @Unique private WorldHearingProfile superficialTrauma$lastProfile;

    @Override
    public void superficialTrauma$setExternal(boolean external) {
        superficialTrauma$external = external;
        superficialTrauma$updateHearing();
    }

    @ModifyVariable(method = "setVolume", at = @At("HEAD"), argsOnly = true)
    private float superficialTrauma$volume(float volume) {
        superficialTrauma$baseVolume = volume;
        return volume * (superficialTrauma$external ? ClientWorldHearing.profile().gain() : 1);
    }

    @Inject(method = "updateStream", at = @At("HEAD"))
    private void superficialTrauma$tickHearing(CallbackInfo callback) {
        // Called for static AND streamed channels, including sounds that started before collapse.
        superficialTrauma$updateHearing();
    }

    @Unique
    private void superficialTrauma$updateHearing() {
        if (!initialized.get()) return;
        var profile = superficialTrauma$external ? ClientWorldHearing.profile() : WorldHearingProfile.CLEAR;
        if (profile.equals(superficialTrauma$lastProfile)) return;
        if (profile == WorldHearingProfile.CLEAR) {
            if (superficialTrauma$filtered) {
                WorldAudioEffects.detach(source);
                WorldAudioEffects.clearTail();
                superficialTrauma$filtered = false;
            }
            if (superficialTrauma$lastProfile != null) AL10.alSourcef(source, AL10.AL_GAIN, superficialTrauma$baseVolume);
        } else {
            superficialTrauma$filtered = WorldAudioEffects.apply(source, profile);
            AL10.alSourcef(source, AL10.AL_GAIN, superficialTrauma$baseVolume * profile.gain());
        }
        superficialTrauma$lastProfile = profile;
    }
}
