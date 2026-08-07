package com.swampd.superficialtrauma.common.body;

import com.swampd.superficialtrauma.common.wound.WoundInstance;
import com.swampd.superficialtrauma.common.wound.WoundType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.UUID;

/**
 * A bounded forensic summary of one wound. Unlike {@link WoundInstance}, this entry remains after healing.
 */
public record WoundHistoryEntry(
        UUID woundId,
        WoundType type,
        int severity,
        float accumulatedDamage,
        long createdGameTime,
        long lastTraumaGameTime,
        long healedGameTime,
        boolean fragmentationEligible,
        boolean closeRangeShot
) {
    private static final String TAG_WOUND_ID = "WoundId";
    private static final String TAG_TYPE = "Type";
    private static final String TAG_SEVERITY = "Severity";
    private static final String TAG_ACCUMULATED_DAMAGE = "AccumulatedDamage";
    private static final String TAG_CREATED_GAME_TIME = "CreatedGameTime";
    private static final String TAG_LAST_TRAUMA_GAME_TIME = "LastTraumaGameTime";
    private static final String TAG_HEALED_GAME_TIME = "HealedGameTime";
    private static final String TAG_FRAGMENTATION_ELIGIBLE = "FragmentationEligible";
    private static final String TAG_CLOSE_RANGE_SHOT = "CloseRangeShot";

    public WoundHistoryEntry {
        if (woundId == null) {
            throw new IllegalArgumentException("woundId cannot be null");
        }
        type = type == null ? WoundType.BLUNT : type;
        severity = Math.max(1, Math.min(3, severity));
        accumulatedDamage = Math.max(0.0F, accumulatedDamage);
        createdGameTime = Math.max(0L, createdGameTime);
        lastTraumaGameTime = Math.max(createdGameTime, lastTraumaGameTime);
        healedGameTime = healedGameTime < 0L ? -1L : Math.max(lastTraumaGameTime, healedGameTime);
    }

    public static WoundHistoryEntry active(WoundInstance wound, long lastTraumaGameTime) {
        return fromWound(wound, lastTraumaGameTime, -1L);
    }

    public WoundHistoryEntry refreshed(WoundInstance wound, long lastTraumaGameTime) {
        if (!woundId.equals(wound.id())) {
            throw new IllegalArgumentException("Cannot refresh wound history from a different wound");
        }
        return fromWound(wound, lastTraumaGameTime, healedGameTime);
    }

    public WoundHistoryEntry healed(WoundInstance wound, long gameTime) {
        if (!woundId.equals(wound.id())) {
            throw new IllegalArgumentException("Cannot heal wound history from a different wound");
        }
        return fromWound(wound, lastTraumaGameTime, gameTime);
    }

    public boolean healed() {
        return healedGameTime >= 0L;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(TAG_WOUND_ID, woundId);
        tag.putString(TAG_TYPE, type.serializedName());
        tag.putInt(TAG_SEVERITY, severity);
        tag.putFloat(TAG_ACCUMULATED_DAMAGE, accumulatedDamage);
        tag.putLong(TAG_CREATED_GAME_TIME, createdGameTime);
        tag.putLong(TAG_LAST_TRAUMA_GAME_TIME, lastTraumaGameTime);
        tag.putLong(TAG_HEALED_GAME_TIME, healedGameTime);
        tag.putBoolean(TAG_FRAGMENTATION_ELIGIBLE, fragmentationEligible);
        tag.putBoolean(TAG_CLOSE_RANGE_SHOT, closeRangeShot);
        return tag;
    }

    public static WoundHistoryEntry deserializeNBT(CompoundTag tag) {
        if (!tag.hasUUID(TAG_WOUND_ID)) {
            throw new IllegalArgumentException("Wound history entry is missing its wound UUID");
        }
        long createdGameTime = Math.max(0L, tag.getLong(TAG_CREATED_GAME_TIME));
        long lastTraumaGameTime = tag.contains(TAG_LAST_TRAUMA_GAME_TIME, Tag.TAG_ANY_NUMERIC)
                ? tag.getLong(TAG_LAST_TRAUMA_GAME_TIME)
                : createdGameTime;
        long healedGameTime = tag.contains(TAG_HEALED_GAME_TIME, Tag.TAG_ANY_NUMERIC)
                ? tag.getLong(TAG_HEALED_GAME_TIME)
                : -1L;
        return new WoundHistoryEntry(
                tag.getUUID(TAG_WOUND_ID),
                WoundType.fromSerializedName(tag.getString(TAG_TYPE)),
                tag.getInt(TAG_SEVERITY),
                tag.getFloat(TAG_ACCUMULATED_DAMAGE),
                createdGameTime,
                lastTraumaGameTime,
                healedGameTime,
                tag.getBoolean(TAG_FRAGMENTATION_ELIGIBLE),
                tag.getBoolean(TAG_CLOSE_RANGE_SHOT)
        );
    }

    private static WoundHistoryEntry fromWound(
            WoundInstance wound,
            long lastTraumaGameTime,
            long healedGameTime
    ) {
        if (wound == null) {
            throw new IllegalArgumentException("wound cannot be null");
        }
        return new WoundHistoryEntry(
                wound.id(),
                wound.type(),
                wound.severity(),
                wound.accumulatedDamage(),
                wound.createdGameTime(),
                lastTraumaGameTime,
                healedGameTime,
                wound.fragmentationEligible(),
                wound.closeRangeShot()
        );
    }
}
