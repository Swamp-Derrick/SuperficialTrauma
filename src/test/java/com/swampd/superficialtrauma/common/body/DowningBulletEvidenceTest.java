package com.swampd.superficialtrauma.common.body;

import com.swampd.superficialtrauma.common.damage.*;
import com.swampd.superficialtrauma.common.forensics.AutopsyAction;
import com.swampd.superficialtrauma.common.forensics.AutopsyReport;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DowningBulletEvidenceTest {
    private BodyState downed(float damage, boolean enabled) {
        BodyState state = new BodyState();
        state.configureSeriousTrauma(enabled, 100);
        state.recordFinalDamage(damage, "cgm.bullet", new DamageClassification(null,
                DamageKind.CGM_HIGH_VELOCITY, "test", "cgm:projectile", "cgm:advanced_bullet", "cgm:rifle"), 10, 100);
        assertTrue(state.incapacitateFromLastDamage(CollapseReason.HEMORRHAGIC_SHOCK, 100));
        return state;
    }

    @Test void thresholdIsStrictAndRollIsExactlyHalf() {
        for (float damage : new float[]{4, 5, 5.001F, 9, 30}) {
            for (double roll : new double[]{0, .49999, .5, .99999}) {
                var state = downed(damage, true);
                AtomicInteger rolls = new AtomicInteger();
                assertTrue(state.completeDowningBulletHit(state.downingHitRecord().orElseThrow(),
                        BulletHitLocation.HEAD, damage, () -> { rolls.incrementAndGet(); return roll; }));
                boolean fatal = damage > 5 && roll < .5;
                assertEquals(damage > 5 ? 1 : 0, rolls.get());
                assertEquals(fatal, state.lifeState() == BodyLifeState.BRAIN_DEAD);
                assertEquals(fatal, state.downingHitRecord().orElseThrow().fatalBrainInjury());
                assertEquals(damage > 5, state.seriousTrauma().conditions().contains(SeriousTraumaState.Condition.CONCUSSION));
                assertFalse(state.administrativeDeath());
                assertFalse(state.voluntaryDeath());
            }
        }
    }

    @Test void configOffStillRecordsForensicsButCannotRollOrGiveConcussion() {
        var state = downed(40, false);
        assertTrue(state.completeDowningBulletHit(state.downingHitRecord().orElseThrow(),
                BulletHitLocation.HEAD, 40, () -> { fail("Disabled feature must not draw RNG"); return 0; }));
        assertEquals(BodyLifeState.INCAPACITATED, state.lifeState());
        assertEquals(BulletHitLocation.HEAD, state.downingHitRecord().orElseThrow().bulletLocation());
        assertTrue(state.seriousTrauma().conditions().isEmpty());
    }

    @Test void chestAndLimbsCannotBorrowHeadPoolOrWholeVolleyDamage() {
        for (var location : new BulletHitLocation[]{BulletHitLocation.CHEST, BulletHitLocation.LIMBS}) {
            var state = downed(40, true);
            state.seriousTrauma().record(GunshotRegion.HEAD, 50, 90);
            assertTrue(state.completeDowningBulletHit(state.downingHitRecord().orElseThrow(), location,
                    0, () -> { fail("Only this shot's head damage is eligible"); return 0; }));
            assertEquals(BodyLifeState.INCAPACITATED, state.lifeState());
            assertEquals(location, state.downingHitRecord().orElseThrow().bulletLocation());
        }
    }

    @Test void immutableEvidenceCannotRerollOnExecutionOrReload() {
        var state = downed(8, true);
        var original = state.downingHitRecord().orElseThrow();
        assertTrue(state.completeDowningBulletHit(original, BulletHitLocation.HEAD, 8, () -> .8));
        var frozen = state.downingHitRecord().orElseThrow();
        assertFalse(state.completeDowningBulletHit(original, BulletHitLocation.HEAD, 50, () -> 0));
        assertFalse(state.completeDowningBulletHit(frozen, BulletHitLocation.HEAD, 50, () -> 0));
        state.recordFinalDamage(50, "later", DamageClassification.blunt("later"), 101);
        assertSame(frozen, state.downingHitRecord().orElseThrow());
        BodyState restored = new BodyState();
        restored.deserializeNBT(state.serializeNBT());
        assertEquals(frozen, restored.downingHitRecord().orElseThrow());
        assertFalse(restored.completeDowningBulletHit(restored.downingHitRecord().orElseThrow(),
                BulletHitLocation.HEAD, 50, () -> 0));
        assertTrue(restored.seriousTrauma().conditions().contains(SeriousTraumaState.Condition.CONCUSSION));
    }

    @Test void staleVolleyCannotAttachToNewDownedEpisodeOrAdministrativeDeath() {
        var state = downed(8, true);
        var original = state.downingHitRecord().orElseThrow();
        state.forceRecoverForDebug();
        state.incapacitateFromLastDamage(CollapseReason.HEMORRHAGIC_SHOCK, 100);
        assertFalse(state.completeDowningBulletHit(original, BulletHitLocation.HEAD, 8, () -> 0));
        state.forceAdministrativeBrainDeath();
        assertFalse(state.completeDowningBulletHit(original, BulletHitLocation.HEAD, 8, () -> 0));
        assertTrue(state.downingHitRecord().isEmpty());
    }

    @Test void unsupportedPosesAndInvalidPointsDoNotFabricateLimbEvidence() {
        AABB box = new AABB(-.3, 0, -.3, .3, 1.8, .3);
        for (Pose pose : Pose.values()) {
            if (pose == Pose.STANDING || pose == Pose.CROUCHING) continue;
            assertEquals(BulletHitLocation.UNKNOWN, GunshotHitLocations.classifyImpact(box, 0, pose, new Vec3(0, 1.7, 0)));
        }
        assertEquals(BulletHitLocation.LIMBS, GunshotHitLocations.classifyImpact(box, 0, Pose.STANDING, new Vec3(0, .1, 0)));
        assertEquals(BulletHitLocation.UNKNOWN, GunshotHitLocations.classifyImpact(box, 0, Pose.STANDING, new Vec3(3, .1, 0)));
    }

    @Test void volleyHeadPriorityAndHeadOnlyDamageAreOrderIndependent() {
        for (boolean headFirst : new boolean[]{true, false}) {
            var accumulator = new ShotgunVolleyAccumulator();
            var key = new ShotgunVolleyAccumulator.VolleyKey(UUID.randomUUID(), UUID.randomUUID(), "ammo", "gun", 100);
            accumulator.addLocatedHit(key, 3, 0, 3, 100, "bullet", headFirst ? BulletHitLocation.HEAD : BulletHitLocation.CHEST);
            accumulator.addLocatedHit(key, 3, 0, 3, 100, "bullet", headFirst ? BulletHitLocation.CHEST : BulletHitLocation.HEAD);
            accumulator.addLocatedHit(key, 30, 0, 3, 100, "bullet", BulletHitLocation.LIMBS);
            var volley = accumulator.take(key);
            assertEquals(BulletHitLocation.HEAD, volley.bulletLocation());
            assertEquals(3, volley.headFinalDamage());
            assertEquals(36, volley.totalFinalDamage());
            assertNull(accumulator.take(key));
            var state = downed(36, true);
            state.completeDowningBulletHit(state.downingHitRecord().orElseThrow(), volley.bulletLocation(),
                    volley.headFinalDamage(), () -> { fail("Total 36 is not head damage 36"); return 0; });
            assertEquals(BodyLifeState.INCAPACITATED, state.lifeState());
        }
    }

    @Test void brainDeathPersistsButAutopsyEvidenceIsHiddenUntilDetailedExam() {
        var state = downed(8, true);
        state.completeDowningBulletHit(state.downingHitRecord().orElseThrow(), BulletHitLocation.HEAD, 8, () -> 0);
        var restored = new BodyState();
        restored.deserializeNBT(state.serializeNBT());
        assertEquals(BodyLifeState.BRAIN_DEAD, restored.lifeState());
        var hit = restored.downingHitRecord().orElseThrow();
        assertTrue(hit.fatalBrainInjury());
        var snapshot = new com.swampd.superficialtrauma.common.entity.CorpseSnapshot(UUID.randomUUID(), "Victim", "", "", 100,
                new DownedPoseSnapshot(100, 0, DownedPosture.STANDING, DownedFallDirection.BACKWARD),
                List.of(), hit, CollapseReason.HEMORRHAGIC_SHOCK, false, false);
        assertEquals(hit, com.swampd.superficialtrauma.common.entity.CorpseSnapshot.load(snapshot.save()).downingHitRecord());
        for (boolean detailed : new boolean[]{false, true}) {
            var report = new AutopsyReport(1, "Victim", 0, List.of(), detailed, hit,
                    false, false, false, false, true, true, true, -1, AutopsyAction.NONE, -1);
            var received = AutopsyReport.load(report.save());
            assertEquals(detailed, report.save().contains("DowningHit"));
            assertEquals(detailed ? hit : null, received.downingHit());
        }
        var old = hit.serializeNBT();
        old.remove("BulletLocation"); old.remove("HeadFinalDamage"); old.remove("FatalBrainInjury");
        var legacy = DowningHitRecord.deserializeNBT(old);
        assertEquals(BulletHitLocation.UNKNOWN, legacy.bulletLocation());
        assertFalse(legacy.fatalBrainInjury());
    }
}
