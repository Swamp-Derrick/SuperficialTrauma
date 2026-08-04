package com.swampd.superficialtrauma.common.damage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Groups final-damage events from the separate projectile entities spawned by one shotgun discharge.
 * This short pre-accumulation window is separate from BodyState's twenty-second wound window.
 */
public final class ShotgunVolleyAccumulator {
    public static final long QUIET_TICKS_BEFORE_RESOLUTION = 2L;

    private final Map<VolleyKey, PendingVolley> pendingVolleys = new LinkedHashMap<>();

    public void addHit(
            VolleyKey key,
            float finalDamage,
            int armorValue,
            double attackerDistance,
            long gameTime,
            String damageType
    ) {
        if (finalDamage <= 0.0F) {
            return;
        }
        PendingVolley volley = pendingVolleys.computeIfAbsent(
                Objects.requireNonNull(key, "key"),
                ignored -> new PendingVolley(
                        Math.max(0, armorValue),
                        normalizeDistance(attackerDistance),
                        gameTime,
                        normalizeDamageType(damageType)
                )
        );
        volley.addHit(finalDamage, armorValue, attackerDistance, gameTime);
    }

    public List<CompletedVolley> drainReady(UUID victimId, long gameTime) {
        List<CompletedVolley> completed = new ArrayList<>();
        Iterator<Map.Entry<VolleyKey, PendingVolley>> iterator = pendingVolleys.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<VolleyKey, PendingVolley> entry = iterator.next();
            VolleyKey key = entry.getKey();
            PendingVolley volley = entry.getValue();
            if (!key.victimId().equals(victimId) || !volley.isReady(gameTime)) {
                continue;
            }
            completed.add(volley.complete(key));
            iterator.remove();
        }
        return completed;
    }

    public int clearVictim(UUID victimId) {
        int previousSize = pendingVolleys.size();
        pendingVolleys.keySet().removeIf(key -> key.victimId().equals(victimId));
        return previousSize - pendingVolleys.size();
    }

    public void clear() {
        pendingVolleys.clear();
    }

    public int pendingVolleyCount() {
        return pendingVolleys.size();
    }

    private static double normalizeDistance(double distance) {
        return Double.isNaN(distance) || distance < 0.0D
                ? Double.POSITIVE_INFINITY
                : distance;
    }

    private static String normalizeDamageType(String damageType) {
        return damageType == null || damageType.isBlank() ? "unknown" : damageType;
    }

    public record VolleyKey(
            UUID victimId,
            UUID shooterId,
            String ammoId,
            String weaponId,
            long projectileSpawnGameTime
    ) {
        public VolleyKey {
            Objects.requireNonNull(victimId, "victimId");
            Objects.requireNonNull(shooterId, "shooterId");
            ammoId = ammoId == null || ammoId.isBlank() ? "unknown" : ammoId;
            weaponId = weaponId == null || weaponId.isBlank() ? "unknown" : weaponId;
        }
    }

    public record CompletedVolley(
            VolleyKey key,
            float totalFinalDamage,
            int pelletHits,
            int maximumArmorValue,
            double minimumAttackerDistance,
            long firstHitGameTime,
            long lastHitGameTime,
            String damageType
    ) {
    }

    private static final class PendingVolley {
        private float totalFinalDamage;
        private int pelletHits;
        private int maximumArmorValue;
        private double minimumAttackerDistance;
        private final long firstHitGameTime;
        private long lastHitGameTime;
        private final String damageType;

        private PendingVolley(
                int armorValue,
                double attackerDistance,
                long gameTime,
                String damageType
        ) {
            this.maximumArmorValue = Math.max(0, armorValue);
            this.minimumAttackerDistance = attackerDistance;
            this.firstHitGameTime = gameTime;
            this.lastHitGameTime = gameTime;
            this.damageType = damageType;
        }

        private void addHit(float damage, int armorValue, double attackerDistance, long gameTime) {
            totalFinalDamage += Math.max(0.0F, damage);
            pelletHits++;
            maximumArmorValue = Math.max(maximumArmorValue, Math.max(0, armorValue));
            minimumAttackerDistance = Math.min(
                    minimumAttackerDistance,
                    normalizeDistance(attackerDistance)
            );
            lastHitGameTime = Math.max(lastHitGameTime, gameTime);
        }

        private boolean isReady(long gameTime) {
            return gameTime >= lastHitGameTime
                    && gameTime - lastHitGameTime >= QUIET_TICKS_BEFORE_RESOLUTION;
        }

        private CompletedVolley complete(VolleyKey key) {
            return new CompletedVolley(
                    key,
                    totalFinalDamage,
                    pelletHits,
                    maximumArmorValue,
                    minimumAttackerDistance,
                    firstHitGameTime,
                    lastHitGameTime,
                    damageType
            );
        }
    }
}
