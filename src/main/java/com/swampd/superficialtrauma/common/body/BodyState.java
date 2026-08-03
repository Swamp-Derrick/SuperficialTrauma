package com.swampd.superficialtrauma.common.body;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.damage.DamageClassification;
import com.swampd.superficialtrauma.common.damage.DamageKind;
import com.swampd.superficialtrauma.common.damage.DamageWindow;
import com.swampd.superficialtrauma.common.wound.WoundInstance;
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
    public static final int CURRENT_DATA_VERSION = 5;
    public static final int MAX_WOUNDS = 8;
    public static final long DAMAGE_WINDOW_TICKS = 20L * 20L;
    public static final long WOUND_PROGRESSION_INTERVAL_TICKS = 20L;
    public static final long STRESS_DURATION_TICKS = 20L * 20L;
    public static final long PAIN_RECOVERY_INTERVAL_TICKS = 30L;
    public static final long MOVEMENT_BLEEDING_LINGER_TICKS = 2L * 20L;

    private static final String TAG_DATA_VERSION = "DataVersion";
    private static final String TAG_REVISION = "Revision";
    private static final String TAG_LIFE_STATE = "LifeState";
    private static final String TAG_BASE_PAIN = "BasePain";
    private static final String LEGACY_TAG_PAIN = "Pain";
    private static final String TAG_INFECTION = "Infection";
    private static final String TAG_BLOOD_DRUG_CONCENTRATION = "BloodDrugConcentration";
    private static final String TAG_ADRENALINE_LEVEL = "AdrenalineLevel";
    private static final String TAG_BLOOD_OXYGEN = "BloodOxygen";
    private static final String TAG_BRAIN_DEATH_DEADLINE = "BrainDeathDeadlineGameTime";
    private static final String TAG_CARDIAC_ARREST_EVENT_ID = "CardiacArrestEventId";
    private static final String TAG_ACCUMULATED_CPR_SECONDS = "AccumulatedCprSeconds";
    private static final String TAG_WOUNDS = "Wounds";
    private static final String TAG_DAMAGE_WINDOWS = "DamageWindows";
    private static final String TAG_LAST_WOUND_PROGRESSION_GAME_TIME = "LastWoundProgressionGameTime";
    private static final String TAG_STRESS_END_GAME_TIME = "StressEndGameTime";
    private static final String TAG_NEXT_PAIN_RECOVERY_GAME_TIME = "NextPainRecoveryGameTime";
    private static final String TAG_PROGRESSION_PAUSED_AT_GAME_TIME = "ProgressionPausedAtGameTime";
    private static final String TAG_MOVEMENT_BLEEDING_END_GAME_TIME = "MovementBleedingEndGameTime";
    private static final String TAG_MOVEMENT_BLEEDING_ACTIVE = "MovementBleedingActive";
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
    private float basePain;
    private float infection;
    private float bloodDrugConcentration;
    private int adrenalineLevel;
    private float bloodOxygen;
    private long brainDeathDeadlineGameTime;
    private UUID cardiacArrestEventId;
    private int accumulatedCprSeconds;
    private final List<WoundInstance> wounds = new ArrayList<>();
    private final EnumMap<WoundType, DamageWindow> damageWindows = new EnumMap<>(WoundType.class);
    private long lastWoundProgressionGameTime;
    private long stressEndGameTime;
    private long nextPainRecoveryGameTime;
    private long progressionPausedAtGameTime;
    private long movementBleedingEndGameTime;
    private boolean movementBleedingActive;
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

    public long stressRemainingTicks(long gameTime) {
        return stressEndGameTime < 0L ? 0L : Math.max(0L, stressEndGameTime - gameTime);
    }

    public boolean isStressActive(long gameTime) {
        return stressRemainingTicks(gameTime) > 0L;
    }

    public float infection() {
        return infection;
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

    public Optional<UUID> cardiacArrestEventId() {
        return Optional.ofNullable(cardiacArrestEventId);
    }

    public int accumulatedCprSeconds() {
        return accumulatedCprSeconds;
    }

    public List<WoundInstance> wounds() {
        return Collections.unmodifiableList(wounds);
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

    public void resumeBodyProgression(long gameTime) {
        if (progressionPausedAtGameTime >= 0L && gameTime >= progressionPausedAtGameTime) {
            long pausedTicks = gameTime - progressionPausedAtGameTime;
            stressEndGameTime = shiftDeadline(stressEndGameTime, pausedTicks);
            nextPainRecoveryGameTime = shiftDeadline(nextPainRecoveryGameTime, pausedTicks);
            movementBleedingEndGameTime = shiftDeadline(movementBleedingEndGameTime, pausedTicks);
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
        return advanceBodyProgression(gameTime, false);
    }

    public BodyProgressionResult advanceBodyProgression(long gameTime, boolean traumaticMovement) {
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

        float recoveredBasePain = recoverBasePain(gameTime);
        return finishBodyProgression(
                progressedWounds,
                healedWounds,
                expiredDamageWindows,
                expiredTransientWoundTags,
                recoveredBasePain,
                bleedingDamage,
                bleedingTimerChanged,
                movementBleedingStateChanged
        );
    }

    private BodyProgressionResult finishBodyProgression(
            int progressedWounds,
            int healedWounds,
            int expiredDamageWindows,
            int expiredTransientWoundTags,
            float recoveredBasePain,
            float bleedingDamage,
            boolean bleedingTimerChanged,
            boolean movementBleedingStateChanged
    ) {
        if (progressedWounds == 0
                && healedWounds == 0
                && expiredDamageWindows == 0
                && expiredTransientWoundTags == 0
                && recoveredBasePain <= 0.0F
                && bleedingDamage <= 0.0F
                && !bleedingTimerChanged
                && !movementBleedingStateChanged) {
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
                bleedingDamage
        );
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

    public void copyFrom(BodyState other) {
        deserializeNBT(other.serializeNBT());
    }

    private void markChanged() {
        revision++;
    }

    private void resetToDefaults() {
        revision = 0L;
        lifeState = BodyLifeState.ACTIVE;
        basePain = 0.0F;
        infection = 0.0F;
        bloodDrugConcentration = 0.0F;
        adrenalineLevel = 0;
        bloodOxygen = 30.0F;
        brainDeathDeadlineGameTime = -1L;
        cardiacArrestEventId = null;
        accumulatedCprSeconds = 0;
        wounds.clear();
        damageWindows.clear();
        lastWoundProgressionGameTime = -1L;
        stressEndGameTime = -1L;
        nextPainRecoveryGameTime = -1L;
        progressionPausedAtGameTime = -1L;
        movementBleedingEndGameTime = -1L;
        movementBleedingActive = false;
        lastFinalDamage = 0.0F;
        lastDamageType = "none";
        lastDamageKind = DamageKind.UNKNOWN;
        lastDamageReason = "none";
        lastProjectileEntityId = "none";
        lastAmmoId = "none";
        lastWeaponId = "none";
        lastDamageGameTime = -1L;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_DATA_VERSION, CURRENT_DATA_VERSION);
        tag.putLong(TAG_REVISION, revision);
        tag.putString(TAG_LIFE_STATE, lifeState.serializedName());
        tag.putFloat(TAG_BASE_PAIN, basePain);
        tag.putFloat(TAG_INFECTION, infection);
        tag.putFloat(TAG_BLOOD_DRUG_CONCENTRATION, bloodDrugConcentration);
        tag.putInt(TAG_ADRENALINE_LEVEL, adrenalineLevel);
        tag.putFloat(TAG_BLOOD_OXYGEN, bloodOxygen);
        tag.putLong(TAG_BRAIN_DEATH_DEADLINE, brainDeathDeadlineGameTime);
        if (cardiacArrestEventId != null) {
            tag.putUUID(TAG_CARDIAC_ARREST_EVENT_ID, cardiacArrestEventId);
        }
        tag.putInt(TAG_ACCUMULATED_CPR_SECONDS, accumulatedCprSeconds);

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
        basePain = tag.contains(TAG_BASE_PAIN, Tag.TAG_ANY_NUMERIC)
                ? clamp(tag.getFloat(TAG_BASE_PAIN), 0.0F, 30.0F)
                : clamp(tag.getFloat(LEGACY_TAG_PAIN), 0.0F, 30.0F);
        infection = Math.max(0.0F, tag.getFloat(TAG_INFECTION));
        bloodDrugConcentration = Math.max(0.0F, tag.getFloat(TAG_BLOOD_DRUG_CONCENTRATION));
        adrenalineLevel = Math.max(0, tag.getInt(TAG_ADRENALINE_LEVEL));
        bloodOxygen = tag.contains(TAG_BLOOD_OXYGEN, Tag.TAG_ANY_NUMERIC)
                ? clamp(tag.getFloat(TAG_BLOOD_OXYGEN), 0.0F, 30.0F)
                : 30.0F;
        brainDeathDeadlineGameTime = tag.contains(TAG_BRAIN_DEATH_DEADLINE, Tag.TAG_ANY_NUMERIC)
                ? tag.getLong(TAG_BRAIN_DEATH_DEADLINE)
                : -1L;
        cardiacArrestEventId = tag.hasUUID(TAG_CARDIAC_ARREST_EVENT_ID)
                ? tag.getUUID(TAG_CARDIAC_ARREST_EVENT_ID)
                : null;
        accumulatedCprSeconds = Math.max(0, tag.getInt(TAG_ACCUMULATED_CPR_SECONDS));

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
}
