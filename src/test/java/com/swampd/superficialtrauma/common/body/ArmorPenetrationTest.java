package com.swampd.superficialtrauma.common.body;

import com.swampd.superficialtrauma.common.damage.*;
import com.swampd.superficialtrauma.common.wound.WoundInstance;
import com.swampd.superficialtrauma.common.wound.WoundType;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static com.swampd.superficialtrauma.common.damage.ArmorPenetration.Piece.NONE;

class ArmorPenetrationTest {
    @Test void reviewedMaterialTableAndIndependentSlots() {
        int[][] armor = {{0,0,0,0}, {1,3,2,1}, {2,5,3,1}, {2,5,4,1}, {2,6,5,2}, {3,8,6,3}, {3,8,6,3}};
        double[][] expected = {{0,0,0,0}, {8.0/3,3,8.0/3,2.8}, {16.0/3,5,32.0/9,4.4},
                {16.0/3,5,40.0/9,4.8}, {16.0/3,6,56.0/9,6}, {12,12,12,12}, {14,14,14,14}};
        for (int i = 0; i < armor.length; i++) {
            double toughness = i == 5 ? 2 : i == 6 ? 3 : 0;
            var r = new ArmorPenetration.Profile(new ArmorPenetration.Piece(armor[i][0], toughness),
                    new ArmorPenetration.Piece(armor[i][1], toughness), new ArmorPenetration.Piece(armor[i][2], toughness),
                    new ArmorPenetration.Piece(armor[i][3], toughness));
            assertEquals(expected[i][0], r.rating(BulletHitLocation.HEAD), 1e-6);
            assertEquals(expected[i][1], r.rating(BulletHitLocation.CHEST), 1e-6);
            assertEquals(expected[i][2], r.rating(BulletHitLocation.LIMBS), 1e-6);
            assertEquals(expected[i][3], r.rating(BulletHitLocation.UNKNOWN), 1e-6);
        }
        var chestOnly = new ArmorPenetration.Profile(NONE, new ArmorPenetration.Piece(8, 3), NONE, NONE);
        assertEquals(0, chestOnly.rating(BulletHitLocation.HEAD));
        assertEquals(0, chestOnly.rating(BulletHitLocation.LIMBS));
        assertEquals(14, chestOnly.rating(BulletHitLocation.CHEST));
        assertEquals(4.7, chestOnly.overallRating(), 1e-6);
    }

    @Test void penetrationBoundariesAndWeaponPowersDoNotDependOnFinalDamage() {
        assertTrue(ArmorPenetration.penetrates(5, 5));
        assertFalse(ArmorPenetration.penetrates(5, 5.001));
        assertFalse(ArmorPenetration.penetrates(Double.NaN, 0));
        assertEquals(5, ArmorPenetration.bulletPower(WoundType.GUNSHOT_LOW_VELOCITY, 1));
        assertEquals(13, ArmorPenetration.bulletPower(WoundType.GUNSHOT_HIGH_VELOCITY, 100));
        assertEquals(8, ArmorPenetration.bulletPower(WoundType.GUNSHOT_SHOTGUN, 3));
        assertEquals(5, ArmorPenetration.bulletPower(WoundType.GUNSHOT_SHOTGUN, 3.001));
        assertEquals(5, ArmorPenetration.bulletPower(WoundType.GUNSHOT_SHOTGUN, Double.POSITIVE_INFINITY));
        assertEquals(6.4, ArmorPenetration.meleePower(8), 1e-6);
    }

    @Test void weakSmgAgainstUnarmoredPlayerAlwaysCreatesGunshotAndCanAccumulate() {
        var state = new BodyState();
        for (int i = 0; i < 3; i++) {
            var result = state.applyGunshotDamage(WoundType.GUNSHOT_LOW_VELOCITY, 2.9F, 0, 20, false, i);
            assertEquals(WoundType.GUNSHOT_LOW_VELOCITY, result.wound().type());
        }
        assertEquals(1, state.wounds().size());
        assertEquals(8.7, state.wounds().getFirst().accumulatedDamage(), 1e-5);
        assertEquals(2, state.wounds().getFirst().severity());
        var tiny = new BodyState().applyGunshotDamage(WoundType.GUNSHOT_HIGH_VELOCITY, .01F, 12, 10, false, 0);
        assertEquals(1, tiny.wound().severity());
    }

    @Test void strongArmorEligibilityAndSufficientAccumulationAreBothRequired() {
        var state = new BodyState();
        var wound = state.applyGunshotDamage(WoundType.GUNSHOT_HIGH_VELOCITY, 3, 12, 10, false, 0).wound();
        assertEquals(1, wound.severity());
        assertTrue(wound.fragmentationEligible());
        state.applyGunshotDamage(WoundType.GUNSHOT_HIGH_VELOCITY, 8.999F, 0, 10, false, 1);
        assertEquals(2, wound.severity());
        state.applyGunshotDamage(WoundType.GUNSHOT_HIGH_VELOCITY, .0011F, 0, 10, false, 2);
        assertEquals(3, wound.severity());
        var unarmored = new BodyState().applyGunshotDamage(WoundType.GUNSHOT_HIGH_VELOCITY, 30, 0, 10, false, 0);
        assertEquals(2, unarmored.wound().severity());
        var next = state.applyGunshotDamage(WoundType.GUNSHOT_HIGH_VELOCITY, 12, 0, 10, false, 400);
        assertNotSame(wound, next.wound());
        assertEquals(2, next.wound().severity());
    }

