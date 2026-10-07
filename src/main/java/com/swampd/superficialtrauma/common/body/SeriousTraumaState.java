package com.swampd.superficialtrauma.common.body;

import com.swampd.superficialtrauma.common.damage.GunshotRegion;
import net.minecraft.nbt.CompoundTag;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

/** Independent regional damage pools and persistent, unique whole-body conditions. */
public final class SeriousTraumaState {
    public enum Condition {
        CONCUSSION(GunshotRegion.HEAD, 10.0F),
        PNEUMOTHORAX(GunshotRegion.CHEST, 15.0F);

        private final GunshotRegion region;
        private final float threshold;

        Condition(GunshotRegion region, float threshold) {
            this.region = region;
            this.threshold = threshold;
        }

        public String translationKey() {
            return "serious_trauma.superficialtrauma." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private boolean enabled;
    public static final long CONCUSSION_DURATION_TICKS = 180L * 20L;
    private long concussionStartedAt = -1, concussionEndsAt = -1;
    private boolean pneumothoraxSealed;
    private float pneumothoraxFloor;
    private final EnumSet<Condition> conditions = EnumSet.noneOf(Condition.class);
    private final EnumMap<GunshotRegion, Pool> pools = new EnumMap<>(GunshotRegion.class);
    // Shotgun volleys settle after their impacts. Keep one just-closed window so a late
    // settlement uses the actual pellet time, not the resolution time or a newer pool.
    private final EnumMap<GunshotRegion, Pool> retiredPools = new EnumMap<>(GunshotRegion.class);

    public boolean enabled() { return enabled; }
    public void addDowningConcussion() {
        addDowningConcussion(0);
    }
    public void addDowningConcussion(long now) {
        if (enabled && conditions.add(Condition.CONCUSSION)) {
            concussionStartedAt = now;
            concussionEndsAt = now + CONCUSSION_DURATION_TICKS;
        }
    }
    public boolean hasConcussion() { return conditions.contains(Condition.CONCUSSION); }
    public long concussionStartedAt() { return concussionStartedAt; }
    public long concussionEndsAt() { return concussionEndsAt; }
    public void externalInjury(long now) {
        if (hasConcussion()) concussionEndsAt = now + CONCUSSION_DURATION_TICKS;
    }
    public boolean hasPneumothorax() { return conditions.contains(Condition.PNEUMOTHORAX); }
    public boolean sealed() { return hasPneumothorax() && pneumothoraxSealed; }
    public boolean openPneumothorax() { return hasPneumothorax() && !pneumothoraxSealed; }
    public float respiratoryFloor() { return sealed() ? pneumothoraxFloor : BodyState.MIN_RESPIRATORY_DISTRESS; }
    public boolean updateSeal(boolean sealed, float respiratoryDistress) {
        if (!hasPneumothorax() || sealed == pneumothoraxSealed) return false;
        pneumothoraxSealed = sealed;
        pneumothoraxFloor = sealed ? Math.max(-10, Math.min(10, respiratoryDistress)) : 0;
        return true;
    }
    public void curePneumothorax() {
        conditions.remove(Condition.PNEUMOTHORAX);
        pneumothoraxSealed = false;
        pneumothoraxFloor = 0;
        pools.remove(GunshotRegion.CHEST);
        retiredPools.remove(GunshotRegion.CHEST);
    }
    public void shiftTimers(long ticks) {
        if (concussionStartedAt >= 0) concussionStartedAt += ticks;
        if (concussionEndsAt >= 0) concussionEndsAt += ticks;
    }
    public Set<Condition> conditions() { return Collections.unmodifiableSet(conditions); }
    public float damage(GunshotRegion region) { return pools.containsKey(region) ? pools.get(region).damage : 0.0F; }

    public boolean configure(boolean value) {
        if (enabled == value) return false;
        enabled = value;
        if (!value) clear();
        return true;
    }

    public void clear() {
        concussionStartedAt = concussionEndsAt = -1;
        pneumothoraxSealed = false;
        pneumothoraxFloor = 0;
        conditions.clear();
        pools.clear();
        retiredPools.clear();
    }

    public boolean expire(long gameTime) {
        retiredPools.values().removeIf(pool -> gameTime < pool.start
                || gameTime - pool.end >= BodyState.DAMAGE_WINDOW_TICKS);
        boolean changed = false;
        if (hasConcussion()) {
            if (concussionEndsAt < 0) {
                concussionEndsAt = gameTime + CONCUSSION_DURATION_TICKS; // Legacy metadata-only saves.
                changed = true;
            } else if (gameTime >= concussionEndsAt) {
                conditions.remove(Condition.CONCUSSION);
                concussionStartedAt = concussionEndsAt = -1;
                pools.remove(GunshotRegion.HEAD);
                retiredPools.remove(GunshotRegion.HEAD);
                changed = true;
            }
        }
        var iterator = pools.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Pool pool = entry.getValue();
            if (gameTime >= pool.end || gameTime < pool.start) {
                if (gameTime >= pool.end) retiredPools.put(entry.getKey(), pool);
                iterator.remove();
                changed = true;
            }
        }
        return changed;
    }

