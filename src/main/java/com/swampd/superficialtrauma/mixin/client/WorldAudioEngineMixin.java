package com.swampd.superficialtrauma.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEngineExecutor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SoundEngine.class)
public abstract class WorldAudioEngineMixin {
    @Shadow @Final private SoundEngineExecutor executor;

    @WrapOperation(method = "stopAll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/ChannelAccess;clear()V"))
    private void superficialTrauma$releaseChannelsOnSoundThread(ChannelAccess channels, Operation<Void> original) {
        // Vanilla queues stop operations, then destroys their sources on the calling thread.
        // Serialize destruction with queued native operations before Library destroys the context.
        executor.submit(() -> { original.call(channels); }).join();
    }
}
