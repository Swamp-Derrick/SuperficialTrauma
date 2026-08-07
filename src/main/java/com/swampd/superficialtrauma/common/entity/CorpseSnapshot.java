package com.swampd.superficialtrauma.common.entity;

import com.swampd.superficialtrauma.common.body.DownedFallDirection;
import com.swampd.superficialtrauma.common.body.DownedPoseSnapshot;
import com.swampd.superficialtrauma.common.body.DownedPosture;
import com.swampd.superficialtrauma.common.body.DowningHitRecord;
import com.swampd.superficialtrauma.common.body.WoundHistoryEntry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Immutable identity and pose data that must survive after the owning player logs out.
 */
public record CorpseSnapshot(
        UUID ownerId,
        String ownerName,
        String skinTextureValue,
        String skinTextureSignature,
        long deathGameTime,
        DownedPoseSnapshot downedPose,
        List<WoundHistoryEntry> woundHistory,
        DowningHitRecord downingHitRecord
) {
    public static final int CURRENT_DATA_VERSION = 4;
    private static final String TAG_DATA_VERSION = "DataVersion";
    private static final String TAG_OWNER_ID = "OwnerId";
    private static final String TAG_OWNER_NAME = "OwnerName";
    private static final String TAG_SKIN_VALUE = "SkinTextureValue";
    private static final String TAG_SKIN_SIGNATURE = "SkinTextureSignature";
    private static final String TAG_DEATH_GAME_TIME = "DeathGameTime";
    private static final String TAG_DOWNED_GAME_TIME = "DownedGameTime";
    private static final String TAG_BODY_YAW = "BodyYaw";
    private static final String TAG_POSTURE = "Posture";
    private static final String TAG_FALL_DIRECTION = "FallDirection";
    private static final String TAG_WOUND_HISTORY = "WoundHistory";
    private static final String TAG_DOWNING_HIT = "DowningHit";

    public CorpseSnapshot {
        ownerId = ownerId == null ? new UUID(0L, 0L) : ownerId;
        ownerName = ownerName == null || ownerName.isBlank() ? "Unknown" : ownerName;
        skinTextureValue = skinTextureValue == null ? "" : skinTextureValue;
        skinTextureSignature = skinTextureSignature == null ? "" : skinTextureSignature;
        deathGameTime = Math.max(0L, deathGameTime);
        downedPose = downedPose == null
                ? new DownedPoseSnapshot(deathGameTime, 0.0F, DownedPosture.UNSAFE, DownedFallDirection.FADE_ONLY)
                : downedPose;
        List<WoundHistoryEntry> normalizedHistory = woundHistory == null
                ? List.of()
                : woundHistory.stream().filter(entry -> entry != null).toList();
        int firstRetained = Math.max(0, normalizedHistory.size() - 6);
        woundHistory = List.copyOf(normalizedHistory.subList(firstRetained, normalizedHistory.size()));
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_DATA_VERSION, CURRENT_DATA_VERSION);
        tag.putUUID(TAG_OWNER_ID, ownerId);
        tag.putString(TAG_OWNER_NAME, ownerName);
        tag.putString(TAG_SKIN_VALUE, skinTextureValue);
        tag.putString(TAG_SKIN_SIGNATURE, skinTextureSignature);
        tag.putLong(TAG_DEATH_GAME_TIME, deathGameTime);
        tag.putLong(TAG_DOWNED_GAME_TIME, downedPose.downedGameTime());
        tag.putFloat(TAG_BODY_YAW, downedPose.bodyYaw());
        tag.putString(TAG_POSTURE, downedPose.posture().serializedName());
        tag.putString(TAG_FALL_DIRECTION, downedPose.fallDirection().serializedName());
        ListTag historyTag = new ListTag();
        for (WoundHistoryEntry entry : woundHistory) {
            historyTag.add(entry.serializeNBT());
        }
        tag.put(TAG_WOUND_HISTORY, historyTag);
        if (downingHitRecord != null) {
            tag.put(TAG_DOWNING_HIT, downingHitRecord.serializeNBT());
        }
        return tag;
    }

    public static CorpseSnapshot load(CompoundTag tag) {
        UUID ownerId = tag.hasUUID(TAG_OWNER_ID) ? tag.getUUID(TAG_OWNER_ID) : new UUID(0L, 0L);
        long deathGameTime = Math.max(0L, tag.getLong(TAG_DEATH_GAME_TIME));
        long downedGameTime = tag.contains(TAG_DOWNED_GAME_TIME, Tag.TAG_ANY_NUMERIC)
                ? Math.max(0L, tag.getLong(TAG_DOWNED_GAME_TIME))
                : deathGameTime;
        DownedPoseSnapshot pose = new DownedPoseSnapshot(
                downedGameTime,
                tag.getFloat(TAG_BODY_YAW),
                DownedPosture.fromSerializedName(tag.getString(TAG_POSTURE)),
                DownedFallDirection.fromSerializedName(tag.getString(TAG_FALL_DIRECTION))
        );
        List<WoundHistoryEntry> history = new ArrayList<>();
        ListTag historyTag = tag.getList(TAG_WOUND_HISTORY, Tag.TAG_COMPOUND);
        for (int index = 0; index < historyTag.size(); index++) {
            try {
                history.add(WoundHistoryEntry.deserializeNBT(historyTag.getCompound(index)));
            } catch (IllegalArgumentException ignored) {
                // Skip malformed legacy evidence without invalidating the corpse.
            }
        }
        DowningHitRecord downingHit = tag.contains(TAG_DOWNING_HIT, Tag.TAG_COMPOUND)
                ? DowningHitRecord.deserializeNBT(tag.getCompound(TAG_DOWNING_HIT))
                : null;
        return new CorpseSnapshot(
                ownerId,
                tag.getString(TAG_OWNER_NAME),
                tag.getString(TAG_SKIN_VALUE),
                tag.getString(TAG_SKIN_SIGNATURE),
                deathGameTime,
                pose,
                history,
                downingHit
        );
    }
}
