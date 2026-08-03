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
    private static final String TAG_TRANSIENT_PAIN_END_GAME_TIME = "TransientPainEndGameTime";
    private static final long SHARP_LEVEL_ONE_PAIN_TICKS = 10L * 20L;

    private final UUID id;
    private final WoundType type;
    private int severity;
    private float accumulatedDamage;
    private float healingProgress;
    private final long createdGameTime;
    private final long windowEndGameTime;
    private final EnumSet<WoundTag> woundTags;
    private long transientPainEndGameTime;

    private WoundInstance(
            UUID id,
            WoundType type,
            int severity,
            float accumulatedDamage,
            float healingProgress,
            long createdGameTime,
            long windowEndGameTime,
            EnumSet<WoundTag> woundTags,
            long transientPainEndGameTime
    ) {
        this.id = id;
        this.type = type;
        this.severity = Math.max(1, Math.min(3, severity));
        this.accumulatedDamage = Math.max(0.0F, accumulatedDamage);
        this.healingProgress = Math.max(0.0F, Math.min(100.0F, healingProgress));
        this.createdGameTime = createdGameTime;
        this.windowEndGameTime = Math.max(createdGameTime, windowEndGameTime);
        this.woundTags = woundTags.clone();
        this.transientPainEndGameTime = transientPainEndGameTime;
    }

    public static WoundInstance createBlunt(float accumulatedDamage, long createdGameTime, long windowEndGameTime) {
        return create(WoundType.BLUNT, accumulatedDamage, createdGameTime, windowEndGameTime);
    }

    public static WoundInstance create(
            WoundType type,
            float accumulatedDamage,
            long createdGameTime,
            long windowEndGameTime
    ) {
        int severity = severityFor(type, accumulatedDamage);
        if (severity == 0) {
            throw new IllegalArgumentException(
                    type.serializedName() + " wounds do not meet their minimum accumulated-damage threshold"
            );
        }
        return new WoundInstance(
                UUID.randomUUID(),
                type,
                severity,
                accumulatedDamage,
                100.0F,
                createdGameTime,
                windowEndGameTime,
                tagsFor(type, severity),
                transientPainEndFor(type, severity, createdGameTime)
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

    public long transientPainEndGameTime() {
        return transientPainEndGameTime;
    }

    public boolean isAccumulationWindowOpen(long gameTime) {
        return gameTime < windowEndGameTime;
    }

    public void addAccumulatedDamage(float amount) {
        accumulatedDamage = Math.max(0.0F, accumulatedDamage + amount);
        int newSeverity = severityFor(type, accumulatedDamage);
        if (newSeverity > severity) {
            severity = newSeverity;
            woundTags.clear();
            woundTags.addAll(tagsFor(type, severity));
            transientPainEndGameTime = transientPainEndFor(type, severity, createdGameTime);
        }
    }

    public boolean expireTransientTags(long gameTime) {
        if (transientPainEndGameTime < 0L || gameTime < transientPainEndGameTime) {
            return false;
        }

        transientPainEndGameTime = -1L;
        return woundTags.remove(WoundTag.PAIN_1);
    }

    public void shiftTransientDeadlines(long deltaTicks) {
        if (transientPainEndGameTime >= 0L && deltaTicks > 0L) {
            transientPainEndGameTime += deltaTicks;
        }
    }

    public float baseHealingPerSecond() {
        return switch (type) {
            case BLUNT -> switch (severity) {
                case 1 -> 1.0F;
                case 2 -> 0.5F;
                default -> 0.2F;
            };
            case SHARP -> switch (severity) {
                case 1 -> 1.0F;
                case 2 -> 0.1F;
                default -> 0.0F;
            };
            case BURN -> switch (severity) {
                case 1 -> 0.8F;
                case 2 -> 0.5F;
                default -> 0.0F;
            };
            case EXPLOSION -> switch (severity) {
                case 1 -> 0.3F;
                case 2 -> 0.2F;
                default -> 0.0F;
            };
        };
    }

    public float minimumHealingProgressWithoutSkinGraft() {
        return type == WoundType.EXPLOSION && severity == 3 ? 10.0F : 0.0F;
    }

    public boolean advanceNaturalHealing(float elapsedSeconds) {
        if (elapsedSeconds <= 0.0F || healingProgress <= 0.0F) {
            return false;
        }

        float healingAmount = baseHealingPerSecond() * elapsedSeconds;
        if (healingAmount <= 0.0F) {
            return false;
        }

        float previousProgress = healingProgress;
        healingProgress = Math.max(0.0F, healingProgress - healingAmount);
        return healingProgress < previousProgress;
    }

    public boolean isHealed() {
        return healingProgress <= 0.0F;
    }

    public String displayTranslationKey() {
        return "wound.superficialtrauma." + type.serializedName() + ".severity_" + severity;
    }

    public String triageTranslationKey() {
        if (severity >= 3) {
            return "triage.superficialtrauma.immediate";
        }
        if (severity == 2 || woundTags.contains(WoundTag.NEEDS_DEBRIDEMENT_1)) {
            return "triage.superficialtrauma.soon";
        }
        return "triage.superficialtrauma.observe";
    }

    public static int bluntSeverityFor(float accumulatedDamage) {
        return severityFor(WoundType.BLUNT, accumulatedDamage);
    }

    public static int sharpSeverityFor(float accumulatedDamage) {
        return severityFor(WoundType.SHARP, accumulatedDamage);
    }

    public static int burnSeverityFor(float accumulatedDamage) {
        return severityFor(WoundType.BURN, accumulatedDamage);
    }

    public static int explosionSeverityFor(float accumulatedDamage) {
        return severityFor(WoundType.EXPLOSION, accumulatedDamage);
    }

    public static int severityFor(WoundType type, float accumulatedDamage) {
        return switch (type) {
            case BLUNT -> thresholdSeverity(accumulatedDamage, 1.5F, 4.0F, 13.0F);
            case SHARP -> thresholdSeverity(accumulatedDamage, 0.5F, 5.0F, 15.0F);
            case BURN -> thresholdSeverity(accumulatedDamage, 0.0F, 5.0F, 16.0F);
            case EXPLOSION -> thresholdSeverity(accumulatedDamage, 4.0F, 8.0F, 16.0F);
        };
    }

    private static int thresholdSeverity(float damage, float levelOne, float levelTwo, float levelThree) {
        if (damage <= 0.0F || damage < levelOne) {
            return 0;
        }
        if (damage < levelTwo) {
            return 1;
        }
        if (damage < levelThree) {
            return 2;
        }
        return 3;
    }

    private static EnumSet<WoundTag> tagsFor(WoundType type, int severity) {
        return switch (type) {
            case BLUNT -> switch (severity) {
                case 2 -> EnumSet.of(WoundTag.SLOWNESS_1, WoundTag.PAIN_1);
                case 3 -> EnumSet.of(WoundTag.SLOWNESS_1, WoundTag.PAIN_1, WoundTag.MOVEMENT_BLEEDING_1);
                default -> EnumSet.noneOf(WoundTag.class);
            };
            case SHARP -> switch (severity) {
                case 1 -> EnumSet.of(WoundTag.PAIN_1);
                case 2 -> EnumSet.of(WoundTag.BLEEDING_2, WoundTag.PAIN_1);
                case 3 -> EnumSet.of(WoundTag.BLEEDING_3, WoundTag.DISORIENTATION_1, WoundTag.PAIN_3);
                default -> EnumSet.noneOf(WoundTag.class);
            };
            case BURN -> switch (severity) {
                case 1 -> EnumSet.of(WoundTag.PAIN_2);
                case 2 -> EnumSet.of(WoundTag.PAIN_3, WoundTag.NEEDS_DEBRIDEMENT_1);
                case 3 -> EnumSet.of(WoundTag.NECROSIS_3, WoundTag.NEEDS_DEBRIDEMENT_1);
                default -> EnumSet.noneOf(WoundTag.class);
            };
            case EXPLOSION -> switch (severity) {
                case 1 -> EnumSet.of(WoundTag.NEEDS_DEBRIDEMENT_1, WoundTag.PAIN_1);
                case 2 -> EnumSet.of(WoundTag.NEEDS_DEBRIDEMENT_1, WoundTag.PAIN_1, WoundTag.BLEEDING_1);
                case 3 -> EnumSet.of(
                        WoundTag.NECROSIS_3,
                        WoundTag.NEEDS_DEBRIDEMENT_1,
                        WoundTag.BLEEDING_2,
                        WoundTag.PAIN_2
                );
                default -> EnumSet.noneOf(WoundTag.class);
            };
        };
    }

    private static long transientPainEndFor(WoundType type, int severity, long createdGameTime) {
        return type == WoundType.SHARP && severity == 1
                ? createdGameTime + SHARP_LEVEL_ONE_PAIN_TICKS
                : -1L;
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
        tag.putLong(TAG_TRANSIENT_PAIN_END_GAME_TIME, transientPainEndGameTime);

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
        boolean hasTransientPainMetadata = tag.contains(TAG_TRANSIENT_PAIN_END_GAME_TIME, Tag.TAG_ANY_NUMERIC);
        if (woundTags.isEmpty() && !hasTransientPainMetadata) {
            woundTags.addAll(tagsFor(type, severity));
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
                woundTags,
                hasTransientPainMetadata
                        ? tag.getLong(TAG_TRANSIENT_PAIN_END_GAME_TIME)
                        : transientPainEndFor(type, severity, tag.getLong(TAG_CREATED_GAME_TIME))
        );
    }
}
