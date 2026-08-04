package com.swampd.superficialtrauma.common.loot;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.body.BodyStateCapability;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkHooks;

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
        if (!(looter.serverLevel().getEntity(targetEntityId) instanceof ServerPlayer target)) {
            return false;
        }
        return tryOpen(looter, target);
    }

    public static boolean tryOpen(ServerPlayer looter, ServerPlayer target) {
        if (!canOpenLooting(looter, target)) {
            return false;
        }

        if (looter.containerMenu instanceof LootTargetMenu existingMenu
                && target.getUUID().equals(existingMenu.targetPlayerId())) {
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
                (containerId, inventory, player) -> new LootTargetMenu(containerId, inventory, target),
                Component.translatable("screen.superficialtrauma.loot.title", target.getDisplayName())
        );
        try {
            NetworkHooks.openScreen(looter, provider, buffer -> buffer.writeVarInt(target.getId()));
            return true;
        } catch (RuntimeException exception) {
            release(targetId, looterId);
            SuperficialTrauma.LOGGER.error(
                    "Failed to open loot menu for {} targeting {}",
                    looter.getGameProfile().getName(),
                    target.getGameProfile().getName(),
                    exception
            );
            return false;
        }
    }

    public static boolean isLootable(ServerPlayer target) {
        return target.isAlive()
                && !target.isRemoved()
                && BodyStateCapability.get(target).map(bodyState -> !bodyState.canAct()).orElse(false);
    }

    public static boolean canContinueLooting(Player looter, ServerPlayer target) {
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

    private static boolean canOpenLooting(ServerPlayer looter, ServerPlayer target) {
        return isEligible(looter, target, MAX_OPEN_DISTANCE_SQUARED, true);
    }

    private static boolean isEligible(
            ServerPlayer looter,
            ServerPlayer target,
            double maximumDistanceSquared,
            boolean requireLineOfSight
    ) {
        if (looter == target
                || !looter.isAlive()
                || looter.isRemoved()
                || looter.isSpectator()
                || !isLootable(target)
                || looter.serverLevel() != target.serverLevel()
                || looter.distanceToSqr(target) > maximumDistanceSquared
                || (requireLineOfSight && !looter.hasLineOfSight(target))) {
            return false;
        }
        return BodyStateCapability.get(looter).map(bodyState -> bodyState.canAct()).orElse(false);
    }
}
