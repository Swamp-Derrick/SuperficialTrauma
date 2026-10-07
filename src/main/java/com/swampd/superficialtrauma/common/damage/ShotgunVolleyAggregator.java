package com.swampd.superficialtrauma.common.damage;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.body.BodyState;
import com.swampd.superficialtrauma.common.body.DowningHitRecord;
import com.swampd.superficialtrauma.common.body.DownedHitbox;
import com.swampd.superficialtrauma.common.body.WoundUpdateResult;
import com.swampd.superficialtrauma.common.wound.WoundType;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = SuperficialTrauma.MOD_ID)
public final class ShotgunVolleyAggregator {
    private static final UUID UNKNOWN_SHOOTER_ID = new UUID(0L, 0L);
    private static final float DEBRIDEMENT_CHANCE = 0.60F;
    private static final ShotgunVolleyAccumulator ACCUMULATOR = new ShotgunVolleyAccumulator();
    private static final Map<UUID, DowningVolley> DOWNING = new HashMap<>();

    private ShotgunVolleyAggregator() {
    }

    public static void queue(
            ServerPlayer victim,
            DamageSource source,
            DamageClassification classification,
            float finalDamage,
            long gameTime,
            BulletHitLocation hitLocation,
            double armorRating
    ) {
        var key = keyFor(victim, source, classification, gameTime);
        ACCUMULATOR.addLocatedHit(key, finalDamage, armorRating,
                attackerDistance(victim, source.getEntity()), gameTime, source.getMsgId(), hitLocation);
        SuperficialTrauma.LOGGER.info(
                "Queued shotgun pellet D={} ammo={} weapon={} spawnTick={} pendingVolleys={}",
                finalDamage, classification.ammoId(), classification.weaponId(),
                key.projectileSpawnGameTime(), ACCUMULATOR.pendingVolleyCount());
    }

    private static ShotgunVolleyAccumulator.VolleyKey keyFor(ServerPlayer victim, DamageSource source,
            DamageClassification classification, long gameTime) {
        Entity directEntity = source.getDirectEntity();
        Entity shooter = source.getEntity();
        long projectileSpawnGameTime = directEntity == null
                ? gameTime
                : gameTime - Math.max(0, directEntity.tickCount);
        return new ShotgunVolleyAccumulator.VolleyKey(
                victim.getUUID(),
                shooter == null ? UNKNOWN_SHOOTER_ID : shooter.getUUID(),
                classification.ammoId(),
                classification.weaponId(),
                projectileSpawnGameTime
        );
    }

    public static void markDowning(ServerPlayer victim, BodyState state, DamageSource source,
            DamageClassification classification, long gameTime) {
        state.downingHitRecord().ifPresent(hit -> DOWNING.put(victim.getUUID(), new DowningVolley(
                victim, state, hit, keyFor(victim, source, classification, gameTime), gameTime)));
    }

    /** Same discharge and same impact tick only; later executions can never change this evidence. */
    public static void queueDowningRemainder(ServerPlayer victim, BodyState state, DamageSource source,
            DamageClassification classification, float damage, long gameTime, BulletHitLocation location, double armorRating) {
        DowningVolley pending = DOWNING.get(victim.getUUID());
        if (pending != null && pending.state == state && pending.tick == gameTime
                && state.downingHitRecord().orElse(null) == pending.hit
                && pending.key.equals(keyFor(victim, source, classification, gameTime))) {
            queue(victim, source, classification, damage, gameTime, location, armorRating);
        }
    }

    /** Keep the original upright hitbox until this tick's pellets finish, not for the whole downed phase. */
    public static boolean awaitingDowningPellets(ServerPlayer victim) {
        DowningVolley pending = DOWNING.get(victim.getUUID());
        return pending != null && pending.tick == victim.serverLevel().getGameTime()
                && pending.state.downingHitRecord().orElse(null) == pending.hit;
    }

