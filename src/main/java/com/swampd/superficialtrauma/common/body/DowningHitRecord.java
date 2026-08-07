package com.swampd.superficialtrauma.common.body;

import com.swampd.superficialtrauma.common.damage.DamageKind;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * Immutable evidence for the external hit that first changed an active player into an incapacitated player.
 */
public record DowningHitRecord(
        float finalDamage,
        String damageType,
        DamageKind damageKind,
        String damageReason,
        String projectileEntityId,
        String ammoId,
        String weaponId,
        double attackerDistance,
        long gameTime
) {
    public static final double UNKNOWN_DISTANCE = -1.0D;

    private static final String TAG_FINAL_DAMAGE = "FinalDamage";
    private static final String TAG_DAMAGE_TYPE = "DamageType";
    private static final String TAG_DAMAGE_KIND = "DamageKind";
    private static final String TAG_DAMAGE_REASON = "DamageReason";
    private static final String TAG_PROJECTILE_ENTITY_ID = "ProjectileEntityId";
    private static final String TAG_AMMO_ID = "AmmoId";
    private static final String TAG_WEAPON_ID = "WeaponId";
    private static final String TAG_ATTACKER_DISTANCE = "AttackerDistance";
    private static final String TAG_GAME_TIME = "GameTime";

    public DowningHitRecord {
        finalDamage = Math.max(0.0F, finalDamage);
        damageType = normalize(damageType);
        damageKind = damageKind == null ? DamageKind.UNKNOWN : damageKind;
        damageReason = normalize(damageReason);
        projectileEntityId = normalize(projectileEntityId);
        ammoId = normalize(ammoId);
        weaponId = normalize(weaponId);
        attackerDistance = normalizeDistance(attackerDistance);
        gameTime = Math.max(0L, gameTime);
    }

    public boolean hasKnownDistance() {
        return attackerDistance >= 0.0D;
    }

    public boolean isRanged() {
        return !"none".equals(projectileEntityId)
                || damageKind == DamageKind.CGM_LOW_VELOCITY
                || damageKind == DamageKind.CGM_HIGH_VELOCITY
                || damageKind == DamageKind.CGM_SHOTGUN
                || damageKind == DamageKind.CGM_UNCLASSIFIED;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat(TAG_FINAL_DAMAGE, finalDamage);
        tag.putString(TAG_DAMAGE_TYPE, damageType);
        tag.putString(TAG_DAMAGE_KIND, damageKind.serializedName());
        tag.putString(TAG_DAMAGE_REASON, damageReason);
        tag.putString(TAG_PROJECTILE_ENTITY_ID, projectileEntityId);
        tag.putString(TAG_AMMO_ID, ammoId);
        tag.putString(TAG_WEAPON_ID, weaponId);
        tag.putDouble(TAG_ATTACKER_DISTANCE, attackerDistance);
        tag.putLong(TAG_GAME_TIME, gameTime);
        return tag;
    }

    public static DowningHitRecord deserializeNBT(CompoundTag tag) {
        return new DowningHitRecord(
                tag.getFloat(TAG_FINAL_DAMAGE),
                getStringOrDefault(tag, TAG_DAMAGE_TYPE, "none"),
                DamageKind.fromSerializedName(getStringOrDefault(tag, TAG_DAMAGE_KIND, "unknown")),
                getStringOrDefault(tag, TAG_DAMAGE_REASON, "none"),
                getStringOrDefault(tag, TAG_PROJECTILE_ENTITY_ID, "none"),
                getStringOrDefault(tag, TAG_AMMO_ID, "none"),
                getStringOrDefault(tag, TAG_WEAPON_ID, "none"),
                tag.contains(TAG_ATTACKER_DISTANCE, Tag.TAG_ANY_NUMERIC)
                        ? tag.getDouble(TAG_ATTACKER_DISTANCE)
                        : UNKNOWN_DISTANCE,
                tag.getLong(TAG_GAME_TIME)
        );
    }

    public static double normalizeDistance(double distance) {
        return Double.isFinite(distance) && distance >= 0.0D ? distance : UNKNOWN_DISTANCE;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? "none" : value;
    }

    private static String getStringOrDefault(CompoundTag tag, String key, String defaultValue) {
        return tag.contains(key, Tag.TAG_STRING) ? tag.getString(key) : defaultValue;
    }
}
