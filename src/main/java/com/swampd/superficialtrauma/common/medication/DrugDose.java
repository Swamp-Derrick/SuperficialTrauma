package com.swampd.superficialtrauma.common.medication;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public record DrugDose(MedicationType type, long expiresGameTime) {
    private static final String TAG_TYPE = "Type";
    private static final String TAG_EXPIRES_GAME_TIME = "ExpiresGameTime";

    public DrugDose {
        type = type == null ? MedicationType.PARACETAMOL : type;
        expiresGameTime = Math.max(0L, expiresGameTime);
    }

    public boolean isExpired(long gameTime) {
        return gameTime >= expiresGameTime;
    }

    public DrugDose shifted(long deltaTicks) {
        if (deltaTicks <= 0L) {
            return this;
        }
        long shifted = expiresGameTime > Long.MAX_VALUE - deltaTicks
                ? Long.MAX_VALUE
                : expiresGameTime + deltaTicks;
        return new DrugDose(type, shifted);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_TYPE, type.serializedName());
        tag.putLong(TAG_EXPIRES_GAME_TIME, expiresGameTime);
        return tag;
    }

    public static DrugDose load(CompoundTag tag) {
        MedicationType type = MedicationType.fromSerializedName(tag.getString(TAG_TYPE));
        long expiresGameTime = tag.contains(TAG_EXPIRES_GAME_TIME, Tag.TAG_ANY_NUMERIC)
                ? tag.getLong(TAG_EXPIRES_GAME_TIME)
                : 0L;
        return new DrugDose(type, expiresGameTime);
    }
}