    public static boolean finishDowningVolley(ServerPlayer victim) {
        DowningVolley pending = DOWNING.remove(victim.getUUID());
        if (pending == null) return false;
        var volley = ACCUMULATOR.take(pending.key);
        // An admin recovery, /kill or a new downed episode invalidates the old pending evidence.
        if (volley != null && pending.state.downingHitRecord().orElse(null) == pending.hit
                && !pending.state.canAct() && !pending.state.administrativeDeath()) {
            applyVolley(victim, pending.state, volley, pending.tick);
            pending.state.completeDowningBulletHit(pending.hit, volley.bulletLocation(),
                    volley.headFinalDamage(), volley.penetratingHeadDamage(), () -> victim.getRandom().nextFloat());
            ModNetworking.syncBodyState(victim);
        }
        DownedHitbox.update(victim, pending.state);
        return volley != null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void endTick(ServerTickEvent.Post event) {
        for (var pending : List.copyOf(DOWNING.values())) finishDowningVolley(pending.victim);
    }

    private record DowningVolley(ServerPlayer victim, BodyState state, DowningHitRecord hit,
                                ShotgunVolleyAccumulator.VolleyKey key, long tick) { }

    public static boolean resolveReady(ServerPlayer victim, BodyState bodyState, long gameTime) {
        DowningVolley pending = DOWNING.get(victim.getUUID());
        boolean changed = pending != null && gameTime > pending.tick && finishDowningVolley(victim);
        List<ShotgunVolleyAccumulator.CompletedVolley> completed = ACCUMULATOR.drainReady(
                victim.getUUID(),
                gameTime
        );
        if (completed.isEmpty()) {
            return changed;
        }

        for (ShotgunVolleyAccumulator.CompletedVolley volley : completed) {
            applyVolley(victim, bodyState, volley, gameTime);
            changed = true;
        }
        return changed;
    }

    private static void applyVolley(ServerPlayer victim, BodyState bodyState,
            ShotgunVolleyAccumulator.CompletedVolley volley, long gameTime) {
        bodyState.configureSeriousTrauma(
                com.swampd.superficialtrauma.common.config.SeriousTraumaConfig.enabled(), gameTime);
        boolean needsDebridement = volley.penetratingFinalDamage() > 0
                && victim.getRandom().nextFloat() < DEBRIDEMENT_CHANCE;
        WoundUpdateResult result = bodyState.applyPenetratingGunshotDamage(
                WoundType.GUNSHOT_SHOTGUN,
                volley.penetratingFinalDamage(),
                volley.penetratedStrongArmor(),
                volley.penetratingCloseRange(),
                needsDebridement,
                volley.key().ammoId(),
                volley.firstHitGameTime()
        );
        if (volley.blockedFinalDamage() > 0) {
            bodyState.applyDamage(WoundType.BLUNT, volley.blockedFinalDamage(), volley.firstHitGameTime());
        }
        // Only penetrating pellets enter gunshot pools. Their armor/location was captured at impact.
        for (var hit : volley.regionalHits()) {
            bodyState.recordGunshotLocations(result, volley.penetratingFinalDamage(),
                    hit.region() == GunshotRegion.HEAD ? hit.damage() : 0,
                    hit.region() == GunshotRegion.CHEST ? hit.damage() : 0, hit.gameTime());
        }
        SuperficialTrauma.LOGGER.info(
                "Resolved shotgun volley pellets={} Dtotal={} result={} A={} maxR={} distance={} spawnTick={} hitTicks={}..{} afterDowning={} penetrating={} blocked={}",
                volley.pelletHits(),
                volley.totalFinalDamage(),
                result.status(),
                result.accumulatedDamage(),
                volley.maximumArmorValue(),
                volley.minimumAttackerDistance(),
                volley.key().projectileSpawnGameTime(),
                volley.firstHitGameTime(),
                volley.lastHitGameTime(),
                !bodyState.canAct(),
                volley.penetratingFinalDamage(),
                volley.blockedFinalDamage()
        );
    }

    public static void clearPlayer(UUID playerId) {
        DOWNING.remove(playerId);
        ACCUMULATOR.clearVictim(playerId);
    }

    public static void clearAll() {
        DOWNING.clear();
        ACCUMULATOR.clear();
    }

    private static double attackerDistance(ServerPlayer victim, Entity shooter) {
        return shooter == null || shooter == victim
                ? Double.POSITIVE_INFINITY
                : shooter.distanceTo(victim);
    }
}
