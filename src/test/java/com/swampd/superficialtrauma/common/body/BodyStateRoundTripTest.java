package com.swampd.superficialtrauma.common.body;

import com.swampd.superficialtrauma.common.damage.DamageClassification;
import com.swampd.superficialtrauma.common.damage.DamageKind;
import com.swampd.superficialtrauma.common.wound.WoundInstance;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public final class BodyStateRoundTripTest {
    private static final float EPSILON = 0.0001F;

    private BodyStateRoundTripTest() {
    }

    public static void main(String[] args) {
        verifyHalfOpenBluntRanges();
        verifyPendingDamageAccumulation();
        verifyNbtRoundTrip();
        verifyCgmDamageTraceRoundTrip();
        verifyVersionOneMigrationDefaults();
        verifyWoundLimitAndActiveWindowUpdate();
        System.out.println("Superficial Trauma BodyState self-test passed.");
    }

    private static void verifyHalfOpenBluntRanges() {
        assertEquals(0, WoundInstance.bluntSeverityFor(0.0F), "0 must not create a wound");
        assertEquals(0, WoundInstance.bluntSeverityFor(1.4999F), "value below 1.5 must not create a wound");
        assertEquals(1, WoundInstance.bluntSeverityFor(1.5F), "1.5 must enter severity 1");
        assertEquals(1, WoundInstance.bluntSeverityFor(3.9999F), "value below 4 must remain severity 1");
        assertEquals(2, WoundInstance.bluntSeverityFor(4.0F), "4 must enter severity 2");
        assertEquals(2, WoundInstance.bluntSeverityFor(12.9999F), "value below 13 must remain severity 2");
        assertEquals(3, WoundInstance.bluntSeverityFor(13.0F), "13 must enter severity 3");
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

    private static void verifyNbtRoundTrip() {
        BodyState original = new BodyState();
        original.recordFinalDamage(4.0F, "fall", DamageClassification.blunt("fall"), 200L);
        WoundUpdateResult created = original.applyBluntDamage(4.0F, 200L);
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
