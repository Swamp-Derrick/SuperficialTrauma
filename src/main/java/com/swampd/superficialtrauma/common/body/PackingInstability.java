package com.swampd.superficialtrauma.common.body;

import net.minecraft.nbt.CompoundTag;

/** Basic treatment rule; deliberately independent of the serious-trauma configuration. */
public final class PackingInstability {
    private double value;
    private long cooldownUntil = -1, lastTick = -1;
    public double value() { return value; }
    public long cooldownUntil() { return cooldownUntil; }
    public boolean tick(long now, boolean strenuous) {
        if (lastTick < 0 || now < lastTick) { lastTick = now; return false; }
        long from = Math.max(lastTick, cooldownUntil);
        lastTick = now;
        if (now <= from) return false;
        return add((now - from) / 20D * (strenuous ? 1 : -.5), now);
    }
    public boolean externalDamage(float damage, long now) {
        return damage > 1 && Float.isFinite(damage) && add(5, now);
    }
    private boolean add(double amount, long now) {
        if (now < cooldownUntil) return false;
        value = Math.max(0, value + amount);
        if (value + 1E-9 < 5) return false;
        value = 0;
        cooldownUntil = now + 200;
        return true;
    }
    public void resume(long now, long pausedTicks) {
        if (cooldownUntil >= 0) cooldownUntil += pausedTicks;
        lastTick = now;
    }
    public void clear() { value = 0; cooldownUntil = lastTick = -1; }
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putDouble("Value", value);
        tag.putLong("CooldownUntil", cooldownUntil);
        return tag;
    }
    public void load(CompoundTag tag) {
        clear();
        double saved = tag.getDouble("Value");
        value = Double.isFinite(saved) ? Math.max(0, Math.min(5, saved)) : 0;
        cooldownUntil = tag.contains("CooldownUntil") ? tag.getLong("CooldownUntil") : -1;
    }
}
