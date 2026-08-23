package com.swampd.superficialtrauma.mixin.client;

import com.swampd.superficialtrauma.client.ClientDownedPoses;
import com.swampd.superficialtrauma.common.config.CorpseServerConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class ClientLivingEntityPushingMixin {
    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void superficialTrauma$configureClientDownedPushability(CallbackInfoReturnable<Boolean> callback) {
        if (superficialTrauma$isNonPushingDownedPlayer()) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true)
    private void superficialTrauma$disableClientDownedOutgoingPushes(CallbackInfo callback) {
        if (superficialTrauma$isNonPushingDownedPlayer()) {
            callback.cancel();
        }
    }

    private boolean superficialTrauma$isNonPushingDownedPlayer() {
        LivingEntity entity = (LivingEntity) (Object) this;
        return entity.level().isClientSide
                && entity instanceof Player player
                && !CorpseServerConfig.downedEntityPushingEnabled()
                && ClientDownedPoses.get(player.getId()).isPresent();
    }
}
