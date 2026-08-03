package com.swampd.superficialtrauma.common.body;

import com.swampd.superficialtrauma.common.damage.DamageClassification;
import com.swampd.superficialtrauma.common.damage.DamageKind;
import com.swampd.superficialtrauma.common.wound.WoundInstance;
import com.swampd.superficialtrauma.common.wound.WoundTag;
import com.swampd.superficialtrauma.common.wound.WoundType;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public final class BodyStateRoundTripTest {
    private static final float EPSILON = 0.0001F;

    private BodyStateRoundTripTest() {
    }

    public static void main(String[] args) {
        verifyHalfOpenWoundRanges();
        verifyPendingDamageAccumulation();
        verifyIndependentDamageWindows();
        verifyWoundDefinitions();
        verifyNaturalHealingRates();
        verifyTimedWoundProgression();
        verifyPendingWindowExpiry();
        verifyNbtRoundTrip();
        verifyNonBluntNbtRoundTrip();
        verifyCgmDamageTraceRoundTrip();
        verifyVersionOneMigrationDefaults();
        verifyVersionTwoProgressionMigrationDefaults();
        verifyWoundLimitAndActiveWindowUpdate();
        System.out.println("Superficial Trauma BodyState self-test passed.");
    }

    private static void verifyHalfOpenWoundRanges() {
        assertEquals(0, WoundInstance.bluntSeverityFor(0.0F), "0 must not create a wound");
        assertEquals(0, WoundInstance.bluntSeverityFor(1.4999F), "value below 1.5 must not create a wound");
        assertEquals(1, WoundInstance.bluntSeverityFor(1.5F), "1.5 must enter severity 1");
        assertEquals(1, WoundInstance.bluntSeverityFor(3.9999F), "value below 4 must remain severity 1");
        assertEquals(2, WoundInstance.bluntSeverityFor(4.0F), "4 must enter severity 2");
        assertEquals(2, WoundInstance.bluntSeverityFor(12.9999F), "value below 13 must remain severity 2");
        assertEquals(3, WoundInstance.bluntSeverityFor(13.0F), "13 must enter severity 3");

        assertEquals(0, WoundInstance.sharpSeverityFor(0.4999F), "sharp value below 0.5 must not create a wound");
        assertEquals(1, WoundInstance.sharpSeverityFor(0.5F), "0.5 must enter sharp severity 1");
        assertEquals(1, WoundInstance.sharpSeverityFor(4.9999F), "sharp value below 5 must remain severity 1");
        assertEquals(2, WoundInstance.sharpSeverityFor(5.0F), "5 must enter sharp severity 2");
        assertEquals(2, WoundInstance.sharpSeverityFor(14.9999F), "sharp value below 15 must remain severity 2");
        assertEquals(3, WoundInstance.sharpSeverityFor(15.0F), "15 must enter sharp severity 3");

        assertEquals(0, WoundInstance.burnSeverityFor(0.0F), "zero burn damage must not create a wound");
        assertEquals(1, WoundInstance.burnSeverityFor(0.0001F), "positive burn damage must enter severity 1");
        assertEquals(1, WoundInstance.burnSeverityFor(4.9999F), "burn value below 5 must remain severity 1");
        assertEquals(2, WoundInstance.burnSeverityFor(5.0F), "5 must enter burn severity 2");
        assertEquals(2, WoundInstance.burnSeverityFor(15.9999F), "burn value below 16 must remain severity 2");
        assertEquals(3, WoundInstance.burnSeverityFor(16.0F), "16 must enter burn severity 3");

        assertEquals(0, WoundInstance.explosionSeverityFor(3.9999F), "explosion value below 4 must not create a wound");
        assertEquals(1, WoundInstance.explosionSeverityFor(4.0F), "4 must enter explosion severity 1");
        assertEquals(1, WoundInstance.explosionSeverityFor(7.9999F), "explosion value below 8 must remain severity 1");
        assertEquals(2, WoundInstance.explosionSeverityFor(8.0F), "8 must enter explosion severity 2");
        assertEquals(2, WoundInstance.explosionSeverityFor(15.9999F), "explosion value below 16 must remain severity 2");
        assertEquals(3, WoundInstance.explosionSeverityFor(16.0F), "16 must enter explosion severity 3");
    }

    private static void verifyPendingDamageAccumulation() {
        BodyState state = new BodyState();
        WoundUpdateResult first = state.applyBluntDamage(1.0F, 100L);
        assertEquals(WoundUpdateResult.Status.PENDING, first.status(), "first sub-threshold hit must remain pending");
        assertEquals(0, state.wounds().size(), "pending damage must not appear as a wound");

        WoundUpdateResult second = state.applyBluntDamage(0.5F, 101L);
        assertEquals(WoundUpdateResult.Status.CREATED, second.status(), "second hit must cross the 1.5 threshold");
        assertEquals(1, state.wounds().size(), "crossing the threshold must create one wound");
        assertFloatEquals(1.5F, state.wounds().get(0).accumulatedDamage(), "pending A must carry into the wound");
    }

    private static void verifyIndependentDamageWindows() {
        BodyState state = new BodyState();
        WoundUpdateResult pendingExplosion = state.applyDamage(WoundType.EXPLOSION, 3.0F, 100L);
        WoundUpdateResult sharp = state.applyDamage(WoundType.SHARP, 0.5F, 101L);
        WoundUpdateResult explosion = state.applyDamage(WoundType.EXPLOSION, 1.0F, 102L);
        WoundUpdateResult burn = state.applyDamage(WoundType.BURN, 0.25F, 103L);

        assertEquals(WoundUpdateResult.Status.PENDING, pendingExplosion.status(), "sub-threshold explosion damage must remain pending");
        assertEquals(WoundUpdateResult.Status.CREATED, sharp.status(), "sharp damage must use its own window");
        assertEquals(WoundUpdateResult.Status.CREATED, explosion.status(), "explosion damage must retain its earlier pending A");
        assertEquals(WoundUpdateResult.Status.CREATED, burn.status(), "any positive burn damage must create a wound");
        assertEquals(3, state.wounds().size(), "three independently classified wounds must be retained");
        assertFloatEquals(4.0F, requireWound(explosion).accumulatedDamage(), "explosion A must accumulate independently");
    }

    private static void verifyWoundDefinitions() {
        WoundInstance fragment = WoundInstance.create(WoundType.EXPLOSION, 4.0F, 0L, 400L);
        assertEquals(1, fragment.severity(), "4 explosion damage must create a level-1 fragment wound");
        assertEquals(true, fragment.woundTags().contains(WoundTag.NEEDS_DEBRIDEMENT_1), "fragment wound must need debridement");
        assertEquals(true, fragment.woundTags().contains(WoundTag.PAIN_1), "fragment wound must carry pain 1");
        assertFloatEquals(0.3F, fragment.baseHealingPerSecond(), "level-1 fragment healing rate must be recorded");

        WoundInstance extensive = WoundInstance.create(WoundType.EXPLOSION, 16.0F, 0L, 400L);
        assertEquals(true, extensive.woundTags().contains(WoundTag.NECROSIS_3), "level-3 explosion wound must carry necrosis 3");
        assertEquals(true, extensive.woundTags().contains(WoundTag.BLEEDING_2), "level-3 explosion wound must carry bleeding 2");
        assertFloatEquals(0.0F, extensive.baseHealingPerSecond(), "level-3 explosion wound must not naturally heal");
        assertFloatEquals(10.0F, extensive.minimumHealingProgressWithoutSkinGraft(), "skin-graft floor must be recorded as H=10");
    }

    private static void verifyNaturalHealingRates() {
        assertNaturalHealing(WoundType.BLUNT, 1.5F, 99.0F, "level-1 blunt");
        assertNaturalHealing(WoundType.BLUNT, 4.0F, 99.5F, "level-2 blunt");
        assertNaturalHealing(WoundType.BLUNT, 13.0F, 99.8F, "level-3 blunt");
        assertNaturalHealing(WoundType.SHARP, 0.5F, 99.0F, "level-1 sharp");
        assertNaturalHealing(WoundType.SHARP, 5.0F, 99.9F, "level-2 sharp");
        assertNoNaturalHealing(WoundType.SHARP, 15.0F, "level-3 sharp");
        assertNaturalHealing(WoundType.BURN, 0.1F, 99.2F, "level-1 burn");
        assertNaturalHealing(WoundType.BURN, 5.0F, 99.5F, "level-2 burn");
        assertNoNaturalHealing(WoundType.BURN, 16.0F, "level-3 burn");
        assertNaturalHealing(WoundType.EXPLOSION, 4.0F, 99.7F, "level-1 explosion");
        assertNaturalHealing(WoundType.EXPLOSION, 8.0F, 99.8F, "level-2 explosion");
        assertNoNaturalHealing(WoundType.EXPLOSION, 16.0F, "level-3 explosion");
    }

    private static void verifyTimedWoundProgression() {
        BodyState state = new BodyState();
        state.applyDamage(WoundType.BLUNT, 1.5F, 0L);
        state.resumeWoundProgression(0L);

        WoundProgressionResult subSecond = state.advanceWoundHealing(19L);
        assertEquals(false, subSecond.changed(), "sub-second tick time must not alter H");
        assertFloatEquals(100.0F, state.wounds().get(0).healingProgress(), "H must remain unchanged before one second");

        WoundProgressionResult firstSecond = state.advanceWoundHealing(20L);
        assertEquals(true, firstSecond.changed(), "one full second must advance natural healing");
        assertEquals(1, firstSecond.progressedWounds(), "one wound must advance");
        assertFloatEquals(99.0F, state.wounds().get(0).healingProgress(), "level-1 blunt H must decrease by one per second");

        state.advanceWoundHealing(50L);
        assertFloatEquals(98.0F, state.wounds().get(0).healingProgress(), "only complete seconds must be processed");
        assertEquals(40L, state.lastWoundProgressionGameTime(), "partial tick remainder must be retained");
        state.advanceWoundHealing(60L);
        assertFloatEquals(97.0F, state.wounds().get(0).healingProgress(), "retained partial ticks must contribute later");

        state.pauseWoundProgression();
        state.resumeWoundProgression(1_000L);
        WoundProgressionResult resumed = state.advanceWoundHealing(1_020L);
        assertEquals(true, resumed.changed(), "online progression must resume from the new baseline");
        assertFloatEquals(96.0F, state.wounds().get(0).healingProgress(), "offline elapsed time must not be applied");

        BodyState completed = new BodyState();
        completed.applyDamage(WoundType.BLUNT, 1.5F, 0L);
        completed.resumeWoundProgression(0L);
        WoundProgressionResult completion = completed.advanceWoundHealing(2_000L);
        assertEquals(1, completion.healedWounds(), "H reaching zero must remove the whole wound");
        assertEquals(0, completed.wounds().size(), "healed wound must no longer be stored");

        BodyState stalled = new BodyState();
        stalled.applyDamage(WoundType.SHARP, 15.0F, 0L);
        stalled.resumeWoundProgression(0L);
        WoundProgressionResult noProgress = stalled.advanceWoundHealing(2_000L);
        assertEquals(false, noProgress.changed(), "a non-self-healing wound must remain unchanged");
        assertFloatEquals(100.0F, stalled.wounds().get(0).healingProgress(), "non-self-healing H must remain at 100");
    }

    private static void verifyPendingWindowExpiry() {
        BodyState state = new BodyState();
        state.applyDamage(WoundType.EXPLOSION, 3.0F, 0L);
        state.resumeWoundProgression(0L);

        WoundProgressionResult stillOpen = state.advanceWoundHealing(399L);
        assertEquals(false, stillOpen.changed(), "pending damage must remain visible before the half-open window end");
        assertEquals(1, state.damageWindows().size(), "pending window must remain stored before tick 400");

        WoundProgressionResult expired = state.advanceWoundHealing(400L);
        assertEquals(true, expired.changed(), "pending damage expiry must update BodyState");
        assertEquals(1, expired.expiredDamageWindows(), "exactly one pending window must expire");
        assertEquals(0, state.damageWindows().size(), "expired pending damage must be removed at the window end");
    }

    private static void verifyNbtRoundTrip() {
        BodyState original = new BodyState();
        original.recordFinalDamage(4.0F, "fall", DamageClassification.blunt("fall"), 200L);
        WoundUpdateResult created = original.applyBluntDamage(4.0F, 200L);
        original.resumeWoundProgression(200L);
        assertEquals(WoundUpdateResult.Status.CREATED, created.status(), "4 damage must create a severity-2 wound");
        UUID originalId = requireWound(created).id();

        CompoundTag serialized = original.serializeNBT();
        assertEquals(BodyState.CURRENT_DATA_VERSION, serialized.getInt("DataVersion"), "serialized data version must be current");

        BodyState restored = new BodyState();
        restored.deserializeNBT(serialized);
        assertEquals(BodyState.CURRENT_DATA_VERSION, restored.dataVersion(), "restored data version must be current");
        assertEquals(1, restored.wounds().size(), "round trip must preserve the wound list");
        WoundInstance restoredWound = restored.wounds().get(0);
        assertEquals(originalId, restoredWound.id(), "round trip must preserve wound UUID");
        assertEquals(2, restoredWound.severity(), "round trip must preserve severity");
        assertFloatEquals(4.0F, restoredWound.accumulatedDamage(), "round trip must preserve A");
        assertFloatEquals(100.0F, restoredWound.healingProgress(), "round trip must preserve H");
        assertEquals(200L, restored.lastWoundProgressionGameTime(), "round trip must preserve the progression clock");
        assertFloatEquals(4.0F, restored.lastFinalDamage(), "round trip must preserve last final damage diagnostics");
        assertEquals("fall", restored.lastDamageType(), "round trip must preserve damage type diagnostics");
        assertEquals(DamageKind.BLUNT, restored.lastDamageKind(), "round trip must preserve damage classification");
        assertEquals("fall", restored.lastDamageReason(), "round trip must preserve classification reason");
    }

    private static void verifyCgmDamageTraceRoundTrip() {
        BodyState original = new BodyState();
        DamageClassification classification = DamageClassification.cgmProjectile(
                DamageKind.CGM_HIGH_VELOCITY,
                "cgm_projectile_ammo_tag",
                "cgm:projectile",
                "nzgexpansion:medium_bullet",
                "nzgexpansion:battle_rifle"
        );
        original.recordFinalDamage(7.0F, "cgm.bullet.killed", classification, 300L);

        BodyState restored = new BodyState();
        restored.deserializeNBT(original.serializeNBT());

        assertEquals(DamageKind.CGM_HIGH_VELOCITY, restored.lastDamageKind(), "CGM classification must survive NBT");
        assertEquals("cgm:projectile", restored.lastProjectileEntityId(), "projectile ID must survive NBT");
        assertEquals("nzgexpansion:medium_bullet", restored.lastAmmoId(), "ammo ID must survive NBT");
        assertEquals("nzgexpansion:battle_rifle", restored.lastWeaponId(), "weapon ID must survive NBT");
    }

    private static void verifyNonBluntNbtRoundTrip() {
        BodyState original = new BodyState();
        original.applyDamage(WoundType.SHARP, 5.0F, 400L);
        original.applyDamage(WoundType.BURN, 5.0F, 401L);
        original.applyDamage(WoundType.EXPLOSION, 16.0F, 402L);

        BodyState restored = new BodyState();
        restored.deserializeNBT(original.serializeNBT());

        assertEquals(3, restored.wounds().size(), "NBT must preserve all implemented wound types");
        assertEquals(WoundType.SHARP, restored.wounds().get(0).type(), "NBT must preserve sharp wounds");
        assertEquals(WoundType.BURN, restored.wounds().get(1).type(), "NBT must preserve burn wounds");
        assertEquals(WoundType.EXPLOSION, restored.wounds().get(2).type(), "NBT must preserve explosion wounds");
        assertEquals(true, restored.wounds().get(2).woundTags().contains(WoundTag.NECROSIS_3), "NBT must preserve explosion tags");
    }

    private static void verifyVersionOneMigrationDefaults() {
        CompoundTag versionOne = new CompoundTag();
        versionOne.putInt("DataVersion", 1);
        versionOne.putFloat("LastFinalDamage", 2.0F);
        versionOne.putString("LastDamageType", "fall");

        BodyState restored = new BodyState();
        restored.deserializeNBT(versionOne);

        assertEquals(DamageKind.UNKNOWN, restored.lastDamageKind(), "version 1 data must use a safe unknown classification");
        assertEquals("none", restored.lastAmmoId(), "version 1 data must not invent an ammo ID");
        assertEquals("none", restored.lastWeaponId(), "version 1 data must not invent a weapon ID");
    }

    private static void verifyVersionTwoProgressionMigrationDefaults() {
        CompoundTag versionTwo = new CompoundTag();
        versionTwo.putInt("DataVersion", 2);

        BodyState restored = new BodyState();
        restored.deserializeNBT(versionTwo);

        assertEquals(-1L, restored.lastWoundProgressionGameTime(), "version 2 data must wait for an online progression baseline");
    }

    private static void verifyWoundLimitAndActiveWindowUpdate() {
        BodyState state = new BodyState();
        long gameTime = 0L;
        for (int i = 0; i < BodyState.MAX_WOUNDS; i++) {
            WoundUpdateResult created = state.applyBluntDamage(1.5F, gameTime);
            assertEquals(WoundUpdateResult.Status.CREATED, created.status(), "each expired window must create a new wound");
            gameTime += BodyState.DAMAGE_WINDOW_TICKS + 1L;
        }
        assertEquals(BodyState.MAX_WOUNDS, state.wounds().size(), "BodyState must retain exactly eight wounds");

        long eighthWindowStart = (BodyState.MAX_WOUNDS - 1L) * (BodyState.DAMAGE_WINDOW_TICKS + 1L);
        WoundUpdateResult activeUpdate = state.applyBluntDamage(0.5F, eighthWindowStart + 1L);
        assertEquals(WoundUpdateResult.Status.UPDATED, activeUpdate.status(), "an active wound may update at the eight-wound limit");

        WoundUpdateResult rejected = state.applyBluntDamage(1.5F, gameTime);
        assertEquals(WoundUpdateResult.Status.LIMIT_REACHED, rejected.status(), "a ninth wound must be rejected");
        assertEquals(BodyState.MAX_WOUNDS, state.wounds().size(), "rejecting the ninth wound must not change the list size");
    }

    private static WoundInstance requireWound(WoundUpdateResult result) {
        if (result.wound() == null) {
            throw new AssertionError("Expected a wound for status " + result.status());
        }
        return result.wound();
    }

    private static void assertNaturalHealing(
            WoundType type,
            float accumulatedDamage,
            float expectedProgress,
            String description
    ) {
        WoundInstance wound = WoundInstance.create(type, accumulatedDamage, 0L, 400L);
        assertEquals(true, wound.advanceNaturalHealing(1.0F), description + " must naturally progress");
        assertFloatEquals(expectedProgress, wound.healingProgress(), description + " must use the reviewed rate");
    }

    private static void assertNoNaturalHealing(WoundType type, float accumulatedDamage, String description) {
        WoundInstance wound = WoundInstance.create(type, accumulatedDamage, 0L, 400L);
        assertEquals(false, wound.advanceNaturalHealing(1.0F), description + " must not naturally progress");
        assertFloatEquals(100.0F, wound.healingProgress(), description + " H must remain unchanged");
    }

    private static void assertFloatEquals(float expected, float actual, String message) {
        if (Math.abs(expected - actual) > EPSILON) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }
}
