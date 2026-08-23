package com.swampd.superficialtrauma.common.damage;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.body.BodyState;
import com.swampd.superficialtrauma.common.body.WoundUpdateResult;
import com.swampd.superficialtrauma.common.wound.WoundType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.UUID;

public final class ShotgunVolleyAggregator {
    private static final UUID UNKNOWN_SHOOTER_ID = new UUID(0L, 0L);
    private static final float DEBRIDEMENT_CHANCE = 0.60F;
    private static final ShotgunVolleyAccumulator ACCUMULATOR = new ShotgunVolleyAccumulator();

    private ShotgunVolleyAggregator() {
    }

    public static void queue(
            ServerPlayer victim,
            DamageSource source,
            DamageClassification classification,
            float finalDamage,
            long gameTime
    ) {
        Entity directEntity = source.getDirectEntity();
        Entity shooter = source.getEntity();
        long projectileSpawnGameTime = directEntity == null
                ? gameTime
                : gameTime - Math.max(0, directEntity.tickCount);
        ShotgunVolleyAccumulator.VolleyKey key = new ShotgunVolleyAccumulator.VolleyKey(
                victim.getUUID(),
                shooter == null ? UNKNOWN_SHOOTER_ID : shooter.getUUID(),
                classification.ammoId(),
                classification.weaponId(),
                projectileSpawnGameTime
        );
        ACCUMULATOR.addHit(
                key,
                finalDamage,
                victim.getArmorValue(),
                attackerDistance(victim, shooter),
                gameTime,
                source.getMsgId()
        );
        SuperficialTrauma.LOGGER.info(
                "Queued shotgun pellet D={} ammo={} weapon={} spawnTick={} pendingVolleys={}",
                finalDamage,
                classification.ammoId(),
                classification.weaponId(),
                projectileSpawnGameTime,
                ACCUMULATOR.pendingVolleyCount()
        );
    }

    public static boolean resolveReady(ServerPlayer victim, BodyState bodyState, long gameTime) {
        List<ShotgunVolleyAccumulator.CompletedVolley> completed = ACCUMULATOR.drainReady(
                victim.getUUID(),
                gameTime
        );
        if (completed.isEmpty()) {
            return false;
        }

        boolean resolvedAfterDowning = !bodyState.canAct();
        boolean changed = false;
        for (ShotgunVolleyAccumulator.CompletedVolley volley : completed) {
            boolean needsDebridement = volley.totalFinalDamage() >= 4.0F
                    && victim.getRandom().nextFloat() < DEBRIDEMENT_CHANCE;
            WoundUpdateResult result = bodyState.applyGunshotDamage(
                    WoundType.GUNSHOT_SHOTGUN,
                    volley.totalFinalDamage(),
                    volley.maximumArmorValue(),
                    volley.minimumAttackerDistance(),
                    needsDebridement,
                    gameTime
            );
            changed = true;
            SuperficialTrauma.LOGGER.info(
                    "Resolved shotgun volley pellets={} Dtotal={} result={} A={} L={} spawnTick={} hitTicks={}..{} afterDowning={}",
                    volley.pelletHits(),
                    volley.totalFinalDamage(),
                    result.status(),
                    result.accumulatedDamage(),
                    volley.minimumAttackerDistance(),
                    volley.key().projectileSpawnGameTime(),
                    volley.firstHitGameTime(),
                    volley.lastHitGameTime(),
                    resolvedAfterDowning
            );
        }
        return changed;
    }

    public static void clearPlayer(UUID playerId) {
        ACCUMULATOR.clearVictim(playerId);
    }

    public static void clearAll() {
        ACCUMULATOR.clear();
    }

    private static double attackerDistance(ServerPlayer victim, Entity shooter) {
        return shooter == null || shooter == victim
                ? Double.POSITIVE_INFINITY
                : shooter.distanceTo(victim);
    }
}
