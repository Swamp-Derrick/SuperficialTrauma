package com.swampd.superficialtrauma.common.damage;

import com.swampd.superficialtrauma.common.body.BodyStateCapability;
import com.swampd.superficialtrauma.common.body.CollapseReason;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class BloodLossDamage {
    private BloodLossDamage() {
    }

    public static void apply(ServerPlayer player, float amount) {
        if (amount <= 0.0F
                || !player.isAlive()
                || player.isSpectator()
                || player.getAbilities().invulnerable) {
            return;
        }

        float currentHealth = player.getHealth();
        boolean wouldBeFatal = DamageDowning.wouldBeFatal(currentHealth, amount);
        float appliedDamage = wouldBeFatal
                ? DamageDowning.clampToPreserveLife(currentHealth, amount)
                : Math.min(amount, currentHealth);
        if (appliedDamage > 0.0F) {
            player.setHealth(currentHealth - appliedDamage);
        }

        boolean becameIncapacitated = wouldBeFatal && BodyStateCapability.get(player)
                .map(bodyState -> bodyState.incapacitate(CollapseReason.HEMORRHAGIC_SHOCK))
                .orElse(false);

        ModNetworking.sendBloodLossFeedback(player, amount);
        if (becameIncapacitated) {
            player.displayClientMessage(
                    Component.translatable("message.superficialtrauma.hemorrhagic_shock_incapacitated"),
                    true
            );
            ModNetworking.syncBodyState(player);
        }
    }
}
