package com.swampd.superficialtrauma.common.loot;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.body.BodyStateCapability;
import com.swampd.superficialtrauma.common.entity.CorpseEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LootingService {
    public static final double MAX_OPEN_DISTANCE_SQUARED = 4.5D * 4.5D;
    public static final double MAX_CONTINUE_DISTANCE_SQUARED = 6.0D * 6.0D;

    private static final Map<UUID, UUID> LOOTER_BY_TARGET = new ConcurrentHashMap<>();

    private LootingService() {
    }

    public static boolean tryOpen(ServerPlayer looter, int targetEntityId) {
        Entity target = looter.serverLevel().getEntity(targetEntityId);
        return target != null && tryOpen(looter, target);
    }

    public static boolean tryOpen(ServerPlayer looter, ServerPlayer target) {
        return tryOpen(looter, (Entity) target);
    }

    public static boolean tryOpen(ServerPlayer looter, CorpseEntity target) {
        return tryOpen(looter, (Entity) target);
    }

    private static boolean tryOpen(ServerPlayer looter, Entity target) {
        if (!canOpenLooting(looter, target)) {
            return false;
        }

        if (looter.containerMenu instanceof LootTargetMenu existingMenu
                && target.getId() == existingMenu.targetEntityId()) {
            return true;
        }

        if (looter.containerMenu != looter.inventoryMenu) {
            looter.closeContainer();
        }

        UUID targetId = target.getUUID();
        UUID looterId = looter.getUUID();
        UUID currentLooter = LOOTER_BY_TARGET.putIfAbsent(targetId, looterId);
        if (currentLooter != null && !currentLooter.equals(looterId)) {
            looter.displayClientMessage(
                    Component.translatable("message.superficialtrauma.loot.busy", target.getDisplayName()),
                    true
            );
            return false;
        }

        MenuProvider provider = new SimpleMenuProvider(
                (containerId, inventory, player) -> createMenu(containerId, inventory, target),
                lootTitle(target)
        );
        try {
            looter.openMenu(provider, buffer -> {
                buffer.writeVarInt(target.getId());
                buffer.writeBoolean(target instanceof CorpseEntity);
            });
            return true;
        } catch (RuntimeException exception) {
            release(targetId, looterId);
            SuperficialTrauma.LOGGER.error(
                    "Failed to open loot menu for {} targeting {}",
                    looter.getGameProfile().getName(),
                    target.getName().getString(),
                    exception
            );
            return false;
        }
    }

    public static boolean isLootable(ServerPlayer target) {
        return isLootable((Entity) target);
    }

    public static boolean isLootable(Entity target) {
        if (target instanceof CorpseEntity) {
            return !target.isRemoved();
        }
        return target instanceof ServerPlayer player
                && player.isAlive()
                && !target.isRemoved()
                && BodyStateCapability.get(player).map(bodyState -> !bodyState.canAct()).orElse(false);
    }

    public static boolean canContinueLooting(Player looter, Entity target) {
        if (!(looter instanceof ServerPlayer serverLooter)) {
            return true;
        }
        return isEligible(serverLooter, target, MAX_CONTINUE_DISTANCE_SQUARED, false);
    }

    public static void closeIfInvalid(ServerPlayer looter) {
        if (looter.containerMenu instanceof LootTargetMenu menu && !menu.stillValid(looter)) {
            looter.closeContainer();
        }
    }

    public static void release(UUID targetId, UUID looterId) {
        LOOTER_BY_TARGET.computeIfPresent(
                targetId,
                (ignored, currentLooter) -> currentLooter.equals(looterId) ? null : currentLooter
        );
    }

    public static void forgetPlayer(UUID playerId) {
        LOOTER_BY_TARGET.remove(playerId);
        LOOTER_BY_TARGET.entrySet().removeIf(entry -> entry.getValue().equals(playerId));
    }

    public static void clearAll() {
        LOOTER_BY_TARGET.clear();
    }

    private static boolean canOpenLooting(ServerPlayer looter, Entity target) {
        return isEligible(looter, target, MAX_OPEN_DISTANCE_SQUARED, true);
    }

    private static boolean isEligible(
            ServerPlayer looter,
            Entity target,
            double maximumDistanceSquared,
            boolean requireLineOfSight
    ) {
        if ((target instanceof ServerPlayer && looter == target)
                || !looter.isAlive()
                || looter.isRemoved()
                || looter.isSpectator()
                || !isLootable(target)
                || looter.serverLevel() != target.level()
                || looter.distanceToSqr(target) > maximumDistanceSquared
                || (requireLineOfSight && !looter.hasLineOfSight(target))) {
            return false;
        }
        return BodyStateCapability.get(looter).map(bodyState -> bodyState.canAct()).orElse(false);
    }

    private static LootTargetMenu createMenu(
            int containerId,
            net.minecraft.world.entity.player.Inventory inventory,
            Entity target
    ) {
        if (target instanceof CorpseEntity corpse) {
            return new LootTargetMenu(containerId, inventory, corpse);
        }
        return new LootTargetMenu(containerId, inventory, (ServerPlayer) target);
    }

    private static Component lootTitle(Entity target) {
        if (target instanceof CorpseEntity corpse) {
            return Component.translatable("screen.superficialtrauma.loot.corpse_title", corpse.ownerName());
        }
        return Component.translatable("screen.superficialtrauma.loot.title", target.getDisplayName());
    }
}
