package com.swampd.superficialtrauma.mixin;

import com.swampd.superficialtrauma.common.body.BodyStateCapability;
import com.swampd.superficialtrauma.common.config.CorpseServerConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityCollisionMixin {
    @Inject(method = "canBeCollidedWith", at = @At("HEAD"), cancellable = true)
    private void superficialTrauma$configureDownedCollision(CallbackInfoReturnable<Boolean> callback) {
        Entity entity = (Entity) (Object) this;
        if (entity.level().isClientSide || !(entity instanceof Player player)) {
            return;
        }
        BodyStateCapability.get(player).ifPresent(bodyState -> {
            if (!bodyState.canAct()) {
                callback.setReturnValue(CorpseServerConfig.downedCollisionEnabled());
            }
        });
    }
}