    @Test void mixedHeadChestAndOtherMergeOnlyPenetratingDamageByAmmoNotByBodyPart() {
        var state = new BodyState();
        state.configureSeriousTrauma(false, 0);
        var head = state.applyPenetratingGunshotDamage(WoundType.GUNSHOT_LOW_VELOCITY, 3, false, false, false, "cgm:basic_bullet", 0);
        state.recordGunshotLocations(head, 3, 3, 0, 0);
        state.applyGunshotDamage(WoundType.GUNSHOT_LOW_VELOCITY, 6, 14, 10, false, 1);
        var foot = state.applyPenetratingGunshotDamage(WoundType.GUNSHOT_LOW_VELOCITY, 3, false, false, false, "cgm:basic_bullet", 2);
        assertSame(head.wound(), foot.wound());
        assertEquals(6, head.wound().accumulatedDamage());
        assertEquals(java.util.Set.of(GunshotRegion.HEAD), head.wound().gunshotRegions());
        assertEquals(2, state.wounds().size());
        assertTrue(state.seriousTrauma().conditions().isEmpty());
        var otherAmmo = state.applyPenetratingGunshotDamage(WoundType.GUNSHOT_LOW_VELOCITY, 3, false, false, false, "test:other", 3);
        assertNotSame(head.wound(), otherAmmo.wound());
        assertEquals(3, state.wounds().size());
        var restored = new BodyState();
        restored.deserializeNBT(state.serializeNBT());
        var continued = restored.applyPenetratingGunshotDamage(WoundType.GUNSHOT_LOW_VELOCITY, 1, false, false, false, "cgm:basic_bullet", 4);
        assertEquals(head.wound().id(), continued.wound().id());
        assertEquals(7, continued.wound().accumulatedDamage());
        var legacy = head.wound().serializeNBT();
        legacy.remove("GunshotAmmoId");
        assertEquals("none", WoundInstance.deserializeNBT(legacy).gunshotAmmoId());
    }

    @Test void shotgunRoutingPreservesPerPelletArmorAndHeadEvidence() {
        var accumulator = new ShotgunVolleyAccumulator();
        var key = new ShotgunVolleyAccumulator.VolleyKey(UUID.randomUUID(), UUID.randomUUID(), "shell", "shotgun", 0);
        accumulator.addLocatedHit(key, 3, 0, 2, 0, "bullet", BulletHitLocation.HEAD);
        accumulator.addLocatedHit(key, 9, 14, 2, 0, "bullet", BulletHitLocation.CHEST);
        accumulator.addLocatedHit(key, 2, 0, 2, 0, "bullet", BulletHitLocation.LIMBS);
        accumulator.addLocatedHit(key, 8, 14, 2, 0, "bullet", BulletHitLocation.HEAD);
        var volley = accumulator.take(key);
        assertEquals(22, volley.totalFinalDamage());
        assertEquals(5, volley.penetratingFinalDamage());
        assertEquals(17, volley.blockedFinalDamage());
        assertEquals(11, volley.headFinalDamage());
        assertEquals(3, volley.penetratingHeadDamage());
        assertEquals(BulletHitLocation.HEAD, volley.bulletLocation());
        assertEquals(1, volley.regionalHits().size());
        assertTrue(volley.penetratingCloseRange());
        assertFalse(volley.penetratedStrongArmor());
        assertEquals(1, WoundInstance.gunshotSeverityFor(WoundType.GUNSHOT_SHOTGUN,
                volley.penetratingFinalDamage(), false, volley.penetratingCloseRange()));
    }

    @Test void stoppedHeadHitCannotRollBrainDeathEvenWithLargeFinalDamage() {
        var state = new BodyState();
        state.configureSeriousTrauma(true, 0);
        state.recordFinalDamage(20, "cgm.bullet", new DamageClassification(null, DamageKind.CGM_HIGH_VELOCITY,
                "test", "cgm:projectile", "cgm:advanced_bullet", "cgm:rifle"), 10, 0);
        state.incapacitateFromLastDamage(CollapseReason.HEMORRHAGIC_SHOCK, 0);
        assertTrue(state.completeDowningBulletHit(state.downingHitRecord().orElseThrow(), BulletHitLocation.HEAD,
                20, 0, () -> { fail("Stopped head bullets must not roll"); return 0; }));
        assertEquals(BodyLifeState.INCAPACITATED, state.lifeState());
        assertTrue(state.seriousTrauma().conditions().isEmpty());
        assertEquals(20, state.downingHitRecord().orElseThrow().headFinalDamage());
        assertEquals(BulletHitLocation.HEAD, state.downingHitRecord().orElseThrow().bulletLocation());
    }

    @Test void delayedVolleyCannotJoinAWoundCreatedAfterItsImpact() {
        var state = new BodyState();
        var first = state.applyPenetratingGunshotDamage(WoundType.GUNSHOT_SHOTGUN, 3, false, true, false, "shell", 0).wound();
        var next = state.applyPenetratingGunshotDamage(WoundType.GUNSHOT_SHOTGUN, 3, false, true, false, "shell", 400).wound();
        var delayed = state.applyPenetratingGunshotDamage(WoundType.GUNSHOT_SHOTGUN, 5, false, true, false, "shell", 399).wound();
        assertSame(first, delayed);
        assertEquals(8, first.accumulatedDamage());
        assertEquals(3, first.severity());
        assertEquals(3, next.accumulatedDamage());
        assertEquals(1, next.severity());
    }
}
