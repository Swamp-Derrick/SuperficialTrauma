package com.swampd.superficialtrauma.common.damage;

import com.swampd.superficialtrauma.common.wound.WoundType;
import net.minecraft.nbt.CompoundTag;

public final class DamageWindow {
    private static final String TAG_TYPE = "Type";
    private static final String TAG_ACCUMULATED_DAMAGE = "AccumulatedDamage";
    private static final String TAG_STARTED_GAME_TIME = "StartedGameTime";
    private static final String TAG_END_GAME_TIME = "EndGameTime";

    private final WoundType type;
    private float accumulatedDamage;
    private final long startedGameTime;
    private final long endGameTime;

    public DamageWindow(WoundType type, float accumulatedDamage, long startedGameTime, long endGameTime) {
        this.type = type;
        this.accumulatedDamage = Math.max(0.0F, accumulatedDamage);
        this.startedGameTime = startedGameTime;
        this.endGameTime = Math.max(startedGameTime, endGameTime);
    }

    public WoundType type() {
        return type;
    }

    public float accumulatedDamage() {
        return accumulatedDamage;
    }

    public long startedGameTime() {
        return startedGameTime;
    }

    public long endGameTime() {
        return endGameTime;
    }

    public boolean isOpen(long gameTime) {
        return gameTime < endGameTime;
    }

    public void addDamage(float amount) {
        accumulatedDamage = Math.max(0.0F, accumulatedDamage + amount);
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_TYPE, type.serializedName());
        tag.putFloat(TAG_ACCUMULATED_DAMAGE, accumulatedDamage);
        tag.putLong(TAG_STARTED_GAME_TIME, startedGameTime);
        tag.putLong(TAG_END_GAME_TIME, endGameTime);
        return tag;
    }

    public static DamageWindow deserializeNBT(CompoundTag tag) {
        return new DamageWindow(
                WoundType.fromSerializedName(tag.getString(TAG_TYPE)),
                tag.getFloat(TAG_ACCUMULATED_DAMAGE),
                tag.getLong(TAG_STARTED_GAME_TIME),
                tag.getLong(TAG_END_GAME_TIME)
        );
    }
}
