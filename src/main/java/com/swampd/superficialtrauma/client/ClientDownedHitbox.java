package com.swampd.superficialtrauma.client;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.body.DownedGeometry;
import com.swampd.superficialtrauma.common.body.DownedPoseSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
        modid = SuperficialTrauma.MOD_ID,
        value = Dist.CLIENT
)
public final class ClientDownedHitbox {
    private static final float DIMENSION_EPSILON = 0.001F;

    private ClientDownedHitbox() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntitySize(EntityEvent.Size event) {
        if (event.getEntity() instanceof Player player
                && ClientDownedPoses.get(player.getId()).isPresent()) {
            event.setNewSize(DownedGeometry.ENTITY_DIMENSIONS);

        }
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        for (Player player : minecraft.level.players()) {
            ClientDownedPoses.get(player.getId()).ifPresent(snapshot -> apply(player, snapshot));
        }
    }

    public static void onPoseChanged(int playerEntityId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Entity entity = minecraft.level.getEntity(playerEntityId);
        if (!(entity instanceof Player player)) {
            return;
        }

        player.refreshDimensions();
        ClientDownedPoses.get(playerEntityId).ifPresent(snapshot -> apply(player, snapshot));
    }

    private static void apply(Player player, DownedPoseSnapshot snapshot) {
        if (Math.abs(player.getBbWidth() - DownedGeometry.BODY_WIDTH) > DIMENSION_EPSILON
                || Math.abs(player.getBbHeight() - DownedGeometry.BODY_HEIGHT) > DIMENSION_EPSILON
                || Math.abs(player.getEyeHeight() - DownedGeometry.EYE_HEIGHT) > DIMENSION_EPSILON) {
            player.refreshDimensions();
        }
        player.setBoundingBox(DownedGeometry.boundingBox(player, snapshot));
    }
}
