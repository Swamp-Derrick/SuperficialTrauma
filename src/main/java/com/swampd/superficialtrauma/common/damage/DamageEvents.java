package com.swampd.superficialtrauma.common.damage;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.body.BodyStateCapability;
import com.swampd.superficialtrauma.common.body.WoundUpdateResult;
import com.swampd.superficialtrauma.common.wound.WoundType;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SuperficialTrauma.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DamageEvents {
    private DamageEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0.0F) {
            return;
        }

        long gameTime = player.serverLevel().getGameTime();
        float finalDamage = event.getAmount();
        String damageType = event.getSource().getMsgId();
        DamageClassification classification = DamageClassifier.classify(player, event.getSource());

        BodyStateCapability.get(player).ifPresent(bodyState -> {
            bodyState.recordFinalDamage(finalDamage, damageType, classification, gameTime);

            if (classification.woundType() == WoundType.BLUNT) {
                WoundUpdateResult result = bodyState.applyBluntDamage(finalDamage, gameTime);
                SuperficialTrauma.LOGGER.info(
                        "Final damage D={} type={} classified={} reason={} result={} A={}",
                        finalDamage,
                        damageType,
                        classification.kind().serializedName(),
                        classification.reason(),
                        result.status(),
                        result.accumulatedDamage()
                );
            } else {
                SuperficialTrauma.LOGGER.info(
                        "Final damage D={} type={} classified={} reason={} projectile={} ammo={} weapon={}",
                        finalDamage,
                        damageType,
                        classification.kind().serializedName(),
                        classification.reason(),
                        classification.projectileEntityId(),
                        classification.ammoId(),
                        classification.weaponId()
                );
            }

            ModNetworking.syncBodyState(player);
        });
    }
}
