package com.swampd.superficialtrauma.common.damage;

import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Captures CGM's authoritative impact before damage; no hard dependency or client-provided hit location. */
@EventBusSubscriber(modid = SuperficialTrauma.MOD_ID)
public final class GunshotHitLocations {
    private static final Map<Key, Hit> HITS = new HashMap<>();
    private static Method projectileGetter, resultGetter;
    private static boolean registered;

    private GunshotHitLocations() { }

    public static void registerOptionalGunEvents() {
        if (registered) return;
        try {
            Class<? extends Event> type = Class.forName("com.mrcrayfish.guns.event.GunProjectileHitEvent")
                    .asSubclass(Event.class);
            projectileGetter = type.getMethod("getProjectile");
            resultGetter = type.getMethod("getRayTrace");
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, type, GunshotHitLocations::capture);
            registered = true;
            SuperficialTrauma.LOGGER.info("Registered optional CGM regional bullet-hit capture");
        } catch (ClassNotFoundException ignored) {
            // CGM and its add-ons remain optional.
        } catch (ReflectiveOperationException exception) {
            SuperficialTrauma.LOGGER.warn("CGM hit API unavailable; serious trauma will not guess hit locations", exception);
        }
    }

    private static void capture(Event event) {
        try {
            if (!(projectileGetter.invoke(event) instanceof Entity projectile)
                    || projectile.level().isClientSide()
                    || !(resultGetter.invoke(event) instanceof EntityHitResult hit)
                    || !(hit.getEntity() instanceof ServerPlayer victim)) return;
            captureImpact(projectile, victim, hit.getLocation());
        } catch (ReflectiveOperationException exception) {
            SuperficialTrauma.LOGGER.warn("Could not read CGM bullet hit", exception);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void captureVanillaImpact(ProjectileImpactEvent event) {
        if (!event.getProjectile().level().isClientSide()
                && event.getRayTraceResult() instanceof EntityHitResult hit
                && hit.getEntity() instanceof ServerPlayer victim) {
            captureImpact(event.getProjectile(), victim, hit.getLocation());
        }
    }

    private static void captureImpact(Entity projectile, ServerPlayer victim, Vec3 point) {
        BulletHitLocation region = classifyImpact(victim.getBoundingBox(), victim.yBodyRot, victim.getPose(), point);
        HITS.put(new Key(projectile.getUUID(), victim.getUUID()),
                new Hit(victim.serverLevel().getGameTime(), region));
    }

    /** Approximate upright anatomy, not animated limbs. Unhandled horizontal poses fail closed. */
    public static GunshotRegion classify(AABB box, float bodyYaw, Pose pose, Vec3 point) {
        return classifyImpact(box, bodyYaw, pose, point).traumaRegion();
    }

    public static BulletHitLocation classifyImpact(AABB box, float bodyYaw, Pose pose, Vec3 point) {
        if (pose != Pose.STANDING && pose != Pose.CROUCHING) return BulletHitLocation.UNKNOWN;
        if (point == null || !Double.isFinite(point.x) || !Double.isFinite(point.y)
                || !Double.isFinite(point.z) || !Float.isFinite(bodyYaw) || box.getYsize() <= 0) return BulletHitLocation.UNKNOWN;
        double height = (point.y - box.minY) / box.getYsize();
        // CGM slightly expands its attack hitboxes. Allow the same small outer tolerance.
        if (!box.inflate(0.125).contains(point)) return BulletHitLocation.UNKNOWN;
        double headStart = pose == Pose.CROUCHING ? 0.75 : 0.80;
        if (height >= headStart) return BulletHitLocation.HEAD;
        double chestStart = pose == Pose.CROUCHING ? 0.45 : 0.50;
        if (height < chestStart) return BulletHitLocation.LIMBS; // Simplified non-head/chest forensic category.
        Vec3 relative = point.subtract(box.getCenter());
        double yaw = Math.toRadians(bodyYaw);
        double localX = relative.x * Math.cos(yaw) + relative.z * Math.sin(yaw);
        double halfTorsoWidth = Math.min(box.getXsize(), box.getZsize()) * (5.0 / 12.0);
        return Math.abs(localX) <= halfTorsoWidth ? BulletHitLocation.CHEST : BulletHitLocation.LIMBS;
    }

    public static BulletHitLocation consume(ServerPlayer victim, DamageSource source) {
        Entity projectile = source.getDirectEntity();
        if (projectile == null) return BulletHitLocation.UNKNOWN;
        Hit hit = HITS.remove(new Key(projectile.getUUID(), victim.getUUID()));
        return hit != null && hit.gameTime == victim.serverLevel().getGameTime()
                ? hit.region : BulletHitLocation.UNKNOWN;
    }

    @SubscribeEvent
    public static void endTick(ServerTickEvent.Post event) { HITS.clear(); }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) { HITS.clear(); }

    private record Key(UUID projectile, UUID victim) { }
    private record Hit(long gameTime, BulletHitLocation region) { }
}
