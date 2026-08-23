package com.swampd.superficialtrauma.mixin.client;

import com.swampd.superficialtrauma.client.ClientDownedPoses;
import com.swampd.superficialtrauma.common.config.CorpseServerConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class ClientEntityCollisionMixin {
    @Inject(method = "canBeCollidedWith", at = @At("HEAD"), cancellable = true)
    private void superficialTrauma$configureClientDownedCollision(CallbackInfoReturnable<Boolean> callback) {
        Entity entity = (Entity) (Object) this;
        if (entity.level().isClientSide
                && entity instanceof Player player
                && ClientDownedPoses.get(player.getId()).isPresent()) {
            callback.setReturnValue(CorpseServerConfig.downedCollisionEnabled());
        }
    }
}
