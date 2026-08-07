package com.swampd.superficialtrauma.common.entity;

import com.mojang.authlib.properties.Property;
import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.body.BodyState;
import com.swampd.superficialtrauma.common.body.DownedFallDirection;
import com.swampd.superficialtrauma.common.body.DownedPoseSnapshot;
import com.swampd.superficialtrauma.common.body.DownedPosture;
import com.swampd.superficialtrauma.common.init.ModEntities;
import net.minecraft.server.level.ServerPlayer;

public final class CorpseService {
    private CorpseService() {
    }

    public static boolean spawn(ServerPlayer player, BodyState bodyState) {
        DownedPoseSnapshot pose = bodyState.downedPoseSnapshot().orElseGet(() ->
                new DownedPoseSnapshot(
                        player.serverLevel().getGameTime(),
                        player.getYRot(),
                        DownedPosture.UNSAFE,
                        DownedFallDirection.FADE_ONLY
                )
        );
        return spawn(player, pose);
    }

    public static boolean spawnPreview(ServerPlayer player, DownedFallDirection direction) {
        return spawn(
                player,
                new DownedPoseSnapshot(
                        player.serverLevel().getGameTime(),
                        player.getYRot(),
                        DownedPosture.STANDING,
                        direction
                )
        );
    }

    private static boolean spawn(ServerPlayer player, DownedPoseSnapshot pose) {
        CorpseEntity corpse = ModEntities.CORPSE.get().create(player.serverLevel());
        if (corpse == null) {
            SuperficialTrauma.LOGGER.error("Could not create corpse entity for {}", player.getGameProfile().getName());
            return false;
        }

        Property skinTexture = firstSkinTexture(player);
        CorpseSnapshot snapshot = new CorpseSnapshot(
                player.getUUID(),
                player.getGameProfile().getName(),
                skinTexture == null ? "" : skinTexture.getValue(),
                skinTexture == null || !skinTexture.hasSignature() ? "" : skinTexture.getSignature(),
                player.serverLevel().getGameTime(),
                pose
        );
        corpse.initialize(snapshot, player.getX(), player.getY(), player.getZ());
        return player.serverLevel().addFreshEntity(corpse);
    }

    private static Property firstSkinTexture(ServerPlayer player) {
        for (Property property : player.getGameProfile().getProperties().get("textures")) {
            return property;
        }
        return null;
    }
}
