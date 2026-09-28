package com.swampd.superficialtrauma.common.damage;

import com.swampd.superficialtrauma.common.wound.WoundType;

import java.util.Optional;

public record DamageClassification(
        WoundType woundType,
        DamageKind kind,
        String reason,
        String projectileEntityId,
        String ammoId,
        String weaponId
) {
    public DamageClassification {
        kind = kind == null ? DamageKind.UNKNOWN : kind;
        reason = normalize(reason);
        projectileEntityId = normalize(projectileEntityId);
        ammoId = normalize(ammoId);
        weaponId = normalize(weaponId);
    }

    public static DamageClassification blunt(String reason) {
        return wound(WoundType.BLUNT, DamageKind.BLUNT, reason);
    }

    public static DamageClassification sharp(String reason) {
        return wound(WoundType.SHARP, DamageKind.SHARP, reason);
    }

    public static DamageClassification puncture(String reason) {
        return wound(WoundType.PUNCTURE, DamageKind.PUNCTURE, reason);
    }

    public static DamageClassification crush(String reason) {
        return wound(WoundType.CRUSH, DamageKind.CRUSH, reason);
    }

    public static DamageClassification frostbite(String reason) {
        return wound(WoundType.FROSTBITE, DamageKind.FROSTBITE, reason);
    }

    public static DamageClassification burn(String reason) {
        return wound(WoundType.BURN, DamageKind.BURN, reason);
    }

    public static DamageClassification explosion(String reason) {
        return wound(WoundType.EXPLOSION, DamageKind.EXPLOSION, reason);
    }

    private static DamageClassification wound(WoundType type, DamageKind kind, String reason) {
        return new DamageClassification(
                type,
                kind,
                reason,
                "none",
                "none",
                "none"
        );
    }

    public static DamageClassification cgmProjectile(
            DamageKind kind,
            String reason,
            CgmProjectileContext context
    ) {
        return cgmProjectile(
                kind,
                reason,
                context.projectileEntityId(),
                context.ammoId(),
                context.weaponId()
        );
    }

    public static DamageClassification cgmProjectile(
            DamageKind kind,
            String reason,
            String projectileEntityId,
            String ammoId,
            String weaponId
    ) {
        if (kind != DamageKind.CGM_LOW_VELOCITY
                && kind != DamageKind.CGM_HIGH_VELOCITY
                && kind != DamageKind.CGM_SHOTGUN
                && kind != DamageKind.CGM_UNCLASSIFIED) {
            throw new IllegalArgumentException("Not a CGM projectile damage kind: " + kind);
        }
        return new DamageClassification(null, kind, reason, projectileEntityId, ammoId, weaponId);
    }

    public static DamageClassification deferred(String reason) {
        return new DamageClassification(null, DamageKind.DEFERRED, reason, "none", "none", "none");
    }

    public static DamageClassification unknown(String reason) {
        return new DamageClassification(null, DamageKind.UNKNOWN, reason, "none", "none", "none");
    }

    public Optional<WoundType> woundTypeOptional() {
        return Optional.ofNullable(woundType);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? "none" : value;
    }
}