    public boolean record(GunshotRegion region, float damage, long gameTime) {
        if (!enabled || region == null || !Float.isFinite(damage) || damage <= 0) return false;
        Pool retired = retiredPools.get(region);
        Pool pool = pools.get(region);
        if (retired != null && gameTime >= retired.start && gameTime < retired.end) {
            pool = retired;
        } else if (pool != null && gameTime < pool.start) {
            if (pool.lastHit - gameTime >= BodyState.DAMAGE_WINDOW_TICKS) return false;
            // The first hit arrived through a delayed volley, after another ammunition type.
            pool.start = gameTime;
            pool.end = gameTime + BodyState.DAMAGE_WINDOW_TICKS;
        } else if (pool == null || gameTime >= pool.end) {
            if (pool != null) retiredPools.put(region, pool);
            pool = new Pool(0, gameTime, gameTime + BodyState.DAMAGE_WINDOW_TICKS);
            pools.put(region, pool);
        }
        pool.damage = Math.min(Float.MAX_VALUE, pool.damage + damage);
        pool.lastHit = Math.max(pool.lastHit, gameTime);
        for (Condition condition : Condition.values()) {
            // Strictly greater: 10 head damage / 15 chest damage alone do not trigger a condition.
            if (condition.region == region && pool.damage > condition.threshold) {
                if (condition == Condition.CONCUSSION) addDowningConcussion(gameTime);
                else conditions.add(condition);
            }
        }
        return true;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Enabled", enabled);
        if (!enabled) return tag;
        for (Condition condition : conditions) tag.putBoolean(condition.name(), true);
        tag.putLong("ConcussionStartedAt", concussionStartedAt);
        tag.putLong("ConcussionEndsAt", concussionEndsAt);
        tag.putBoolean("PneumothoraxSealed", pneumothoraxSealed);
        tag.putFloat("PneumothoraxFloor", pneumothoraxFloor);
        for (var entry : pools.entrySet()) {
            CompoundTag pool = new CompoundTag();
            pool.putFloat("Damage", entry.getValue().damage);
            pool.putLong("Start", entry.getValue().start);
            pool.putLong("End", entry.getValue().end);
            tag.put(entry.getKey().serializedName(), pool);
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        clear();
        enabled = tag.getBoolean("Enabled");
        if (!enabled) return;
        for (Condition condition : Condition.values()) {
            if (tag.getBoolean(condition.name())) conditions.add(condition);
        }
        concussionStartedAt = tag.contains("ConcussionStartedAt") ? tag.getLong("ConcussionStartedAt") : -1;
        concussionEndsAt = tag.contains("ConcussionEndsAt") ? tag.getLong("ConcussionEndsAt") : -1;
        pneumothoraxSealed = hasPneumothorax() && tag.getBoolean("PneumothoraxSealed");
        float floor = tag.getFloat("PneumothoraxFloor");
        pneumothoraxFloor = Float.isFinite(floor) ? Math.max(-10, Math.min(10, floor)) : 0;
        for (GunshotRegion region : GunshotRegion.values()) {
            CompoundTag pool = tag.getCompound(region.serializedName());
            float damage = pool.getFloat("Damage");
            long start = pool.getLong("Start"), end = pool.getLong("End");
            if (Float.isFinite(damage) && damage > 0 && start >= 0 && end > start
                    && end - start <= BodyState.DAMAGE_WINDOW_TICKS) {
                pools.put(region, new Pool(damage, start, end));
            }
        }
    }

    private static final class Pool {
        private float damage;
        private long start, end, lastHit;
        private Pool(float damage, long start, long end) {
            this.damage = damage;
            this.start = start;
            this.end = end;
            this.lastHit = start;
        }
    }
}
