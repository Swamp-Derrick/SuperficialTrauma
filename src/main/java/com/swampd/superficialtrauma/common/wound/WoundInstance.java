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
    private static final String TAG_NEXT_BLEEDING_GAME_TIME = "NextBleedingGameTime";
    private static final String TAG_BLEEDING_TIMER_LEVEL = "BleedingTimerLevel";
    private static final String TAG_FRAGMENTATION_ELIGIBLE = "FragmentationEligible";
    private static final String TAG_CLOSE_RANGE_SHOT = "CloseRangeShot";
    private static final String TAG_TEMPORARY_DRESSING = "TemporaryDressing";
    private static final String TAG_COVERING = "Covering";
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
    private long nextBleedingGameTime;
    private int bleedingTimerLevel;
    private boolean fragmentationEligible;
    private boolean closeRangeShot;
    private WoundCovering covering;

    private WoundInstance(
            UUID id,
            WoundType type,
            int severity,
            float accumulatedDamage,
            float healingProgress,
            long createdGameTime,
            long windowEndGameTime,
            EnumSet<WoundTag> woundTags,
            long transientPainEndGameTime,
            long nextBleedingGameTime,
            int bleedingTimerLevel,
            boolean fragmentationEligible,
            boolean closeRangeShot,
            WoundCovering covering
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
        this.nextBleedingGameTime = nextBleedingGameTime;
        this.bleedingTimerLevel = Math.max(0, Math.min(4, bleedingTimerLevel));
        this.fragmentationEligible = fragmentationEligible;
        this.closeRangeShot = closeRangeShot;
        this.covering = covering == null ? WoundCovering.NONE : covering;
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
        EnumSet<WoundTag> initialTags = tagsFor(type, severity);
        int initialBleedingLevel = bleedingLevel(initialTags, false);
        return new WoundInstance(
                UUID.randomUUID(),
                type,
                severity,
                accumulatedDamage,
                100.0F,
                createdGameTime,
                windowEndGameTime,
                initialTags,
                transientPainEndFor(type, severity, createdGameTime),
                initialBleedingLevel > 0
                        ? createdGameTime + bleedingIntervalTicksFor(initialBleedingLevel)
                        : -1L,
                initialBleedingLevel,
                false,
                false,
                WoundCovering.NONE
        );
    }

    public static WoundInstance createGunshot(
            WoundType type,
            float accumulatedDamage,
            boolean fragmentationEligible,
            boolean closeRangeShot,
            boolean needsDebridement,
            long createdGameTime,
            long windowEndGameTime
    ) {
        if (!type.isGunshot()) {
            throw new IllegalArgumentException("Not a gunshot wound type: " + type);
        }
        int severity = severityFor(type, accumulatedDamage, fragmentationEligible, closeRangeShot);
        if (severity == 0) {
            throw new IllegalArgumentException("Gunshot wounds require at least four final-damage points");
        }
        EnumSet<WoundTag> initialTags = tagsFor(type, severity);
        if (needsDebridement) {
            initialTags.add(WoundTag.NEEDS_DEBRIDEMENT_1);
        }
        int initialBleedingLevel = bleedingLevel(initialTags, false);
        return new WoundInstance(
                UUID.randomUUID(),
                type,
                severity,
                accumulatedDamage,
                100.0F,
                createdGameTime,
                windowEndGameTime,
                initialTags,
                -1L,
                createdGameTime + bleedingIntervalTicksFor(initialBleedingLevel),
                initialBleedingLevel,
                fragmentationEligible,
                closeRangeShot,
                WoundCovering.NONE
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

    public long nextBleedingGameTime() {
        return nextBleedingGameTime;
    }

    public int bleedingLevel(boolean movementBleedingActive) {
        int untreatedLevel = bleedingLevel(woundTags, movementBleedingActive);
        return Math.max(0, untreatedLevel - covering.bleedingReduction());
    }

    public boolean fragmentationEligible() {
        return fragmentationEligible;
    }

    public boolean closeRangeShot() {
        return closeRangeShot;
    }

    public boolean temporaryDressingApplied() {
        return covering == WoundCovering.TEMPORARY_DRESSING;
    }

    public boolean applyTemporaryDressing(long gameTime) {
        return applyCovering(WoundCovering.TEMPORARY_DRESSING, gameTime);
    }

    public boolean removeTemporaryDressing(long gameTime) {
        return removeCovering(WoundCovering.TEMPORARY_DRESSING, gameTime);
    }

    public WoundCovering covering() {
        return covering;
    }

    public boolean applyCovering(WoundCovering newCovering, long gameTime) {
        if (newCovering == null || !newCovering.isApplied() || covering.isApplied() || isHealed()) {
            return false;
        }
        covering = newCovering;
        int effectiveBleedingLevel = bleedingLevel(false);
        bleedingTimerLevel = effectiveBleedingLevel;
        nextBleedingGameTime = effectiveBleedingLevel > 0
                ? gameTime + bleedingIntervalTicksFor(effectiveBleedingLevel)
                : -1L;
        return true;
    }

    public boolean removeCovering(WoundCovering expectedCovering, long gameTime) {
        if (!covering.isApplied() || covering != expectedCovering || isHealed()) {
            return false;
        }
        covering = WoundCovering.NONE;
        int effectiveBleedingLevel = bleedingLevel(false);
        bleedingTimerLevel = effectiveBleedingLevel;
        nextBleedingGameTime = effectiveBleedingLevel > 0
                ? gameTime + bleedingIntervalTicksFor(effectiveBleedingLevel)
                : -1L;
        return true;
    }

    public boolean isAccumulationWindowOpen(long gameTime) {
        return gameTime < windowEndGameTime;
    }

    public void addAccumulatedDamage(float amount, long gameTime) {
        updateAccumulatedDamage(amount, false, false, gameTime);
    }

    public void addGunshotAccumulatedDamage(
            float amount,
            boolean hitFragmentationEligible,
            boolean hitAtCloseRange,
            long gameTime
    ) {
        if (!type.isGunshot()) {
            throw new IllegalStateException("Cannot add gunshot context to " + type);
        }
        updateAccumulatedDamage(amount, hitFragmentationEligible, hitAtCloseRange, gameTime);
    }

    private void updateAccumulatedDamage(
            float amount,
            boolean hitFragmentationEligible,
            boolean hitAtCloseRange,
            long gameTime
    ) {
        int previousBleedingLevel = bleedingLevel(false);
        if (amount > 0.0F) {
            covering = WoundCovering.NONE;
        }
        accumulatedDamage = Math.max(0.0F, accumulatedDamage + amount);
        fragmentationEligible |= hitFragmentationEligible;
        closeRangeShot |= hitAtCloseRange;
        int newSeverity = severityFor(type, accumulatedDamage, fragmentationEligible, closeRangeShot);
        if (newSeverity > severity) {
            boolean needsDebridement = woundTags.contains(WoundTag.NEEDS_DEBRIDEMENT_1);
            severity = newSeverity;
            woundTags.clear();
            woundTags.addAll(tagsFor(type, severity));
            if (needsDebridement) {
                woundTags.add(WoundTag.NEEDS_DEBRIDEMENT_1);
            }
            transientPainEndGameTime = transientPainEndFor(type, severity, createdGameTime);

        }

        int newBleedingLevel = bleedingLevel(false);
        if (newBleedingLevel != previousBleedingLevel) {
            bleedingTimerLevel = newBleedingLevel;
            nextBleedingGameTime = newBleedingLevel > 0
                    ? gameTime + bleedingIntervalTicksFor(newBleedingLevel)
                    : -1L;
        }
    }

    public float advanceBleeding(long gameTime, boolean movementBleedingActive) {
        int effectiveLevel = bleedingLevel(movementBleedingActive);
        if (effectiveLevel <= 0) {
            nextBleedingGameTime = -1L;
            bleedingTimerLevel = 0;
            return 0.0F;
        }

        long intervalTicks = bleedingIntervalTicksFor(effectiveLevel);
        if (nextBleedingGameTime < 0L || bleedingTimerLevel != effectiveLevel) {
            bleedingTimerLevel = effectiveLevel;
            nextBleedingGameTime = gameTime + intervalTicks;
            return 0.0F;
        }
        if (gameTime < nextBleedingGameTime) {
            return 0.0F;
        }

        long completedPulses = 1L + (gameTime - nextBleedingGameTime) / intervalTicks;
        nextBleedingGameTime += completedPulses * intervalTicks;
        return completedPulses * bleedingDamagePerPulseFor(effectiveLevel);
    }

    public boolean expireTransientTags(long gameTime) {
        if (transientPainEndGameTime < 0L || gameTime < transientPainEndGameTime) {
            return false;
        }

        transientPainEndGameTime = -1L;
        return woundTags.remove(WoundTag.PAIN_1);
    }

    public void shiftProgressionDeadlines(long deltaTicks) {
        if (transientPainEndGameTime >= 0L && deltaTicks > 0L) {
            transientPainEndGameTime += deltaTicks;
        }
        if (nextBleedingGameTime >= 0L && deltaTicks > 0L) {
            nextBleedingGameTime += deltaTicks;
        }
    }

    public float baseHealingPerSecond() {
        float naturalRate = switch (type) {
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
            case GUNSHOT_LOW_VELOCITY -> severity == 1 ? 0.4F : 0.0F;
            case GUNSHOT_HIGH_VELOCITY -> switch (severity) {
                case 1 -> 0.3F;
                case 2 -> 0.2F;
                default -> 0.0F;
            };
            case GUNSHOT_SHOTGUN -> severity == 1 ? 0.3F : 0.0F;
        };
        return covering.isApplied() ? Math.max(covering.healingPerSecond(), naturalRate) : naturalRate;
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
        return severityFor(type, accumulatedDamage, false, false);
    }

    public static int gunshotSeverityFor(
            WoundType type,
            float accumulatedDamage,
            boolean fragmentationEligible,
            boolean closeRangeShot
    ) {
        if (!type.isGunshot()) {
            throw new IllegalArgumentException("Not a gunshot wound type: " + type);
        }
        return severityFor(type, accumulatedDamage, fragmentationEligible, closeRangeShot);
    }

    private static int severityFor(
            WoundType type,
            float accumulatedDamage,
            boolean fragmentationEligible,
            boolean closeRangeShot
    ) {
        return switch (type) {
            case BLUNT -> thresholdSeverity(accumulatedDamage, 1.5F, 4.0F, 13.0F);
            case SHARP -> thresholdSeverity(accumulatedDamage, 0.5F, 5.0F, 15.0F);
            case BURN -> thresholdSeverity(accumulatedDamage, 0.0F, 5.0F, 16.0F);
            case EXPLOSION -> thresholdSeverity(accumulatedDamage, 4.0F, 8.0F, 16.0F);
            case GUNSHOT_LOW_VELOCITY -> cappedGunshotSeverity(
                    accumulatedDamage,
                    6.0F,
                    15.0F,
                    fragmentationEligible
            );
            case GUNSHOT_HIGH_VELOCITY -> cappedGunshotSeverity(
                    accumulatedDamage,
                    10.0F,
                    12.0F,
                    fragmentationEligible
            );
            case GUNSHOT_SHOTGUN -> cappedGunshotSeverity(
                    accumulatedDamage,
                    8.0F,
                    8.0F,
                    closeRangeShot
            );
        };
    }

    private static int cappedGunshotSeverity(float damage, float levelTwo, float levelThree, boolean levelThreeEligible) {
        if (damage < 4.0F) {
            return 0;
        }
        if (damage < levelTwo) {
            return 1;
        }
        if (damage < levelThree || !levelThreeEligible) {
            return 2;
        }
        return 3;
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
            case GUNSHOT_LOW_VELOCITY -> switch (severity) {
                case 1 -> EnumSet.of(WoundTag.BLEEDING_1, WoundTag.PAIN_1);
                case 2 -> EnumSet.of(WoundTag.BLEEDING_3, WoundTag.DISORIENTATION_1, WoundTag.PAIN_2);
                case 3 -> EnumSet.of(WoundTag.BLEEDING_3, WoundTag.DISORIENTATION_2, WoundTag.PAIN_3);
                default -> EnumSet.noneOf(WoundTag.class);
            };
            case GUNSHOT_HIGH_VELOCITY -> switch (severity) {
                case 1 -> EnumSet.of(WoundTag.BLEEDING_2, WoundTag.PAIN_1);
                case 2 -> EnumSet.of(WoundTag.BLEEDING_3, WoundTag.PAIN_1);
                case 3 -> EnumSet.of(WoundTag.BLEEDING_4, WoundTag.DISORIENTATION_2, WoundTag.PAIN_2);
                default -> EnumSet.noneOf(WoundTag.class);
            };
            case GUNSHOT_SHOTGUN -> switch (severity) {
                case 1 -> EnumSet.of(WoundTag.BLEEDING_2, WoundTag.PAIN_2);
                case 2 -> EnumSet.of(WoundTag.BLEEDING_2, WoundTag.DISORIENTATION_1, WoundTag.PAIN_2);
                case 3 -> EnumSet.of(WoundTag.BLEEDING_4, WoundTag.DISORIENTATION_3, WoundTag.PAIN_3);
                default -> EnumSet.noneOf(WoundTag.class);
            };
        };
    }

    private static long transientPainEndFor(WoundType type, int severity, long createdGameTime) {
        return type == WoundType.SHARP && severity == 1
                ? createdGameTime + SHARP_LEVEL_ONE_PAIN_TICKS
                : -1L;
    }

    private static int bleedingLevel(Set<WoundTag> tags, boolean movementBleedingActive) {
        int level = 0;
        for (WoundTag tag : tags) {
            if (tag == WoundTag.MOVEMENT_BLEEDING_1 && !movementBleedingActive) {
                continue;
            }
            level = Math.max(level, tag.bleedingLevel());
        }
        return level;
    }

    private static long bleedingIntervalTicksFor(int bleedingLevel) {
        return switch (bleedingLevel) {
            case 1 -> 10L * 20L;
            case 2 -> 7L * 20L;
            case 3 -> 5L * 20L;
            case 4 -> 6L * 20L;
            default -> throw new IllegalArgumentException("Unsupported bleeding level: " + bleedingLevel);
        };
    }

    private static float bleedingDamagePerPulseFor(int bleedingLevel) {
        return bleedingLevel == 4 ? 2.0F : 1.0F;
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
        tag.putLong(TAG_NEXT_BLEEDING_GAME_TIME, nextBleedingGameTime);
        tag.putInt(TAG_BLEEDING_TIMER_LEVEL, bleedingTimerLevel);
        tag.putBoolean(TAG_FRAGMENTATION_ELIGIBLE, fragmentationEligible);
        tag.putBoolean(TAG_CLOSE_RANGE_SHOT, closeRangeShot);
        tag.putString(TAG_COVERING, covering.serializedName());
        tag.putBoolean(TAG_TEMPORARY_DRESSING, covering == WoundCovering.TEMPORARY_DRESSING);

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
        WoundCovering covering = tag.contains(TAG_COVERING, Tag.TAG_STRING)
                ? WoundCovering.fromSerializedName(tag.getString(TAG_COVERING))
                : tag.getBoolean(TAG_TEMPORARY_DRESSING)
                        ? WoundCovering.TEMPORARY_DRESSING
                        : WoundCovering.NONE;
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
                        : transientPainEndFor(type, severity, tag.getLong(TAG_CREATED_GAME_TIME)),
                tag.contains(TAG_NEXT_BLEEDING_GAME_TIME, Tag.TAG_ANY_NUMERIC)
                        ? tag.getLong(TAG_NEXT_BLEEDING_GAME_TIME)
                        : -1L,
                tag.contains(TAG_BLEEDING_TIMER_LEVEL, Tag.TAG_ANY_NUMERIC)
                        ? tag.getInt(TAG_BLEEDING_TIMER_LEVEL)
                        : 0,
                tag.getBoolean(TAG_FRAGMENTATION_ELIGIBLE),
                tag.getBoolean(TAG_CLOSE_RANGE_SHOT),
                covering
        );
    }
}
