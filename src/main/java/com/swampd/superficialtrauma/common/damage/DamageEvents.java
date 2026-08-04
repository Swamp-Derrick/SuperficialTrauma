package com.swampd.superficialtrauma.common.damage;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.body.BodyStateCapability;
import com.swampd.superficialtrauma.common.body.CollapseReason;
import com.swampd.superficialtrauma.common.body.DownedDamageResult;
import com.swampd.superficialtrauma.common.body.WoundUpdateResult;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.network.chat.Component;
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
        if (ModDamageTypes.isInternal(event.getSource())) {
            return;
        }

        long gameTime = player.serverLevel().getGameTime();
        float finalDamage = event.getAmount();
        String damageType = event.getSource().getMsgId();
        DamageClassification classification = DamageClassifier.classify(player, event.getSource());

        BodyStateCapability.get(player).ifPresent(bodyState -> {
            bodyState.recordFinalDamage(finalDamage, damageType, classification, gameTime);

            if (!bodyState.canAct()) {
                DownedDamageResult downedResult = bodyState.applyDownedDamage(finalDamage, gameTime);
                event.setAmount(DamageDowning.clampToPreserveLife(player.getHealth(), finalDamage));
                SuperficialTrauma.LOGGER.info(
                        "Downed final damage D={} type={} traumaSkipped=true shortened={}t remaining={}t state={}=>{}",
                        finalDamage,
                        damageType,
                        downedResult.shortenedTicks(),
                        downedResult.remainingTicks(),
                        downedResult.previousState().serializedName(),
                        downedResult.resultingState().serializedName()
                );
                ModNetworking.syncBodyState(player);
                return;
            }

            if (classification.woundType() != null) {
                WoundUpdateResult result = bodyState.applyDamage(
                        classification.woundType(),
                        finalDamage,
                        gameTime
                );
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

            boolean lethalHit = DamageDowning.wouldBeFatal(player.getHealth(), finalDamage);
            if (lethalHit) {
                event.setAmount(DamageDowning.clampToPreserveLife(player.getHealth(), finalDamage));
            }
            if (lethalHit && bodyState.incapacitate(CollapseReason.LETHAL_DAMAGE, gameTime)) {
                player.displayClientMessage(
                        Component.translatable("message.superficialtrauma.lethal_damage_incapacitated"),
                        true
                );
            }

            ModNetworking.syncBodyState(player);
        });
    }
}
