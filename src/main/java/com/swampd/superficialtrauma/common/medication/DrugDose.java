package com.swampd.superficialtrauma.common.medication;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public record DrugDose(MedicationType type, long expiresGameTime, boolean grantsMovementSpeed) {
    private static final String TAG_TYPE = "Type";
    private static final String TAG_EXPIRES_GAME_TIME = "ExpiresGameTime";
    private static final String TAG_GRANTS_MOVEMENT_SPEED = "GrantsMovementSpeed";

    public DrugDose(MedicationType type, long expiresGameTime) {
        this(type, expiresGameTime, type != MedicationType.EPINEPHRINE);
    }

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
        return new DrugDose(type, shifted, grantsMovementSpeed);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_TYPE, type.serializedName());
        tag.putLong(TAG_EXPIRES_GAME_TIME, expiresGameTime);
        tag.putBoolean(TAG_GRANTS_MOVEMENT_SPEED, grantsMovementSpeed);
        return tag;
    }

    public static DrugDose load(CompoundTag tag) {
        MedicationType type = MedicationType.fromSerializedName(tag.getString(TAG_TYPE));
        long expiresGameTime = tag.contains(TAG_EXPIRES_GAME_TIME, Tag.TAG_ANY_NUMERIC)
                ? tag.getLong(TAG_EXPIRES_GAME_TIME)
                : 0L;
        boolean grantsMovementSpeed = tag.contains(TAG_GRANTS_MOVEMENT_SPEED, Tag.TAG_BYTE)
                ? tag.getBoolean(TAG_GRANTS_MOVEMENT_SPEED)
                : type != MedicationType.EPINEPHRINE;
        return new DrugDose(type, expiresGameTime, grantsMovementSpeed);
    }
}
