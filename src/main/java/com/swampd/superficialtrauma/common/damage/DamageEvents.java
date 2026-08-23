package com.swampd.superficialtrauma.common.damage;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.body.BodyState;
import com.swampd.superficialtrauma.common.body.BodyStateCapability;
import com.swampd.superficialtrauma.common.body.CollapseReason;
import com.swampd.superficialtrauma.common.body.DownedDamageResult;
import com.swampd.superficialtrauma.common.body.DownedHitbox;
import com.swampd.superficialtrauma.common.body.DownedPoseCapture;
import com.swampd.superficialtrauma.common.body.WoundUpdateResult;
import com.swampd.superficialtrauma.common.wound.WoundType;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = SuperficialTrauma.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DamageEvents {
    private DamageEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0.0F) {
            return;
        }
        if (event.getSource().is(DamageTypes.GENERIC_KILL)) {
            handleAdministrativeKill(player, event);
            return;
        }
        if (ModDamageTypes.isInternal(event.getSource())) {
            return;
        }

        long gameTime = player.serverLevel().getGameTime();
        float finalDamage = event.getAmount();
        String damageType = event.getSource().getMsgId();
        DamageClassification classification = withDirectWeapon(
                DamageClassifier.classify(player, event.getSource()),
                event.getSource()
        );
        double sourceDistance = attackerDistance(player, event.getSource());

        BodyStateCapability.get(player).ifPresent(bodyState -> {
            bodyState.recordFinalDamage(
                    finalDamage,
                    damageType,
                    classification,
                    sourceDistance,
                    gameTime
            );

            if (event.getSource().is(DamageTypeTags.IS_DROWNING)) {
                bodyState.addRespiratoryDistress(finalDamage);
                boolean becameHypoxic = bodyState.canAct()
                        && bodyState.respiratoryDistress()
                        >= BodyState.RESPIRATORY_DISTRESS_COLLAPSE_THRESHOLD
                        && bodyState.incapacitateFromLastDamage(CollapseReason.HYPOXIA, gameTime);
                if (becameHypoxic) {
                    event.setAmount(DamageDowning.clampToPreserveLife(player.getHealth(), finalDamage));
                    bodyState.captureDownedPose(
                            DownedPoseCapture.capture(player, event.getSource(), gameTime)
                    );
                    DownedHitbox.update(player, bodyState);
                    ModNetworking.syncBodyState(player);
                    ModNetworking.syncDownedPose(player);
                    return;
                }
            }

            if (!bodyState.canAct()) {
                boolean poseCaptured = bodyState.captureDownedPose(
                        DownedPoseCapture.capture(player, event.getSource(), gameTime)
                );
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
                if (poseCaptured) {
                    ModNetworking.syncDownedPose(player);
                }
                return;
            }

            boolean shotgunPelletQueued = classification.kind() == DamageKind.CGM_SHOTGUN;
            WoundUpdateResult gunshotResult = null;
            if (shotgunPelletQueued) {
                ShotgunVolleyAggregator.queue(
                        player,
                        event.getSource(),
                        classification,
                        finalDamage,
                        gameTime
                );
            } else {
                gunshotResult = applyGunshotDamage(
                        bodyState,
                        player,
                        event.getSource(),
                        classification,
                        finalDamage,
                        gameTime
                );
            }
            if (!shotgunPelletQueued && gunshotResult != null) {
                SuperficialTrauma.LOGGER.info(
                        "Final gunshot D={} type={} classified={} reason={} result={} A={} V={} L={}",
                        finalDamage,
                        damageType,
                        classification.kind().serializedName(),
                        classification.reason(),
                        gunshotResult.status(),
                        gunshotResult.accumulatedDamage(),
                        player.getArmorValue(),
                        sourceDistance
                );
            } else if (!shotgunPelletQueued && classification.woundType() != null) {
                WoundUpdateResult result = bodyState.applyDamage(classification.woundType(), finalDamage, gameTime);
                SuperficialTrauma.LOGGER.info(
                        "Final damage D={} type={} classified={} reason={} result={} A={}",
                        finalDamage,
                        damageType,
                        classification.kind().serializedName(),
                        classification.reason(),
                        result.status(),
                        result.accumulatedDamage()
                );
            } else if (!shotgunPelletQueued) {
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
            boolean becameDowned = lethalHit
                    && bodyState.incapacitateFromLastDamage(CollapseReason.HEMORRHAGIC_SHOCK, gameTime);
            if (becameDowned) {
                bodyState.captureDownedPose(DownedPoseCapture.capture(player, event.getSource(), gameTime));
                DownedHitbox.update(player, bodyState);
                player.displayClientMessage(
                        Component.translatable("message.superficialtrauma.hemorrhagic_shock_incapacitated"),
                        true
                );
            }

            ModNetworking.syncBodyState(player);
            if (becameDowned) {
                ModNetworking.syncDownedPose(player);
            }
        });
    }

    private static void handleAdministrativeKill(ServerPlayer player, LivingDamageEvent event) {
        BodyState bodyState = BodyStateCapability.get(player).orElse(null);
        if (bodyState == null) {
            // Preserve vanilla /kill semantics if a third party prevented capability attachment.
            return;
        }

        ShotgunVolleyAggregator.clearPlayer(player.getUUID());
        boolean stateChanged = bodyState.forceAdministrativeBrainDeath();
        boolean poseCaptured = bodyState.captureDownedPose(
                DownedPoseCapture.capture(player, event.getSource(), player.serverLevel().getGameTime())
        );

        // BodyStateEvents performs the existing corpse snapshot and true-death sequence next tick.
        event.setAmount(0.0F);
        DownedHitbox.update(player, bodyState);
        ModNetworking.syncBodyState(player);
        if (poseCaptured) {
            ModNetworking.syncDownedPose(player);
        }
        SuperficialTrauma.LOGGER.info(
                "Administrative generic_kill for player {}: brainDeathChanged={} poseCaptured={}",
                player.getGameProfile().getName(),
                stateChanged,
                poseCaptured
        );
    }

    private static WoundUpdateResult applyGunshotDamage(
            com.swampd.superficialtrauma.common.body.BodyState bodyState,
            ServerPlayer player,
            DamageSource source,
            DamageClassification classification,
            float finalDamage,
            long gameTime
    ) {
        WoundType woundType = switch (classification.kind()) {
            case CGM_LOW_VELOCITY -> WoundType.GUNSHOT_LOW_VELOCITY;
            case CGM_HIGH_VELOCITY -> WoundType.GUNSHOT_HIGH_VELOCITY;
            case CGM_SHOTGUN -> WoundType.GUNSHOT_SHOTGUN;
            default -> null;
        };
        if (woundType == null) {
            return null;
        }

        float debridementChance = switch (woundType) {
            case GUNSHOT_LOW_VELOCITY -> 0.50F;
            case GUNSHOT_HIGH_VELOCITY -> 0.30F;
            case GUNSHOT_SHOTGUN -> 0.60F;
            default -> 0.0F;
        };
        boolean needsDebridement = finalDamage >= 4.0F
                && player.getRandom().nextFloat() < debridementChance;
        return bodyState.applyGunshotDamage(
                woundType,
                finalDamage,
                player.getArmorValue(),
                attackerDistance(player, source),
                needsDebridement,
                gameTime
        );
    }

    private static double attackerDistance(ServerPlayer player, DamageSource source) {
        Entity attacker = source.getEntity();
        return attacker == null || attacker == player
                ? Double.POSITIVE_INFINITY
                : attacker.distanceTo(player);
    }

    private static DamageClassification withDirectWeapon(
            DamageClassification classification,
            DamageSource source
    ) {
        if (!"none".equals(classification.weaponId())) {
            return classification;
        }
        Entity attacker = source.getEntity();
        if (attacker == null
                || source.getDirectEntity() != attacker
                || !(attacker instanceof LivingEntity livingAttacker)) {
            return classification;
        }
        ItemStack weapon = livingAttacker.getMainHandItem();
        if (weapon.isEmpty()) {
            return classification;
        }
        ResourceLocation weaponId = ForgeRegistries.ITEMS.getKey(weapon.getItem());
        if (weaponId == null) {
            return classification;
        }
        return new DamageClassification(
                classification.woundType(),
                classification.kind(),
                classification.reason(),
                classification.projectileEntityId(),
                classification.ammoId(),
                weaponId.toString()
        );
    }
}
