package com.swampd.superficialtrauma.common.body;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.damage.DamageClassification;
import com.swampd.superficialtrauma.common.damage.DamageKind;
import com.swampd.superficialtrauma.common.damage.DamageWindow;
import com.swampd.superficialtrauma.common.wound.WoundInstance;
import com.swampd.superficialtrauma.common.wound.WoundCovering;
import com.swampd.superficialtrauma.common.wound.WoundTag;
import com.swampd.superficialtrauma.common.wound.WoundType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class BodyState implements INBTSerializable<CompoundTag> {
    public static final int CURRENT_DATA_VERSION = 13;
    public static final int MAX_WOUNDS = 8;
    public static final long DAMAGE_WINDOW_TICKS = 20L * 20L;
    public static final long WOUND_PROGRESSION_INTERVAL_TICKS = 20L;
    public static final long STRESS_DURATION_TICKS = 20L * 20L;
    public static final long PAIN_RECOVERY_INTERVAL_TICKS = 30L;
    public static final long MOVEMENT_BLEEDING_LINGER_TICKS = 2L * 20L;
    public static final float TRAUMATIC_SHOCK_PAIN_THRESHOLD = 20.0F;
    public static final long SHOCK_WARNING_DURATION_TICKS = 10L * 20L;
    public static final float INITIAL_INCAPACITATED_BLOOD_OXYGEN = 20.0F;
    public static final float MAX_BLOOD_OXYGEN = 30.0F;
    public static final long BLOOD_OXYGEN_POINT_DURATION_TICKS = 9L * 20L;
    public static final long CARDIAC_ARREST_DURATION_TICKS = 180L * 20L;
    public static final long DOWNED_DAMAGE_TICKS_PER_POINT = 10L * 20L;
    public static final long INFECTION_SETTLEMENT_INTERVAL_TICKS = 60L * 20L;

    private static final String TAG_DATA_VERSION = "DataVersion";
    private static final String TAG_REVISION = "Revision";
    private static final String TAG_LIFE_STATE = "LifeState";
    private static final String TAG_COLLAPSE_REASON = "CollapseReason";
    private static final String TAG_BASE_PAIN = "BasePain";
    private static final String LEGACY_TAG_PAIN = "Pain";
    private static final String TAG_INFECTION = "Infection";
    private static final String TAG_NEXT_INFECTION_SETTLEMENT_GAME_TIME = "NextInfectionSettlementGameTime";
    private static final String TAG_SURGERY_SKILL = "SurgerySkill";
    private static final String TAG_BLOOD_DRUG_CONCENTRATION = "BloodDrugConcentration";
    private static final String TAG_ADRENALINE_LEVEL = "AdrenalineLevel";
    private static final String TAG_BLOOD_OXYGEN = "BloodOxygen";
    private static final String TAG_BLOOD_OXYGEN_DEADLINE = "BloodOxygenDeadlineGameTime";
    private static final String TAG_BRAIN_DEATH_DEADLINE = "BrainDeathDeadlineGameTime";
    private static final String TAG_CARDIAC_ARREST_EVENT_ID = "CardiacArrestEventId";
    private static final String TAG_ACCUMULATED_CPR_SECONDS = "AccumulatedCprSeconds";
    private static final String TAG_DOWNED_GAME_TIME = "DownedGameTime";
    private static final String TAG_DOWNED_BODY_YAW = "DownedBodyYaw";
    private static final String TAG_DOWNED_POSTURE = "DownedPosture";
    private static final String TAG_DOWNED_FALL_DIRECTION = "DownedFallDirection";
    private static final String TAG_WOUNDS = "Wounds";
    private static final String TAG_DAMAGE_WINDOWS = "DamageWindows";
    private static final String TAG_LAST_WOUND_PROGRESSION_GAME_TIME = "LastWoundProgressionGameTime";
    private static final String TAG_STRESS_END_GAME_TIME = "StressEndGameTime";
    private static final String TAG_NEXT_PAIN_RECOVERY_GAME_TIME = "NextPainRecoveryGameTime";
    private static final String TAG_PROGRESSION_PAUSED_AT_GAME_TIME = "ProgressionPausedAtGameTime";
    private static final String TAG_MOVEMENT_BLEEDING_END_GAME_TIME = "MovementBleedingEndGameTime";
    private static final String TAG_MOVEMENT_BLEEDING_ACTIVE = "MovementBleedingActive";
    private static final String TAG_SHOCK_WARNING_END_GAME_TIME = "ShockWarningEndGameTime";
    private static final String TAG_LAST_FINAL_DAMAGE = "LastFinalDamage";
    private static final String TAG_LAST_DAMAGE_TYPE = "LastDamageType";
    private static final String TAG_LAST_DAMAGE_KIND = "LastDamageKind";
    private static final String TAG_LAST_DAMAGE_REASON = "LastDamageReason";
    private static final String TAG_LAST_PROJECTILE_ENTITY_ID = "LastProjectileEntityId";
    private static final String TAG_LAST_AMMO_ID = "LastAmmoId";
    private static final String TAG_LAST_WEAPON_ID = "LastWeaponId";
    private static final String TAG_LAST_DAMAGE_GAME_TIME = "LastDamageGameTime";

    private long revision;
    private BodyLifeState lifeState;
    private CollapseReason collapseReason;
    private float basePain;
    private float infection;
    private long nextInfectionSettlementGameTime;
    private boolean surgerySkill;
    private float bloodDrugConcentration;
    private int adrenalineLevel;
    private float bloodOxygen;
    private long bloodOxygenDeadlineGameTime;
    private long brainDeathDeadlineGameTime;
    private UUID cardiacArrestEventId;
    private int accumulatedCprSeconds;
    private long downedGameTime;
    private float downedBodyYaw;
    private DownedPosture downedPosture;
    private DownedFallDirection downedFallDirection;
    private final List<WoundInstance> wounds = new ArrayList<>();
    private final EnumMap<WoundType, DamageWindow> damageWindows = new EnumMap<>(WoundType.class);
    private long lastWoundProgressionGameTime;
    private long stressEndGameTime;
    private long nextPainRecoveryGameTime;
    private long progressionPausedAtGameTime;
    private long movementBleedingEndGameTime;
    private boolean movementBleedingActive;
    private long shockWarningEndGameTime;
    private float lastFinalDamage;
    private String lastDamageType;
    private DamageKind lastDamageKind;
    private String lastDamageReason;
    private String lastProjectileEntityId;
    private String lastAmmoId;
    private String lastWeaponId;
    private long lastDamageGameTime;

    public BodyState() {
        resetToDefaults();
    }

    public int dataVersion() {
        return CURRENT_DATA_VERSION;
    }

    public long revision() {
        return revision;
    }

    public BodyLifeState lifeState() {
        return lifeState;
    }

    public CollapseReason collapseReason() {
        return collapseReason;
    }

    public boolean canAct() {
        return lifeState == BodyLifeState.ACTIVE;
    }

    public boolean incapacitate(CollapseReason reason, long gameTime) {
        if (lifeState != BodyLifeState.ACTIVE) {
            return false;
        }

        enterIncapacitated(reason, gameTime);
        markChanged();
        return true;
    }

    public float pain() {
        return clamp(basePain + woundPainContribution(), 0.0F, 30.0F);
    }

    public float basePain() {
        return basePain;
    }

    public float woundPainContribution() {
        float contribution = 0.0F;
        for (WoundInstance wound : wounds) {
            for (var woundTag : wound.woundTags()) {
                contribution += woundTag.painContribution();
            }
        }
        return contribution;
    }

    public long stressEndGameTime() {
        return stressEndGameTime;
    }

    public long nextPainRecoveryGameTime() {
        return nextPainRecoveryGameTime;
    }

    public long progressionPausedAtGameTime() {
        return progressionPausedAtGameTime;
    }

    public boolean movementBleedingActive() {
        return movementBleedingActive;
    }

    public long shockWarningEndGameTime() {
        return shockWarningEndGameTime;
    }

    public long shockWarningRemainingTicks(long gameTime) {
        return shockWarningEndGameTime < 0L
                ? 0L
                : Math.max(0L, shockWarningEndGameTime - gameTime);
    }

    public boolean isShockWarningActive(long gameTime) {
        return lifeState == BodyLifeState.ACTIVE && shockWarningRemainingTicks(gameTime) > 0L;
    }

    public long stressRemainingTicks(long gameTime) {
        return stressEndGameTime < 0L ? 0L : Math.max(0L, stressEndGameTime - gameTime);
    }

    public boolean isStressActive(long gameTime) {
        return stressRemainingTicks(gameTime) > 0L;
    }

    public float infection() {
        return infection;
    }

    public long nextInfectionSettlementGameTime() {
        return nextInfectionSettlementGameTime;
    }

    public float vanillaHealingMultiplier() {
        return infection > 10.0F ? 0.5F : 1.0F;
    }

    public boolean hasInfectionNausea() {
        return infection > 17.0F;
    }

    public boolean hasSurgerySkill() {
        return surgerySkill;
    }

    public boolean unlockSurgerySkill() {
        if (surgerySkill) {
            return false;
        }
        surgerySkill = true;
        markChanged();
        return true;
    }

    public float bloodDrugConcentration() {
        return bloodDrugConcentration;
    }

    public int adrenalineLevel() {
        return adrenalineLevel;
    }

    public float bloodOxygen() {
        return bloodOxygen;
    }

    public long brainDeathDeadlineGameTime() {
        return brainDeathDeadlineGameTime;
    }

    public long bloodOxygenDeadlineGameTime() {
        return bloodOxygenDeadlineGameTime;
    }

    public long downedDangerRemainingTicks(long gameTime) {
        long deadline = switch (lifeState) {
            case INCAPACITATED, AWAKENING -> bloodOxygenDeadlineGameTime;
            case CARDIAC_ARREST, VENTRICULAR_FIBRILLATION -> brainDeathDeadlineGameTime;
            default -> -1L;
        };
        return deadline < 0L ? 0L : Math.max(0L, deadline - gameTime);
    }

    public DownedDamageResult applyDownedDamage(float finalDamage, long gameTime) {
        BodyLifeState previousState = lifeState;
        if (finalDamage <= 0.0F
                || gameTime < 0L
                || lifeState == BodyLifeState.ACTIVE
                || lifeState == BodyLifeState.BRAIN_DEAD) {
            return DownedDamageResult.ignored(previousState);
        }

        if (lifeState == BodyLifeState.AWAKENING) {
            lifeState = BodyLifeState.INCAPACITATED;
        }

        long shortenedTicks = Math.max(
                1L,
                Math.round((double) finalDamage * DOWNED_DAMAGE_TICKS_PER_POINT)
        );
        if (lifeState == BodyLifeState.INCAPACITATED) {
            ensureBloodOxygenDeadline(gameTime);
            bloodOxygenDeadlineGameTime = Math.max(
                    gameTime,
                    saturatingSubtract(bloodOxygenDeadlineGameTime, shortenedTicks)
            );
        } else if (lifeState == BodyLifeState.CARDIAC_ARREST
                || lifeState == BodyLifeState.VENTRICULAR_FIBRILLATION) {
            ensureBrainDeathDeadline(gameTime);
            brainDeathDeadlineGameTime = Math.max(
                    gameTime,
                    saturatingSubtract(brainDeathDeadlineGameTime, shortenedTicks)
            );
        } else {
            return DownedDamageResult.ignored(previousState);
        }

        advanceDownedProgression(gameTime);
        markChanged();
        return new DownedDamageResult(
                true,
                shortenedTicks,
                downedDangerRemainingTicks(gameTime),
                previousState,
                lifeState
        );
    }

    public Optional<UUID> cardiacArrestEventId() {
        return Optional.ofNullable(cardiacArrestEventId);
    }

    public int accumulatedCprSeconds() {
        return accumulatedCprSeconds;
    }

    public Optional<DownedPoseSnapshot> downedPoseSnapshot() {
        if (downedGameTime < 0L || lifeState == BodyLifeState.ACTIVE) {
            return Optional.empty();
        }
        return Optional.of(new DownedPoseSnapshot(
                downedGameTime,
                downedBodyYaw,
                downedPosture,
                downedFallDirection
        ));
    }

    public boolean captureDownedPose(DownedPoseSnapshot snapshot) {
        if (snapshot == null || lifeState == BodyLifeState.ACTIVE || downedGameTime >= 0L) {
            return false;
        }
        downedGameTime = snapshot.downedGameTime();
        downedBodyYaw = snapshot.bodyYaw();
        downedPosture = snapshot.posture();
        downedFallDirection = snapshot.fallDirection();
        markChanged();
        return true;
    }

    public List<WoundInstance> wounds() {
        return Collections.unmodifiableList(wounds);
    }

    public Optional<WoundInstance> wound(UUID woundId) {
        if (woundId == null) {
            return Optional.empty();
        }
        return wounds.stream().filter(wound -> wound.id().equals(woundId)).findFirst();
    }

    public boolean applyTemporaryDressing(UUID woundId, long gameTime) {
        return applyCovering(woundId, WoundCovering.TEMPORARY_DRESSING, gameTime);
    }

    public boolean applyCovering(UUID woundId, WoundCovering covering, long gameTime) {
        Optional<WoundInstance> wound = wound(woundId);
        if (wound.isEmpty() || !wound.get().applyCovering(covering, gameTime)) {
            return false;
        }
        if (covering == WoundCovering.TEMPORARY_DRESSING) {
            addInfection(wound.get().applyTemporaryDressingContamination(gameTime));
        }
        markChanged();
        return true;
    }

    public boolean removeTemporaryDressing(UUID woundId, long gameTime) {
        return removeCovering(woundId, WoundCovering.TEMPORARY_DRESSING, gameTime);
    }

    public boolean removeCovering(UUID woundId, WoundCovering expectedCovering, long gameTime) {
        Optional<WoundInstance> wound = wound(woundId);
        if (wound.isEmpty() || !wound.get().removeCovering(expectedCovering, gameTime)) {
            return false;
        }
        markChanged();
        return true;
    }

    public boolean applyWoundPacking(UUID woundId, long gameTime) {
        Optional<WoundInstance> wound = wound(woundId);
        if (wound.isEmpty() || !wound.get().applyWoundPacking(gameTime)) {
            return false;
        }
        markChanged();
        return true;
    }

    public boolean removeWoundPacking(UUID woundId, long gameTime) {
        Optional<WoundInstance> wound = wound(woundId);
        if (wound.isEmpty() || !wound.get().removeWoundPacking(gameTime)) {
            return false;
        }
        markChanged();
        return true;
    }

    public boolean debrideWound(UUID woundId) {
        Optional<WoundInstance> wound = wound(woundId);
        if (wound.isEmpty() || !wound.get().debride()) {
            return false;
        }
        markChanged();
        return true;
    }

    public Map<WoundType, DamageWindow> damageWindows() {
        return Collections.unmodifiableMap(damageWindows);
    }

    public Optional<DamageWindow> damageWindow(WoundType type) {
        return Optional.ofNullable(damageWindows.get(type));
    }

    public long lastWoundProgressionGameTime() {
        return lastWoundProgressionGameTime;
    }

    public float lastFinalDamage() {
        return lastFinalDamage;
    }

    public String lastDamageType() {
        return lastDamageType;
    }

    public DamageKind lastDamageKind() {
        return lastDamageKind;
    }

    public String lastDamageReason() {
        return lastDamageReason;
    }

    public String lastProjectileEntityId() {
        return lastProjectileEntityId;
    }

    public String lastAmmoId() {
        return lastAmmoId;
    }

    public String lastWeaponId() {
        return lastWeaponId;
    }

    public long lastDamageGameTime() {
        return lastDamageGameTime;
    }

    public void recordFinalDamage(
            float finalDamage,
            String damageType,
            DamageClassification classification,
            long gameTime
    ) {
        lastFinalDamage = Math.max(0.0F, finalDamage);
        lastDamageType = damageType == null ? "unknown" : damageType;
        lastDamageKind = classification.kind();
        lastDamageReason = classification.reason();
        lastProjectileEntityId = classification.projectileEntityId();
        lastAmmoId = classification.ammoId();
        lastWeaponId = classification.weaponId();
        lastDamageGameTime = gameTime;
        markChanged();
    }

    public WoundUpdateResult applyBluntDamage(float finalDamage, long gameTime) {
        return applyDamage(WoundType.BLUNT, finalDamage, gameTime);
    }

    public WoundUpdateResult applyDamage(WoundType type, float finalDamage, long gameTime) {
        if (finalDamage <= 0.0F) {
            return new WoundUpdateResult(WoundUpdateResult.Status.PENDING, null, 0.0F);
        }

        addTraumaticPain(finalDamage, gameTime);

        Optional<WoundInstance> activeWound = wounds.stream()
                .filter(wound -> wound.type() == type)
                .filter(wound -> wound.isAccumulationWindowOpen(gameTime))
                .max((first, second) -> Long.compare(first.createdGameTime(), second.createdGameTime()));

        if (activeWound.isPresent()) {
            WoundInstance wound = activeWound.get();
            wound.addAccumulatedDamage(finalDamage, gameTime);
            markChanged();
            return new WoundUpdateResult(WoundUpdateResult.Status.UPDATED, wound, wound.accumulatedDamage());
        }

        DamageWindow pendingWindow = damageWindows.get(type);
        if (pendingWindow == null || !pendingWindow.isOpen(gameTime)) {
            pendingWindow = new DamageWindow(
                    type,
                    0.0F,
                    gameTime,
                    gameTime + DAMAGE_WINDOW_TICKS
            );
            damageWindows.put(type, pendingWindow);
        }
        pendingWindow.addDamage(finalDamage);

        int severity = WoundInstance.severityFor(type, pendingWindow.accumulatedDamage());
        if (severity == 0) {
            markChanged();
            return new WoundUpdateResult(
                    WoundUpdateResult.Status.PENDING,
                    null,
                    pendingWindow.accumulatedDamage()
            );
        }

        if (wounds.size() >= MAX_WOUNDS) {
            damageWindows.remove(type);
            markChanged();
            return new WoundUpdateResult(
                    WoundUpdateResult.Status.LIMIT_REACHED,
                    null,
                    pendingWindow.accumulatedDamage()
            );
        }

        WoundInstance wound = WoundInstance.create(
                type,
                pendingWindow.accumulatedDamage(),
                gameTime,
                pendingWindow.endGameTime()
        );
        wounds.add(wound);
        damageWindows.remove(type);
        markChanged();
        return new WoundUpdateResult(WoundUpdateResult.Status.CREATED, wound, wound.accumulatedDamage());
    }

    public WoundUpdateResult applyGunshotDamage(
            WoundType type,
            float finalDamage,
            int armorValue,
            double attackerDistance,
            boolean needsDebridement,
            long gameTime
    ) {
        if (!type.isGunshot()) {
            throw new IllegalArgumentException("Not a gunshot wound type: " + type);
        }
        if (finalDamage <= 0.0F) {
            return new WoundUpdateResult(WoundUpdateResult.Status.PENDING, null, 0.0F);
        }
        if (finalDamage < 4.0F) {
            return applyDamage(WoundType.BLUNT, finalDamage, gameTime);
        }

        addTraumaticPain(finalDamage, gameTime);
        boolean fragmentationEligible = type != WoundType.GUNSHOT_SHOTGUN && armorValue > 10;
        boolean closeRangeShot = type == WoundType.GUNSHOT_SHOTGUN && attackerDistance <= 3.0D;

        Optional<WoundInstance> activeWound = wounds.stream()
                .filter(wound -> wound.type() == type)
                .filter(wound -> wound.isAccumulationWindowOpen(gameTime))
                .max((first, second) -> Long.compare(first.createdGameTime(), second.createdGameTime()));

        if (activeWound.isPresent()) {
            WoundInstance wound = activeWound.get();
            wound.addGunshotAccumulatedDamage(
                    finalDamage,
                    fragmentationEligible,
                    closeRangeShot,
                    gameTime
            );
            markChanged();
            return new WoundUpdateResult(WoundUpdateResult.Status.UPDATED, wound, wound.accumulatedDamage());
        }

        if (wounds.size() >= MAX_WOUNDS) {
            markChanged();
            return new WoundUpdateResult(WoundUpdateResult.Status.LIMIT_REACHED, null, finalDamage);
        }

        WoundInstance wound = WoundInstance.createGunshot(
                type,
                finalDamage,
                fragmentationEligible,
                closeRangeShot,
                needsDebridement,
                gameTime,
                gameTime + DAMAGE_WINDOW_TICKS
        );
        wounds.add(wound);
        markChanged();
        return new WoundUpdateResult(WoundUpdateResult.Status.CREATED, wound, wound.accumulatedDamage());
    }

    public void resumeBodyProgression(long gameTime) {
        if (progressionPausedAtGameTime >= 0L && gameTime >= progressionPausedAtGameTime) {
            long pausedTicks = gameTime - progressionPausedAtGameTime;
            stressEndGameTime = shiftDeadline(stressEndGameTime, pausedTicks);
            nextPainRecoveryGameTime = shiftDeadline(nextPainRecoveryGameTime, pausedTicks);
            movementBleedingEndGameTime = shiftDeadline(movementBleedingEndGameTime, pausedTicks);
            shockWarningEndGameTime = shiftDeadline(shockWarningEndGameTime, pausedTicks);
            nextInfectionSettlementGameTime = shiftDeadline(nextInfectionSettlementGameTime, pausedTicks);
            for (WoundInstance wound : wounds) {
                wound.shiftProgressionDeadlines(pausedTicks);
            }
        }
        progressionPausedAtGameTime = -1L;
        lastWoundProgressionGameTime = Math.max(0L, gameTime);
    }

    public void pauseBodyProgression(long gameTime) {
        progressionPausedAtGameTime = Math.max(0L, gameTime);
        lastWoundProgressionGameTime = -1L;
    }

    public BodyProgressionResult advanceBodyProgression(long gameTime) {
        return advanceBodyProgression(gameTime, false, 20);
    }

    public BodyProgressionResult advanceBodyProgression(long gameTime, boolean traumaticMovement) {
        return advanceBodyProgression(gameTime, traumaticMovement, 20);
    }

    public BodyProgressionResult advanceBodyProgression(
            long gameTime,
            boolean traumaticMovement,
            int foodLevel
    ) {
        if (gameTime < 0L) {
            return BodyProgressionResult.unchanged();
        }

        int expiredDamageWindows = 0;
        Iterator<DamageWindow> windowIterator = damageWindows.values().iterator();
        while (windowIterator.hasNext()) {
            if (!windowIterator.next().isOpen(gameTime)) {
                windowIterator.remove();
                expiredDamageWindows++;
            }
        }

        if (lastWoundProgressionGameTime < 0L || gameTime < lastWoundProgressionGameTime) {
            resumeBodyProgression(gameTime);
        }

        boolean movementBleedingStateChanged = updateMovementBleedingState(gameTime, traumaticMovement);

        int expiredTransientWoundTags = 0;
        for (WoundInstance wound : wounds) {
            if (wound.expireTransientTags(gameTime)) {
                expiredTransientWoundTags++;
            }
        }

        int progressedWounds = 0;
        int healedWounds = 0;
        if (wounds.isEmpty()) {
            lastWoundProgressionGameTime = gameTime;
        } else {
            long elapsedTicks = gameTime - lastWoundProgressionGameTime;
            long elapsedWholeSeconds = elapsedTicks / WOUND_PROGRESSION_INTERVAL_TICKS;
            if (elapsedWholeSeconds > 0L) {
                lastWoundProgressionGameTime += elapsedWholeSeconds * WOUND_PROGRESSION_INTERVAL_TICKS;
                Iterator<WoundInstance> iterator = wounds.iterator();
                while (iterator.hasNext()) {
                    WoundInstance wound = iterator.next();
                    if (wound.advanceNaturalHealing((float) elapsedWholeSeconds)) {
                        progressedWounds++;
                    }
                    if (wound.isHealed()) {
                        iterator.remove();
                        healedWounds++;
                    }
                }
            }
        }

        if (healedWounds > 0) {
            movementBleedingStateChanged |= updateMovementBleedingState(gameTime, traumaticMovement);
        }

        float bleedingDamage = 0.0F;
        boolean bleedingTimerChanged = false;
        for (WoundInstance wound : wounds) {
            long previousDeadline = wound.nextBleedingGameTime();
            bleedingDamage += wound.advanceBleeding(gameTime, movementBleedingActive);
            bleedingTimerChanged |= previousDeadline != wound.nextBleedingGameTime();
        }

        float woundInfectionIncrease = 0.0F;
        for (WoundInstance wound : wounds) {
            woundInfectionIncrease += wound.advanceInfection(gameTime);
        }
        float infectionChange = changeInfection(woundInfectionIncrease);
        long previousInfectionSettlement = nextInfectionSettlementGameTime;
        infectionChange += advanceSystemicInfection(gameTime, foodLevel);
        boolean infectionTimerChanged = previousInfectionSettlement != nextInfectionSettlementGameTime;

        boolean becameSeptic = false;
        if (infection >= 20.0F && lifeState == BodyLifeState.ACTIVE) {
            enterIncapacitated(CollapseReason.SEPSIS, gameTime);
            becameSeptic = true;
        }

        ShockProgression shockProgression = advanceTraumaticShock(gameTime);
        DownedProgression downedProgression = advanceDownedProgression(gameTime);
        float recoveredBasePain = isShockWarningActive(gameTime)
                ? 0.0F
                : recoverBasePain(gameTime);
        return finishBodyProgression(
                progressedWounds,
                healedWounds,
                expiredDamageWindows,
                expiredTransientWoundTags,
                recoveredBasePain,
                bleedingDamage,
                infectionChange,
                becameSeptic,
                infectionTimerChanged,
                bleedingTimerChanged,
                movementBleedingStateChanged,
                shockProgression,
                downedProgression
        );
    }

    private BodyProgressionResult finishBodyProgression(
            int progressedWounds,
            int healedWounds,
            int expiredDamageWindows,
            int expiredTransientWoundTags,
            float recoveredBasePain,
            float bleedingDamage,
            float infectionChange,
            boolean becameSeptic,
            boolean infectionTimerChanged,
            boolean bleedingTimerChanged,
            boolean movementBleedingStateChanged,
            ShockProgression shockProgression,
            DownedProgression downedProgression
    ) {
        if (progressedWounds == 0
                && healedWounds == 0
                && expiredDamageWindows == 0
                && expiredTransientWoundTags == 0
                && recoveredBasePain <= 0.0F
                && bleedingDamage <= 0.0F
                && Math.abs(infectionChange) <= 0.0001F
                && !becameSeptic
                && !infectionTimerChanged
                && !bleedingTimerChanged
                && !movementBleedingStateChanged
                && !shockProgression.changed()
                && !downedProgression.changed()) {
            return BodyProgressionResult.unchanged();
        }

        markChanged();
        return new BodyProgressionResult(
                true,
                progressedWounds,
                healedWounds,
                expiredDamageWindows,
                expiredTransientWoundTags,
                recoveredBasePain,
                bleedingDamage,
                infectionChange,
                shockProgression.warningStarted(),
                shockProgression.warningCancelled(),
                becameSeptic || shockProgression.becameIncapacitated(),
                downedProgression.bloodOxygenChanged(),
                downedProgression.becameCardiacArrest(),
                downedProgression.becameBrainDead()
        );
    }

    private ShockProgression advanceTraumaticShock(long gameTime) {
        if (lifeState != BodyLifeState.ACTIVE) {
            if (shockWarningEndGameTime >= 0L) {
                shockWarningEndGameTime = -1L;
                return ShockProgression.cancelled();
            }
            return ShockProgression.unchanged();
        }

        if (isStressActive(gameTime)) {
            if (shockWarningEndGameTime >= 0L) {
                shockWarningEndGameTime = -1L;
                return ShockProgression.cancelled();
            }
            return ShockProgression.unchanged();
        }

        if (shockWarningEndGameTime >= 0L) {
            if (pain() < TRAUMATIC_SHOCK_PAIN_THRESHOLD) {
                shockWarningEndGameTime = -1L;
                nextPainRecoveryGameTime = basePain > 0.0F
                        ? gameTime + PAIN_RECOVERY_INTERVAL_TICKS
                        : -1L;
                return ShockProgression.cancelled();
            }
            if (gameTime >= shockWarningEndGameTime) {
                enterIncapacitated(CollapseReason.TRAUMATIC_SHOCK, gameTime);
                nextPainRecoveryGameTime = basePain > 0.0F
                        ? gameTime + PAIN_RECOVERY_INTERVAL_TICKS
                        : -1L;
                return ShockProgression.incapacitated();
            }
            return ShockProgression.unchanged();
        }

        if (pain() >= TRAUMATIC_SHOCK_PAIN_THRESHOLD) {
            shockWarningEndGameTime = gameTime + SHOCK_WARNING_DURATION_TICKS;
            nextPainRecoveryGameTime = basePain > 0.0F
                    ? shockWarningEndGameTime + PAIN_RECOVERY_INTERVAL_TICKS
                    : -1L;
            return ShockProgression.started();
        }
        return ShockProgression.unchanged();
    }

    private void enterIncapacitated(CollapseReason reason, long gameTime) {
        clearDownedPoseSnapshot();
        lifeState = BodyLifeState.INCAPACITATED;
        collapseReason = reason == null || reason == CollapseReason.NONE
                ? CollapseReason.LETHAL_DAMAGE
                : reason;
        bloodOxygen = INITIAL_INCAPACITATED_BLOOD_OXYGEN;
        bloodOxygenDeadlineGameTime = gameTime
                + Math.round(INITIAL_INCAPACITATED_BLOOD_OXYGEN * BLOOD_OXYGEN_POINT_DURATION_TICKS);
        brainDeathDeadlineGameTime = -1L;
        cardiacArrestEventId = null;
        accumulatedCprSeconds = 0;
        shockWarningEndGameTime = -1L;
    }

    private DownedProgression advanceDownedProgression(long gameTime) {
        boolean bloodOxygenChanged = false;
        boolean becameCardiacArrest = false;
        boolean becameBrainDead = false;

        if (lifeState == BodyLifeState.INCAPACITATED || lifeState == BodyLifeState.AWAKENING) {
            ensureBloodOxygenDeadline(gameTime);
            float previousBloodOxygen = bloodOxygen;
            long remainingTicks = Math.max(0L, bloodOxygenDeadlineGameTime - gameTime);
            bloodOxygen = clamp(
                    (float) Math.ceil(remainingTicks / (double) BLOOD_OXYGEN_POINT_DURATION_TICKS),
                    0.0F,
                    MAX_BLOOD_OXYGEN
            );
            bloodOxygenChanged = Float.compare(previousBloodOxygen, bloodOxygen) != 0;

            if (remainingTicks == 0L) {
                long cardiacArrestStart = bloodOxygenDeadlineGameTime;
                lifeState = BodyLifeState.CARDIAC_ARREST;
                bloodOxygen = 0.0F;
                bloodOxygenDeadlineGameTime = -1L;
                brainDeathDeadlineGameTime = cardiacArrestStart + CARDIAC_ARREST_DURATION_TICKS;
                cardiacArrestEventId = UUID.randomUUID();
                accumulatedCprSeconds = 0;
                becameCardiacArrest = true;
            }
        }

        if (lifeState == BodyLifeState.CARDIAC_ARREST
                || lifeState == BodyLifeState.VENTRICULAR_FIBRILLATION) {
            ensureBrainDeathDeadline(gameTime);
            if (gameTime >= brainDeathDeadlineGameTime) {
                lifeState = BodyLifeState.BRAIN_DEAD;
                accumulatedCprSeconds = 0;
                becameBrainDead = true;
            }
        }

        return new DownedProgression(bloodOxygenChanged, becameCardiacArrest, becameBrainDead);
    }

    private void ensureBloodOxygenDeadline(long gameTime) {
        if (bloodOxygenDeadlineGameTime >= 0L) {
            return;
        }
        bloodOxygenDeadlineGameTime = gameTime
                + Math.round((double) clamp(bloodOxygen, 0.0F, MAX_BLOOD_OXYGEN)
                * BLOOD_OXYGEN_POINT_DURATION_TICKS);
    }

    private void ensureBrainDeathDeadline(long gameTime) {
        if (brainDeathDeadlineGameTime < 0L) {
            brainDeathDeadlineGameTime = gameTime + CARDIAC_ARREST_DURATION_TICKS;
        }
        if (cardiacArrestEventId == null) {
            cardiacArrestEventId = UUID.randomUUID();
        }
    }

    public boolean forceRecoverForDebug() {
        boolean changed = lifeState != BodyLifeState.ACTIVE
                || collapseReason != CollapseReason.NONE
                || shockWarningEndGameTime >= 0L
                || basePain > 0.0F
                || stressEndGameTime >= 0L
                || nextPainRecoveryGameTime >= 0L
                || bloodOxygen < MAX_BLOOD_OXYGEN
                || bloodOxygenDeadlineGameTime >= 0L
                || brainDeathDeadlineGameTime >= 0L
                || cardiacArrestEventId != null
                || accumulatedCprSeconds > 0
                || downedGameTime >= 0L;
        if (!changed) {
            return false;
        }

        lifeState = BodyLifeState.ACTIVE;
        collapseReason = CollapseReason.NONE;
        shockWarningEndGameTime = -1L;
        basePain = 0.0F;
        bloodOxygen = MAX_BLOOD_OXYGEN;
        bloodOxygenDeadlineGameTime = -1L;
        brainDeathDeadlineGameTime = -1L;
        cardiacArrestEventId = null;
        accumulatedCprSeconds = 0;
        clearDownedPoseSnapshot();
        stressEndGameTime = -1L;
        nextPainRecoveryGameTime = -1L;
        markChanged();
        return true;
    }

    public void resetAllForDebug() {
        long nextRevision = revision == Long.MAX_VALUE ? Long.MAX_VALUE : revision + 1L;
        resetToDefaults();
        revision = nextRevision;
    }

    private boolean updateMovementBleedingState(long gameTime, boolean traumaticMovement) {
        boolean previousState = movementBleedingActive;
        boolean hasMovementBleedingWound = wounds.stream()
                .anyMatch(wound -> wound.woundTags().contains(WoundTag.MOVEMENT_BLEEDING_1));
        if (!hasMovementBleedingWound) {
            movementBleedingEndGameTime = -1L;
            movementBleedingActive = false;
            return previousState;
        }

        if (traumaticMovement) {
            movementBleedingEndGameTime = gameTime + MOVEMENT_BLEEDING_LINGER_TICKS;
        }
        movementBleedingActive = movementBleedingEndGameTime >= 0L
                && gameTime < movementBleedingEndGameTime;
        return previousState != movementBleedingActive;
    }

    private void addTraumaticPain(float finalDamage, long gameTime) {
        basePain = clamp(basePain + finalDamage, 0.0F, 30.0F);
        stressEndGameTime = Math.max(stressEndGameTime, gameTime + STRESS_DURATION_TICKS);
        nextPainRecoveryGameTime = stressEndGameTime + PAIN_RECOVERY_INTERVAL_TICKS;
    }

    private float recoverBasePain(long gameTime) {
        if (basePain <= 0.0F || nextPainRecoveryGameTime < 0L || gameTime < nextPainRecoveryGameTime) {
            return 0.0F;
        }

        long recoverySteps = 1L
                + (gameTime - nextPainRecoveryGameTime) / PAIN_RECOVERY_INTERVAL_TICKS;
        float previousPain = basePain;
        basePain = Math.max(0.0F, basePain - recoverySteps);
        if (basePain <= 0.0F) {
            nextPainRecoveryGameTime = -1L;
        } else {
            nextPainRecoveryGameTime += recoverySteps * PAIN_RECOVERY_INTERVAL_TICKS;
        }
        return previousPain - basePain;
    }

    private static long shiftDeadline(long deadline, long deltaTicks) {
        return deadline < 0L ? -1L : deadline + Math.max(0L, deltaTicks);
    }

    private static long saturatingSubtract(long value, long decrement) {
        if (decrement <= 0L) {
            return value;
        }
        return value < Long.MIN_VALUE + decrement ? Long.MIN_VALUE : value - decrement;
    }

    public void copyFrom(BodyState other) {
        deserializeNBT(other.serializeNBT());
    }

    public void copyPersistentKnowledgeFrom(BodyState other) {
        if (other != null && other.surgerySkill && !surgerySkill) {
            surgerySkill = true;
            markChanged();
        }
    }

    private void markChanged() {
        revision++;
    }

    private void addInfection(float amount) {
        changeInfection(amount);
    }

    private float changeInfection(float amount) {
        if (amount == 0.0F) {
            return 0.0F;
        }
        float previous = infection;
        infection = clamp(infection + amount, 0.0F, 20.0F);
        return infection - previous;
    }

    private float advanceSystemicInfection(long gameTime, int foodLevel) {
        if (infection <= 0.5F || infection >= 20.0F) {
            nextInfectionSettlementGameTime = -1L;
            return 0.0F;
        }
        if (nextInfectionSettlementGameTime < 0L) {
            nextInfectionSettlementGameTime = gameTime + INFECTION_SETTLEMENT_INTERVAL_TICKS;
            return 0.0F;
        }
        if (gameTime < nextInfectionSettlementGameTime) {
            return 0.0F;
        }

        float totalChange = 0.0F;
        while (gameTime >= nextInfectionSettlementGameTime && infection > 0.5F && infection < 20.0F) {
            float step = foodLevel >= 15 ? -1.0F : systemicInfectionGrowth(infection);
            totalChange += changeInfection(step);
            nextInfectionSettlementGameTime += INFECTION_SETTLEMENT_INTERVAL_TICKS;
        }
        if (infection <= 0.5F || infection >= 20.0F) {
            nextInfectionSettlementGameTime = -1L;
        }
        return totalChange;
    }

    private static float systemicInfectionGrowth(float currentInfection) {
        if (currentInfection <= 5.0F) {
            return 0.5F;
        }
        if (currentInfection <= 10.0F) {
            return 1.0F;
        }
        return 1.5F;
    }

    private void resetToDefaults() {
        revision = 0L;
        lifeState = BodyLifeState.ACTIVE;
        collapseReason = CollapseReason.NONE;
        basePain = 0.0F;
        infection = 0.0F;
        nextInfectionSettlementGameTime = -1L;
        surgerySkill = false;
        bloodDrugConcentration = 0.0F;
        adrenalineLevel = 0;
        bloodOxygen = MAX_BLOOD_OXYGEN;
        bloodOxygenDeadlineGameTime = -1L;
        brainDeathDeadlineGameTime = -1L;
        cardiacArrestEventId = null;
        accumulatedCprSeconds = 0;
        clearDownedPoseSnapshot();
        wounds.clear();
        damageWindows.clear();
        lastWoundProgressionGameTime = -1L;
        stressEndGameTime = -1L;
        nextPainRecoveryGameTime = -1L;
        progressionPausedAtGameTime = -1L;
        movementBleedingEndGameTime = -1L;
        movementBleedingActive = false;
        shockWarningEndGameTime = -1L;
        lastFinalDamage = 0.0F;
        lastDamageType = "none";
        lastDamageKind = DamageKind.UNKNOWN;
        lastDamageReason = "none";
        lastProjectileEntityId = "none";
        lastAmmoId = "none";
        lastWeaponId = "none";
        lastDamageGameTime = -1L;
    }

    private void clearDownedPoseSnapshot() {
        downedGameTime = -1L;
        downedBodyYaw = 0.0F;
        downedPosture = DownedPosture.UNSAFE;
        downedFallDirection = DownedFallDirection.FADE_ONLY;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_DATA_VERSION, CURRENT_DATA_VERSION);
        tag.putLong(TAG_REVISION, revision);
        tag.putString(TAG_LIFE_STATE, lifeState.serializedName());
        tag.putString(TAG_COLLAPSE_REASON, collapseReason.serializedName());
        tag.putFloat(TAG_BASE_PAIN, basePain);
        tag.putFloat(TAG_INFECTION, infection);
        tag.putLong(TAG_NEXT_INFECTION_SETTLEMENT_GAME_TIME, nextInfectionSettlementGameTime);
        tag.putBoolean(TAG_SURGERY_SKILL, surgerySkill);
        tag.putFloat(TAG_BLOOD_DRUG_CONCENTRATION, bloodDrugConcentration);
        tag.putInt(TAG_ADRENALINE_LEVEL, adrenalineLevel);
        tag.putFloat(TAG_BLOOD_OXYGEN, bloodOxygen);
        tag.putLong(TAG_BLOOD_OXYGEN_DEADLINE, bloodOxygenDeadlineGameTime);
        tag.putLong(TAG_BRAIN_DEATH_DEADLINE, brainDeathDeadlineGameTime);
        if (cardiacArrestEventId != null) {
            tag.putUUID(TAG_CARDIAC_ARREST_EVENT_ID, cardiacArrestEventId);
        }
        tag.putInt(TAG_ACCUMULATED_CPR_SECONDS, accumulatedCprSeconds);
        if (downedGameTime >= 0L && lifeState != BodyLifeState.ACTIVE) {
            tag.putLong(TAG_DOWNED_GAME_TIME, downedGameTime);
            tag.putFloat(TAG_DOWNED_BODY_YAW, downedBodyYaw);
            tag.putString(TAG_DOWNED_POSTURE, downedPosture.serializedName());
            tag.putString(TAG_DOWNED_FALL_DIRECTION, downedFallDirection.serializedName());
        }

        ListTag woundList = new ListTag();
        for (WoundInstance wound : wounds) {
            woundList.add(wound.serializeNBT());
        }
        tag.put(TAG_WOUNDS, woundList);

        ListTag damageWindowList = new ListTag();
        for (DamageWindow damageWindow : damageWindows.values()) {
            damageWindowList.add(damageWindow.serializeNBT());
        }
        tag.put(TAG_DAMAGE_WINDOWS, damageWindowList);
        tag.putLong(TAG_LAST_WOUND_PROGRESSION_GAME_TIME, lastWoundProgressionGameTime);
        tag.putLong(TAG_STRESS_END_GAME_TIME, stressEndGameTime);
        tag.putLong(TAG_NEXT_PAIN_RECOVERY_GAME_TIME, nextPainRecoveryGameTime);
        tag.putLong(TAG_PROGRESSION_PAUSED_AT_GAME_TIME, progressionPausedAtGameTime);
        tag.putLong(TAG_MOVEMENT_BLEEDING_END_GAME_TIME, movementBleedingEndGameTime);
        tag.putBoolean(TAG_MOVEMENT_BLEEDING_ACTIVE, movementBleedingActive);
        tag.putLong(TAG_SHOCK_WARNING_END_GAME_TIME, shockWarningEndGameTime);

        tag.putFloat(TAG_LAST_FINAL_DAMAGE, lastFinalDamage);
        tag.putString(TAG_LAST_DAMAGE_TYPE, lastDamageType);
        tag.putString(TAG_LAST_DAMAGE_KIND, lastDamageKind.serializedName());
        tag.putString(TAG_LAST_DAMAGE_REASON, lastDamageReason);
        tag.putString(TAG_LAST_PROJECTILE_ENTITY_ID, lastProjectileEntityId);
        tag.putString(TAG_LAST_AMMO_ID, lastAmmoId);
        tag.putString(TAG_LAST_WEAPON_ID, lastWeaponId);
        tag.putLong(TAG_LAST_DAMAGE_GAME_TIME, lastDamageGameTime);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        resetToDefaults();
        int storedVersion = tag.contains(TAG_DATA_VERSION, Tag.TAG_INT) ? tag.getInt(TAG_DATA_VERSION) : 0;
        if (storedVersion > CURRENT_DATA_VERSION) {
            SuperficialTrauma.LOGGER.warn(
                    "Loading BodyState data version {} with older supported version {}; using known fields only",
                    storedVersion,
                    CURRENT_DATA_VERSION
            );
        }

        revision = Math.max(0L, tag.getLong(TAG_REVISION));
        lifeState = BodyLifeState.fromSerializedName(tag.getString(TAG_LIFE_STATE));
        collapseReason = tag.contains(TAG_COLLAPSE_REASON, Tag.TAG_STRING)
                ? CollapseReason.fromSerializedName(tag.getString(TAG_COLLAPSE_REASON))
                : CollapseReason.NONE;
        basePain = tag.contains(TAG_BASE_PAIN, Tag.TAG_ANY_NUMERIC)
                ? clamp(tag.getFloat(TAG_BASE_PAIN), 0.0F, 30.0F)
                : clamp(tag.getFloat(LEGACY_TAG_PAIN), 0.0F, 30.0F);
        infection = clamp(tag.getFloat(TAG_INFECTION), 0.0F, 20.0F);
        nextInfectionSettlementGameTime = tag.contains(TAG_NEXT_INFECTION_SETTLEMENT_GAME_TIME, Tag.TAG_ANY_NUMERIC)
                ? tag.getLong(TAG_NEXT_INFECTION_SETTLEMENT_GAME_TIME)
                : -1L;
        surgerySkill = tag.getBoolean(TAG_SURGERY_SKILL);
        bloodDrugConcentration = Math.max(0.0F, tag.getFloat(TAG_BLOOD_DRUG_CONCENTRATION));
        adrenalineLevel = Math.max(0, tag.getInt(TAG_ADRENALINE_LEVEL));
        bloodOxygen = tag.contains(TAG_BLOOD_OXYGEN, Tag.TAG_ANY_NUMERIC)
                ? clamp(tag.getFloat(TAG_BLOOD_OXYGEN), 0.0F, MAX_BLOOD_OXYGEN)
                : MAX_BLOOD_OXYGEN;
        bloodOxygenDeadlineGameTime = tag.contains(TAG_BLOOD_OXYGEN_DEADLINE, Tag.TAG_ANY_NUMERIC)
                ? tag.getLong(TAG_BLOOD_OXYGEN_DEADLINE)
                : -1L;
        brainDeathDeadlineGameTime = tag.contains(TAG_BRAIN_DEATH_DEADLINE, Tag.TAG_ANY_NUMERIC)
                ? tag.getLong(TAG_BRAIN_DEATH_DEADLINE)
                : -1L;
        cardiacArrestEventId = tag.hasUUID(TAG_CARDIAC_ARREST_EVENT_ID)
                ? tag.getUUID(TAG_CARDIAC_ARREST_EVENT_ID)
                : null;
        accumulatedCprSeconds = Math.max(0, tag.getInt(TAG_ACCUMULATED_CPR_SECONDS));
        if (lifeState != BodyLifeState.ACTIVE && tag.contains(TAG_DOWNED_GAME_TIME, Tag.TAG_ANY_NUMERIC)) {
            DownedPoseSnapshot snapshot = new DownedPoseSnapshot(
                    Math.max(0L, tag.getLong(TAG_DOWNED_GAME_TIME)),
                    tag.getFloat(TAG_DOWNED_BODY_YAW),
                    DownedPosture.fromSerializedName(tag.getString(TAG_DOWNED_POSTURE)),
                    DownedFallDirection.fromSerializedName(tag.getString(TAG_DOWNED_FALL_DIRECTION))
            );
            downedGameTime = snapshot.downedGameTime();
            downedBodyYaw = snapshot.bodyYaw();
            downedPosture = snapshot.posture();
            downedFallDirection = snapshot.fallDirection();
        }

        ListTag woundList = tag.getList(TAG_WOUNDS, Tag.TAG_COMPOUND);
        for (int i = 0; i < woundList.size() && wounds.size() < MAX_WOUNDS; i++) {
            wounds.add(WoundInstance.deserializeNBT(woundList.getCompound(i)));
        }

        ListTag damageWindowList = tag.getList(TAG_DAMAGE_WINDOWS, Tag.TAG_COMPOUND);
        for (int i = 0; i < damageWindowList.size(); i++) {
            DamageWindow damageWindow = DamageWindow.deserializeNBT(damageWindowList.getCompound(i));
            damageWindows.put(damageWindow.type(), damageWindow);
        }
        lastWoundProgressionGameTime = tag.contains(TAG_LAST_WOUND_PROGRESSION_GAME_TIME, Tag.TAG_ANY_NUMERIC)
                ? tag.getLong(TAG_LAST_WOUND_PROGRESSION_GAME_TIME)
                : -1L;
        stressEndGameTime = tag.contains(TAG_STRESS_END_GAME_TIME, Tag.TAG_ANY_NUMERIC)
                ? tag.getLong(TAG_STRESS_END_GAME_TIME)
                : -1L;
        nextPainRecoveryGameTime = tag.contains(TAG_NEXT_PAIN_RECOVERY_GAME_TIME, Tag.TAG_ANY_NUMERIC)
                ? tag.getLong(TAG_NEXT_PAIN_RECOVERY_GAME_TIME)
                : -1L;
        progressionPausedAtGameTime = tag.contains(TAG_PROGRESSION_PAUSED_AT_GAME_TIME, Tag.TAG_ANY_NUMERIC)
                ? tag.getLong(TAG_PROGRESSION_PAUSED_AT_GAME_TIME)
                : -1L;
        movementBleedingEndGameTime = tag.contains(TAG_MOVEMENT_BLEEDING_END_GAME_TIME, Tag.TAG_ANY_NUMERIC)
                ? tag.getLong(TAG_MOVEMENT_BLEEDING_END_GAME_TIME)
                : -1L;
        movementBleedingActive = tag.getBoolean(TAG_MOVEMENT_BLEEDING_ACTIVE);
        shockWarningEndGameTime = tag.contains(TAG_SHOCK_WARNING_END_GAME_TIME, Tag.TAG_ANY_NUMERIC)
                ? tag.getLong(TAG_SHOCK_WARNING_END_GAME_TIME)
                : -1L;

        lastFinalDamage = Math.max(0.0F, tag.getFloat(TAG_LAST_FINAL_DAMAGE));
        lastDamageType = tag.contains(TAG_LAST_DAMAGE_TYPE, Tag.TAG_STRING)
                ? tag.getString(TAG_LAST_DAMAGE_TYPE)
                : "none";
        lastDamageKind = tag.contains(TAG_LAST_DAMAGE_KIND, Tag.TAG_STRING)
                ? DamageKind.fromSerializedName(tag.getString(TAG_LAST_DAMAGE_KIND))
                : DamageKind.UNKNOWN;
        lastDamageReason = getStringOrDefault(tag, TAG_LAST_DAMAGE_REASON, "none");
        lastProjectileEntityId = getStringOrDefault(tag, TAG_LAST_PROJECTILE_ENTITY_ID, "none");
        lastAmmoId = getStringOrDefault(tag, TAG_LAST_AMMO_ID, "none");
        lastWeaponId = getStringOrDefault(tag, TAG_LAST_WEAPON_ID, "none");
        lastDamageGameTime = tag.contains(TAG_LAST_DAMAGE_GAME_TIME, Tag.TAG_ANY_NUMERIC)
                ? tag.getLong(TAG_LAST_DAMAGE_GAME_TIME)
                : -1L;
    }

    private static String getStringOrDefault(CompoundTag tag, String key, String defaultValue) {
        return tag.contains(key, Tag.TAG_STRING) ? tag.getString(key) : defaultValue;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private record ShockProgression(
            boolean warningStarted,
            boolean warningCancelled,
            boolean becameIncapacitated
    ) {
        private static final ShockProgression UNCHANGED = new ShockProgression(false, false, false);

        private static ShockProgression unchanged() {
            return UNCHANGED;
        }

        private static ShockProgression started() {
            return new ShockProgression(true, false, false);
        }

        private static ShockProgression cancelled() {
            return new ShockProgression(false, true, false);
        }

        private static ShockProgression incapacitated() {
            return new ShockProgression(false, false, true);
        }

        private boolean changed() {
            return warningStarted || warningCancelled || becameIncapacitated;
        }
    }

    private record DownedProgression(
            boolean bloodOxygenChanged,
            boolean becameCardiacArrest,
            boolean becameBrainDead
    ) {
        private boolean changed() {
            return bloodOxygenChanged || becameCardiacArrest || becameBrainDead;
        }
    }
}
