package com.swampd.superficialtrauma.common.body;

import com.swampd.superficialtrauma.SuperficialTrauma;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SuperficialTrauma.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DownedHitbox {
    private static final float DIMENSION_EPSILON = 0.001F;

    private DownedHitbox() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntitySize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isDowned(player)) {
            return;
        }
        event.setNewSize(DownedGeometry.ENTITY_DIMENSIONS);
        event.setNewEyeHeight(DownedGeometry.EYE_HEIGHT);
    }

    public static void update(ServerPlayer player, BodyState bodyState) {
        if (bodyState.canAct()) {
            return;
        }
        bodyState.downedPoseSnapshot().ifPresent(snapshot -> {
            ensureLowDimensions(player);
            player.setBoundingBox(DownedGeometry.boundingBox(player, snapshot));
        });
    }

    public static void restore(ServerPlayer player) {
        player.refreshDimensions();
    }

    private static void ensureLowDimensions(ServerPlayer player) {
        if (Math.abs(player.getBbWidth() - DownedGeometry.BODY_WIDTH) > DIMENSION_EPSILON
                || Math.abs(player.getBbHeight() - DownedGeometry.BODY_HEIGHT) > DIMENSION_EPSILON
                || Math.abs(player.getEyeHeight() - DownedGeometry.EYE_HEIGHT) > DIMENSION_EPSILON) {
            player.refreshDimensions();
        }
    }

    private static boolean isDowned(ServerPlayer player) {
        return BodyStateCapability.get(player)
                .map(bodyState -> !bodyState.canAct())
                .orElse(false);
    }
}
