package com.swampd.superficialtrauma.common.wound;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public final class WoundInstance {
    private static final String TAG_ID = "Id";
    private static final String TAG_TYPE = "Type";
    private static final String TAG_SEVERITY = "Severity";
    private static final String TAG_ACCUMULATED_DAMAGE = "AccumulatedDamage";
    private static final String TAG_HEALING_PROGRESS = "HealingProgress";
    private static final String TAG_CREATED_GAME_TIME = "CreatedGameTime";
    private static final String TAG_WINDOW_END_GAME_TIME = "WindowEndGameTime";
    private static final String TAG_WOUND_TAGS = "WoundTags";

    private final UUID id;
    private final WoundType type;
    private int severity;
    private float accumulatedDamage;
    private float healingProgress;
    private final long createdGameTime;
    private final long windowEndGameTime;
    private final EnumSet<WoundTag> woundTags;

    private WoundInstance(
            UUID id,
            WoundType type,
            int severity,
            float accumulatedDamage,
            float healingProgress,
            long createdGameTime,
            long windowEndGameTime,
            EnumSet<WoundTag> woundTags
    ) {
        this.id = id;
        this.type = type;
        this.severity = Math.max(1, Math.min(3, severity));
        this.accumulatedDamage = Math.max(0.0F, accumulatedDamage);
        this.healingProgress = Math.max(0.0F, Math.min(100.0F, healingProgress));
        this.createdGameTime = createdGameTime;
        this.windowEndGameTime = Math.max(createdGameTime, windowEndGameTime);
        this.woundTags = woundTags.clone();
    }

    public static WoundInstance createBlunt(float accumulatedDamage, long createdGameTime, long windowEndGameTime) {
        int severity = bluntSeverityFor(accumulatedDamage);
        if (severity == 0) {
            throw new IllegalArgumentException("Blunt wounds require at least 1.5 final accumulated damage");
        }
        return new WoundInstance(
                UUID.randomUUID(),
                WoundType.BLUNT,
                severity,
                accumulatedDamage,
                100.0F,
                createdGameTime,
                windowEndGameTime,
                tagsForBluntSeverity(severity)
        );
    }

    public UUID id() {
        return id;
    }

    public WoundType type() {
        return type;
    }

    public int severity() {
        return severity;
    }

    public float accumulatedDamage() {
        return accumulatedDamage;
    }

    public float healingProgress() {
        return healingProgress;
    }

    public long createdGameTime() {
        return createdGameTime;
    }

    public long windowEndGameTime() {
        return windowEndGameTime;
    }

    public Set<WoundTag> woundTags() {
        return Collections.unmodifiableSet(woundTags);
    }

    public boolean isAccumulationWindowOpen(long gameTime) {
        return gameTime < windowEndGameTime;
    }

    public void addAccumulatedDamage(float amount) {
        accumulatedDamage = Math.max(0.0F, accumulatedDamage + amount);
        if (type == WoundType.BLUNT) {
            int newSeverity = bluntSeverityFor(accumulatedDamage);
            if (newSeverity > severity) {
                severity = newSeverity;
                woundTags.clear();
                woundTags.addAll(tagsForBluntSeverity(severity));
            }
        }
    }

    public String displayTranslationKey() {
        return "wound.superficialtrauma." + type.serializedName() + ".severity_" + severity;
    }

    public String triageTranslationKey() {
        return switch (severity) {
            case 3 -> "triage.superficialtrauma.immediate";
            case 2 -> "triage.superficialtrauma.soon";
            default -> "triage.superficialtrauma.observe";
        };
    }

    public static int bluntSeverityFor(float accumulatedDamage) {
        if (accumulatedDamage < 1.5F) {
            return 0;
        }
        if (accumulatedDamage < 4.0F) {
            return 1;
        }
        if (accumulatedDamage < 13.0F) {
            return 2;
        }
        return 3;
    }

    private static EnumSet<WoundTag> tagsForBluntSeverity(int severity) {
        return switch (severity) {
            case 2 -> EnumSet.of(WoundTag.SLOWNESS_1, WoundTag.PAIN_1);
            case 3 -> EnumSet.of(WoundTag.SLOWNESS_1, WoundTag.PAIN_1, WoundTag.MOVEMENT_BLEEDING_1);
            default -> EnumSet.noneOf(WoundTag.class);
        };
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(TAG_ID, id);
        tag.putString(TAG_TYPE, type.serializedName());
        tag.putInt(TAG_SEVERITY, severity);
        tag.putFloat(TAG_ACCUMULATED_DAMAGE, accumulatedDamage);
        tag.putFloat(TAG_HEALING_PROGRESS, healingProgress);
        tag.putLong(TAG_CREATED_GAME_TIME, createdGameTime);
        tag.putLong(TAG_WINDOW_END_GAME_TIME, windowEndGameTime);

        ListTag woundTagList = new ListTag();
        for (WoundTag woundTag : woundTags) {
            woundTagList.add(StringTag.valueOf(woundTag.serializedName()));
        }
        tag.put(TAG_WOUND_TAGS, woundTagList);
        return tag;
    }

    public static WoundInstance deserializeNBT(CompoundTag tag) {
        WoundType type = WoundType.fromSerializedName(tag.getString(TAG_TYPE));
        int severity = tag.getInt(TAG_SEVERITY);
        EnumSet<WoundTag> woundTags = EnumSet.noneOf(WoundTag.class);
        ListTag woundTagList = tag.getList(TAG_WOUND_TAGS, Tag.TAG_STRING);
        for (int i = 0; i < woundTagList.size(); i++) {
            try {
                woundTags.add(WoundTag.fromSerializedName(woundTagList.getString(i)));
            } catch (IllegalArgumentException ignored) {
                // Unknown future tags are ignored by this schema version.
            }
        }
        if (woundTags.isEmpty() && type == WoundType.BLUNT) {
            woundTags.addAll(tagsForBluntSeverity(severity));
        }

        UUID id = tag.hasUUID(TAG_ID) ? tag.getUUID(TAG_ID) : UUID.randomUUID();
        return new WoundInstance(
                id,
                type,
                severity,
                tag.getFloat(TAG_ACCUMULATED_DAMAGE),
                tag.contains(TAG_HEALING_PROGRESS, Tag.TAG_FLOAT) ? tag.getFloat(TAG_HEALING_PROGRESS) : 100.0F,
                tag.getLong(TAG_CREATED_GAME_TIME),
                tag.getLong(TAG_WINDOW_END_GAME_TIME),
                woundTags
        );
    }
}
