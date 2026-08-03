package com.swampd.superficialtrauma.common.body;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.damage.DamageClassification;
import com.swampd.superficialtrauma.common.damage.DamageKind;
import com.swampd.superficialtrauma.common.damage.DamageWindow;
import com.swampd.superficialtrauma.common.wound.WoundInstance;
import com.swampd.superficialtrauma.common.wound.WoundType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class BodyState implements INBTSerializable<CompoundTag> {
    public static final int CURRENT_DATA_VERSION = 2;
    public static final int MAX_WOUNDS = 8;
    public static final long DAMAGE_WINDOW_TICKS = 20L * 20L;

    private static final String TAG_DATA_VERSION = "DataVersion";
    private static final String TAG_REVISION = "Revision";
    private static final String TAG_LIFE_STATE = "LifeState";
    private static final String TAG_PAIN = "Pain";
    private static final String TAG_INFECTION = "Infection";
    private static final String TAG_BLOOD_DRUG_CONCENTRATION = "BloodDrugConcentration";
    private static final String TAG_ADRENALINE_LEVEL = "AdrenalineLevel";
    private static final String TAG_BLOOD_OXYGEN = "BloodOxygen";
    private static final String TAG_BRAIN_DEATH_DEADLINE = "BrainDeathDeadlineGameTime";
    private static final String TAG_CARDIAC_ARREST_EVENT_ID = "CardiacArrestEventId";
    private static final String TAG_ACCUMULATED_CPR_SECONDS = "AccumulatedCprSeconds";
    private static final String TAG_WOUNDS = "Wounds";
    private static final String TAG_DAMAGE_WINDOWS = "DamageWindows";
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
    private float pain;
    private float infection;
    private float bloodDrugConcentration;
    private int adrenalineLevel;
    private float bloodOxygen;
    private long brainDeathDeadlineGameTime;
    private UUID cardiacArrestEventId;
    private int accumulatedCprSeconds;
    private final List<WoundInstance> wounds = new ArrayList<>();
    private final EnumMap<WoundType, DamageWindow> damageWindows = new EnumMap<>(WoundType.class);
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
        return pain;
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
        if (finalDamage <= 0.0F) {
            return new WoundUpdateResult(WoundUpdateResult.Status.PENDING, null, 0.0F);
        }

        Optional<WoundInstance> activeWound = wounds.stream()
                .filter(wound -> wound.type() == WoundType.BLUNT)
                .filter(wound -> wound.isAccumulationWindowOpen(gameTime))
                .max((first, second) -> Long.compare(first.createdGameTime(), second.createdGameTime()));

        if (activeWound.isPresent()) {
            WoundInstance wound = activeWound.get();
            wound.addAccumulatedDamage(finalDamage);
            markChanged();
            return new WoundUpdateResult(WoundUpdateResult.Status.UPDATED, wound, wound.accumulatedDamage());
        }

        DamageWindow pendingWindow = damageWindows.get(WoundType.BLUNT);
        if (pendingWindow == null || !pendingWindow.isOpen(gameTime)) {
            pendingWindow = new DamageWindow(
                    WoundType.BLUNT,
                    0.0F,
                    gameTime,
                    gameTime + DAMAGE_WINDOW_TICKS
            );
            damageWindows.put(WoundType.BLUNT, pendingWindow);
        }
        pendingWindow.addDamage(finalDamage);

        int severity = WoundInstance.bluntSeverityFor(pendingWindow.accumulatedDamage());
        if (severity == 0) {
            markChanged();
            return new WoundUpdateResult(
                    WoundUpdateResult.Status.PENDING,
                    null,
                    pendingWindow.accumulatedDamage()
            );
        }

        if (wounds.size() >= MAX_WOUNDS) {
            damageWindows.remove(WoundType.BLUNT);
            markChanged();
            return new WoundUpdateResult(
                    WoundUpdateResult.Status.LIMIT_REACHED,
                    null,
                    pendingWindow.accumulatedDamage()
            );
        }

        WoundInstance wound = WoundInstance.createBlunt(
                pendingWindow.accumulatedDamage(),
                pendingWindow.startedGameTime(),
                pendingWindow.endGameTime()
        );
        wounds.add(wound);
        damageWindows.remove(WoundType.BLUNT);
        markChanged();
        return new WoundUpdateResult(WoundUpdateResult.Status.CREATED, wound, wound.accumulatedDamage());
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
        pain = 0.0F;
        infection = 0.0F;
        bloodDrugConcentration = 0.0F;
        adrenalineLevel = 0;
        bloodOxygen = 30.0F;
        brainDeathDeadlineGameTime = -1L;
        cardiacArrestEventId = null;
        accumulatedCprSeconds = 0;
        wounds.clear();
        damageWindows.clear();
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
        tag.putFloat(TAG_PAIN, pain);
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
        pain = clamp(tag.getFloat(TAG_PAIN), 0.0F, 30.0F);
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
