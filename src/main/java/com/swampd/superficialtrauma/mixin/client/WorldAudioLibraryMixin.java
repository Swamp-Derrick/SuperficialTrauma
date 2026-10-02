package com.swampd.superficialtrauma.mixin.client;

import com.mojang.blaze3d.audio.Library;
import com.swampd.superficialtrauma.client.audio.WorldAudioEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Library.class)
public abstract class WorldAudioLibraryMixin {
    @Shadow private long currentDevice;
    @Shadow private long context;

    @Inject(method = "init", at = @At("RETURN"))
    private void superficialTrauma$initialized(String deviceName, boolean directional, CallbackInfo callback) {
        WorldAudioEffects.initialized(currentDevice, context);
    }

    @Inject(method = "cleanup", at = @At(value = "INVOKE", target = "Lorg/lwjgl/openal/ALC10;alcDestroyContext(J)V", remap = false))
    private void superficialTrauma$releaseEffects(CallbackInfo callback) {
        WorldAudioEffects.beforeContextDestroyed(context);
    }
}
