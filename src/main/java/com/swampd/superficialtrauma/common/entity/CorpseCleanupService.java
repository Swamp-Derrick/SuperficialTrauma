package com.swampd.superficialtrauma.common.entity;

import com.swampd.superficialtrauma.common.drag.BodyDragService;
import com.swampd.superficialtrauma.common.forensics.AutopsyService;
import com.swampd.superficialtrauma.common.loot.LootingService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Explicit administrator cleanup only; never force-loads or edits unloaded entity storage. */
public final class CorpseCleanupService {
    private CorpseCleanupService() {}

    public static List<CorpseEntity> loadedCorpses(ServerLevel level) {
        List<CorpseEntity> corpses = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof CorpseEntity corpse && !corpse.isRemoved()) {
                corpses.add(corpse);
            }
        }
        return corpses;
    }

    public static List<CorpseEntity> loadedCorpses(MinecraftServer server) {
        List<CorpseEntity> corpses = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            corpses.addAll(loadedCorpses(level));
        }
        return corpses;
    }

    public static List<CorpseEntity> nearest(ServerLevel level, Vec3 origin, int count) {
        if (count <= 0) {
            return List.of();
        }
        return loadedCorpses(level).stream()
                .sorted(Comparator.comparingDouble((CorpseEntity corpse) -> corpse.distanceToSqr(origin))
                        .thenComparing(Entity::getUUID))
                .limit(count)
                .toList();
    }

    /** Accept offline snapshot names as well as UUIDs; online names use the player's stable UUID. */
    public static Optional<CorpseEntity> latestForOwner(MinecraftServer server, String owner) {
        ServerPlayer online = server.getPlayerList().getPlayerByName(owner);
        UUID ownerId = online == null ? parseUuid(owner) : online.getUUID();
        return loadedCorpses(server).stream()
                .filter(corpse -> ownerId != null
                        ? corpse.ownerId().filter(ownerId::equals).isPresent()
                        : corpse.ownerName().equalsIgnoreCase(owner))
                .max(Comparator.comparingLong(CorpseEntity::deathGameTime).thenComparing(Entity::getUUID));
    }

    /** Use the real eye ray and interaction range; nearer blocks/other entities obstruct selection. */
    public static Optional<CorpseEntity> targeted(ServerPlayer player) {
        double reach = player.entityInteractionRange();
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getViewVector(1.0F).scale(reach);
        Vec3 end = start.add(direction);
        HitResult blockHit = player.pick(reach, 1.0F, false);
        double distanceSquared = blockHit.getType() == HitResult.Type.MISS
                ? reach * reach : start.distanceToSqr(blockHit.getLocation());
        var hit = ProjectileUtil.getEntityHitResult(player, start, end,
                player.getBoundingBox().expandTowards(direction).inflate(1.0D),
                entity -> !entity.isSpectator() && entity.isPickable(), distanceSquared);
        return hit != null && hit.getEntity() instanceof CorpseEntity corpse && !corpse.isRemoved()
                ? Optional.of(corpse) : Optional.empty();
    }

    public static int clear(List<CorpseEntity> corpses) {
        int cleared = 0;
        for (CorpseEntity corpse : corpses) {
            if (clear(corpse)) {
                cleared++;
            }
        }
        return cleared;
    }

    public static boolean clear(CorpseEntity corpse) {
        if (corpse.isRemoved() || !(corpse.level() instanceof ServerLevel level)) {
            return false;
        }
        LootingService.closeForTarget(corpse);
        AutopsyService.closeForCorpse(corpse);
        BodyDragService.stopDraggingTarget(corpse);
        // Includes inventory, armor and offhand. Vanilla splitting retains item components.
        // Discard rather than kill: no second equipment/loot-table drop, and no gamerule gate.
        Containers.dropContents(level, corpse, corpse);
        corpse.clearContent();
        corpse.discard();
        return true;
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
