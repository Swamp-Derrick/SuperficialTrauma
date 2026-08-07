package com.swampd.superficialtrauma.common.body;

import com.swampd.superficialtrauma.common.damage.DamageClassification;
import com.swampd.superficialtrauma.common.damage.DamageDowning;
import com.swampd.superficialtrauma.common.damage.DamageKind;
import com.swampd.superficialtrauma.common.damage.ShotgunVolleyAccumulator;
import com.swampd.superficialtrauma.common.entity.CorpseSnapshot;
import com.swampd.superficialtrauma.common.loot.CorpseEquipmentTransfer;
import com.swampd.superficialtrauma.common.treatment.TreatmentMovementRules;
import com.swampd.superficialtrauma.common.treatment.TreatmentProcedure;
import com.swampd.superficialtrauma.common.treatment.TreatmentType;
import com.swampd.superficialtrauma.common.wound.WoundCovering;
import com.swampd.superficialtrauma.common.wound.WoundInstance;
import com.swampd.superficialtrauma.common.wound.WoundTag;
import com.swampd.superficialtrauma.common.wound.WoundType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

public final class BodyStateRoundTripTest {
    private static final float EPSILON = 0.0001F;

    private BodyStateRoundTripTest() {
    }

    public static void main(String[] args) {
        verifyHalfOpenWoundRanges();
        verifyGunshotRangesAndContext();
        verifyGunshotCreationAndRoundTrip();
        verifyShotgunVolleyAggregation();
        verifyPendingDamageAccumulation();
        verifyIndependentDamageWindows();
        verifyWoundDefinitions();
        verifyTemporaryDressing();
        verifyCoveringVariants();
        verifyWoundPacking();
        verifyContextualTreatmentVisibility();
        verifyIcePackTreatment();
        verifyTourniquetAndNecrosis();
        verifyInfectionAndDebridement();
        verifyTemporaryDressingContamination();
        verifySystemicInfectionSettlement();
        verifyDebugInfectionSetter();
        verifySkillKnowledgeRoundTrip();
        verifyTreatmentMovementRules();
        verifyPainAccumulationAndTags();
        verifyStressAndPainRecovery();
        verifyTraumaticShockWarningAndCollapse();
        verifyShockWarningCancellationAndNbt();
        verifyLethalDamageIncapacitation();
        verifyDownedDamageCountdowns();
        verifyAssistedBreathing();
        verifyCprAndDefibrillation();
        verifyInfusionProgression();
        verifyTraumaticShockAwakeningAndRetryCooldown();
        verifyHemorrhagicShockAwakeningRequirements();
        verifyDownedPostureClassification();
        verifyDownedGeometry();
        verifyCorpseSnapshotRoundTrip();
        verifyCorpseArmorUpgradeRules();
        verifyDownedPoseSnapshotAndReset();
        verifyPainTagFloorAndClamp();
        verifyPainOfflinePauseAndNbt();
        verifyTransientSharpPain();
        verifyBleedingSchedulesAndStacking();
        verifyMovementBleeding();
        verifyBleedingOfflinePauseAndNbt();
        verifyNaturalHealingRates();
        verifyTimedWoundProgression();
        verifyWoundHistoryLifecycleAndLimit();
        verifyDowningHitEvidence();
        verifyPendingWindowExpiry();
        verifyNbtRoundTrip();
        verifyNonBluntNbtRoundTrip();
        verifyCgmDamageTraceRoundTrip();
        verifyVersionOneMigrationDefaults();
        verifyVersionTwoProgressionMigrationDefaults();
        verifyVersionThreePainMigrationDefaults();
        verifyVersionFourBleedingMigrationDefaults();
        verifyVersionFiveShockMigrationDefaults();
        verifyVersionSixDownedMigrationDefaults();
        verifyVersionSevenPoseMigrationDefaults();
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

    private static void verifyGunshotRangesAndContext() {
        assertGunshotSeverity(WoundType.GUNSHOT_LOW_VELOCITY, 3.9999F, false, false, 0);
        assertGunshotSeverity(WoundType.GUNSHOT_LOW_VELOCITY, 4.0F, false, false, 1);
        assertGunshotSeverity(WoundType.GUNSHOT_LOW_VELOCITY, 5.9999F, false, false, 1);
        assertGunshotSeverity(WoundType.GUNSHOT_LOW_VELOCITY, 6.0F, false, false, 2);
        assertGunshotSeverity(WoundType.GUNSHOT_LOW_VELOCITY, 15.0F, false, false, 2);
        assertGunshotSeverity(WoundType.GUNSHOT_LOW_VELOCITY, 15.0F, true, false, 3);

        assertGunshotSeverity(WoundType.GUNSHOT_HIGH_VELOCITY, 4.0F, false, false, 1);
        assertGunshotSeverity(WoundType.GUNSHOT_HIGH_VELOCITY, 9.9999F, false, false, 1);
        assertGunshotSeverity(WoundType.GUNSHOT_HIGH_VELOCITY, 10.0F, false, false, 2);
        assertGunshotSeverity(WoundType.GUNSHOT_HIGH_VELOCITY, 12.0F, false, false, 2);
        assertGunshotSeverity(WoundType.GUNSHOT_HIGH_VELOCITY, 12.0F, true, false, 3);

        assertGunshotSeverity(WoundType.GUNSHOT_SHOTGUN, 4.0F, false, false, 1);
        assertGunshotSeverity(WoundType.GUNSHOT_SHOTGUN, 7.9999F, false, true, 1);
        assertGunshotSeverity(WoundType.GUNSHOT_SHOTGUN, 8.0F, false, false, 2);
        assertGunshotSeverity(WoundType.GUNSHOT_SHOTGUN, 8.0F, false, true, 3);
    }

    private static void verifyGunshotCreationAndRoundTrip() {
        BodyState state = new BodyState();
        WoundUpdateResult converted = state.applyGunshotDamage(
                WoundType.GUNSHOT_LOW_VELOCITY,
                3.0F,
                20,
                10.0D,
                true,
                0L
        );
        assertEquals(WoundType.BLUNT, requireWound(converted).type(), "D below four must convert to blunt trauma");

        WoundUpdateResult lowVelocity = state.applyGunshotDamage(
                WoundType.GUNSHOT_LOW_VELOCITY,
                4.0F,
                0,
                10.0D,
                true,
                1L
        );
        WoundInstance lowWound = requireWound(lowVelocity);
        assertEquals(1, lowWound.severity(), "four low-velocity damage must create severity one");
        assertEquals(true, lowWound.woundTags().contains(WoundTag.NEEDS_DEBRIDEMENT_1), "creation roll must add debridement");
        assertFloatEquals(0.4F, lowWound.baseHealingPerSecond(), "low-velocity severity one must heal at 0.4 H/s");

        state.applyGunshotDamage(
                WoundType.GUNSHOT_LOW_VELOCITY,
                11.0F,
                11,
                10.0D,
                false,
                2L
        );
        assertEquals(3, lowWound.severity(), "A fifteen with V above ten must upgrade to severity three");
        assertEquals(true, lowWound.fragmentationEligible(), "armor-qualified context must persist on the wound");
        assertEquals(true, lowWound.woundTags().contains(WoundTag.DISORIENTATION_2), "severity three must add disorientation two");
        assertEquals(true, lowWound.woundTags().contains(WoundTag.NEEDS_DEBRIDEMENT_1), "severity upgrades must retain a successful debridement roll");

        WoundUpdateResult closeShotgun = state.applyGunshotDamage(
                WoundType.GUNSHOT_SHOTGUN,
                8.0F,
                0,
                3.0D,
                false,
                3L
        );
        WoundInstance shotgunWound = requireWound(closeShotgun);
        assertEquals(3, shotgunWound.severity(), "L equal to three must count as a close-range shotgun wound");
        assertEquals(true, shotgunWound.closeRangeShot(), "close-range context must be recorded");
        assertEquals(true, shotgunWound.woundTags().contains(WoundTag.DISORIENTATION_3), "close shotgun severity three must add disorientation three");

        BodyState restored = new BodyState();
        restored.deserializeNBT(state.serializeNBT());
        WoundInstance restoredLow = restored.wounds().stream()
                .filter(wound -> wound.type() == WoundType.GUNSHOT_LOW_VELOCITY)
                .findFirst()
                .orElseThrow(() -> new AssertionError("NBT must preserve low-velocity wounds"));
        WoundInstance restoredShotgun = restored.wounds().stream()
                .filter(wound -> wound.type() == WoundType.GUNSHOT_SHOTGUN)
                .findFirst()
                .orElseThrow(() -> new AssertionError("NBT must preserve shotgun wounds"));
        assertEquals(true, restoredLow.fragmentationEligible(), "NBT must preserve armor-qualified context");
        assertEquals(true, restoredLow.woundTags().contains(WoundTag.NEEDS_DEBRIDEMENT_1), "NBT must preserve the debridement roll");
        assertEquals(true, restoredShotgun.closeRangeShot(), "NBT must preserve close-range context");
    }

    private static void assertGunshotSeverity(
            WoundType type,
            float damage,
            boolean fragmentationEligible,
            boolean closeRangeShot,
            int expected
    ) {
        assertEquals(
                expected,
                WoundInstance.gunshotSeverityFor(type, damage, fragmentationEligible, closeRangeShot),
                type + " severity must follow the reviewed half-open range"
        );
    }

    private static void verifyShotgunVolleyAggregation() {
        ShotgunVolleyAccumulator accumulator = new ShotgunVolleyAccumulator();
        UUID victimId = UUID.randomUUID();
        UUID shooterId = UUID.randomUUID();
        ShotgunVolleyAccumulator.VolleyKey mixedHitKey = new ShotgunVolleyAccumulator.VolleyKey(
                victimId,
                shooterId,
                "cgm:shell",
                "cgm:shotgun",
                98L
        );
        accumulator.addHit(mixedHitKey, 3.6F, 0, 5.0D, 100L, "cgm.bullet.executed");
        accumulator.addHit(mixedHitKey, 4.5F, 0, 5.0D, 100L, "cgm.bullet.killed");

        assertEquals(0, accumulator.drainReady(victimId, 101L).size(), "a volley must wait for two quiet ticks");
        var completedMixedHits = accumulator.drainReady(victimId, 102L);
        assertEquals(1, completedMixedHits.size(), "pellets from the same shot must resolve as one volley");
        var mixedVolley = completedMixedHits.get(0);
        assertFloatEquals(8.1F, mixedVolley.totalFinalDamage(), "mixed body and head pellets must sum final damage");
        assertEquals(2, mixedVolley.pelletHits(), "the volley must retain its pellet-hit count");

        BodyState mixedState = new BodyState();
        WoundUpdateResult mixedResult = mixedState.applyGunshotDamage(
                WoundType.GUNSHOT_SHOTGUN,
                mixedVolley.totalFinalDamage(),
                mixedVolley.maximumArmorValue(),
                mixedVolley.minimumAttackerDistance(),
                false,
                102L
        );
        assertEquals(WoundType.GUNSHOT_SHOTGUN, requireWound(mixedResult).type(), "a mixed volley must create only shotgun trauma");
        assertEquals(1, mixedState.wounds().size(), "a mixed volley must not also create a blunt wound");
        assertEquals(2, mixedState.wounds().get(0).severity(), "D total 8.1 beyond three blocks must create severity two");

        ShotgunVolleyAccumulator.VolleyKey bodyHitKey = new ShotgunVolleyAccumulator.VolleyKey(
                victimId,
                shooterId,
                "cgm:shell",
                "cgm:shotgun",
                148L
        );
        accumulator.addHit(bodyHitKey, 3.6F, 0, 6.0D, 150L, "cgm.bullet.executed");
        accumulator.addHit(bodyHitKey, 3.6F, 0, 6.0D, 150L, "cgm.bullet.executed");
        accumulator.addHit(bodyHitKey, 3.6F, 0, 6.0D, 150L, "cgm.bullet.executed");
        var bodyHitVolley = accumulator.drainReady(victimId, 152L).get(0);
        assertFloatEquals(10.8F, bodyHitVolley.totalFinalDamage(), "three ordinary pellets must combine before classification");
        BodyState bodyHitState = new BodyState();
        WoundUpdateResult bodyHitResult = bodyHitState.applyGunshotDamage(
                WoundType.GUNSHOT_SHOTGUN,
                bodyHitVolley.totalFinalDamage(),
                bodyHitVolley.maximumArmorValue(),
                bodyHitVolley.minimumAttackerDistance(),
                false,
                152L
        );
        assertEquals(WoundType.GUNSHOT_SHOTGUN, requireWound(bodyHitResult).type(), "several sub-four pellets must combine into shotgun trauma");
        assertEquals(1, bodyHitState.wounds().size(), "ordinary pellets in one volley must not create a blunt wound per pellet");

        ShotgunVolleyAccumulator.VolleyKey singlePelletKey = new ShotgunVolleyAccumulator.VolleyKey(
                victimId,
                shooterId,
                "cgm:shell",
                "cgm:shotgun",
                198L
        );
        accumulator.addHit(singlePelletKey, 3.6F, 0, 8.0D, 200L, "cgm.bullet.executed");
        var singlePelletVolley = accumulator.drainReady(victimId, 202L).get(0);
        BodyState singlePelletState = new BodyState();
        WoundUpdateResult singlePelletResult = singlePelletState.applyGunshotDamage(
                WoundType.GUNSHOT_SHOTGUN,
                singlePelletVolley.totalFinalDamage(),
                singlePelletVolley.maximumArmorValue(),
                singlePelletVolley.minimumAttackerDistance(),
                false,
                202L
        );
        assertEquals(WoundType.BLUNT, requireWound(singlePelletResult).type(), "one sub-four pellet must still become blunt trauma");

        ShotgunVolleyAccumulator.VolleyKey firstShot = new ShotgunVolleyAccumulator.VolleyKey(
                victimId,
                shooterId,
                "cgm:shell",
                "cgm:shotgun",
                300L
        );
        ShotgunVolleyAccumulator.VolleyKey secondShot = new ShotgunVolleyAccumulator.VolleyKey(
                victimId,
                shooterId,
                "cgm:shell",
                "cgm:shotgun",
                301L
        );
        accumulator.addHit(firstShot, 3.6F, 0, 6.0D, 302L, "cgm.bullet.executed");
        accumulator.addHit(secondShot, 3.6F, 0, 6.0D, 302L, "cgm.bullet.executed");
        assertEquals(2, accumulator.drainReady(victimId, 304L).size(), "different projectile spawn ticks must remain separate shots");

        UUID otherVictimId = UUID.randomUUID();
        ShotgunVolleyAccumulator.VolleyKey otherVictim = new ShotgunVolleyAccumulator.VolleyKey(
                otherVictimId,
                shooterId,
                "cgm:shell",
                "cgm:shotgun",
                400L
        );
        accumulator.addHit(otherVictim, 4.5F, 0, 5.0D, 401L, "cgm.bullet.killed");
        assertEquals(0, accumulator.clearVictim(victimId), "clearing one victim must not remove another victim's volley");
        assertEquals(1, accumulator.pendingVolleyCount(), "the other victim's volley must remain pending");
        assertEquals(1, accumulator.clearVictim(otherVictimId), "clearing the matching victim must remove its volley");
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

    private static void verifyAssistedBreathing() {
        BodyState state = new BodyState();
        assertEquals(true, state.incapacitate(CollapseReason.HEMORRHAGIC_SHOCK, 0L), "test patient must become downed");
        long initialDeadline = state.bloodOxygenDeadlineGameTime();
        assertEquals(true, state.advanceAssistedBreathing(20L, 0, 20L, true), "assisted breathing must apply while incapacitated");
        assertEquals(initialDeadline + 20L, state.bloodOxygenDeadlineGameTime(), "assisted breathing must pause natural oxygen loss");
        assertEquals(true, state.advanceAssistedBreathing(40L, 1, 60L, true), "three held seconds must grant an oxygen pulse");
        assertEquals(initialDeadline + 240L, state.bloodOxygenDeadlineGameTime(), "one oxygen point must add nine seconds after pausing three seconds");
        assertFloatEquals(21.0F, state.bloodOxygen(), "one completed assisted-breathing pulse must restore one oxygen point");
    }

    private static void verifyInfusionProgression() {
        BodyState blood = new BodyState();
        blood.incapacitate(CollapseReason.HEMORRHAGIC_SHOCK, 0L);
        assertEquals(true, blood.startInfusion(InfusionType.BLOOD_BAG, 0L), "blood infusion must start on a downed patient");
        assertEquals(false, blood.startInfusion(InfusionType.SALINE, 1L), "a second infusion must be rejected while one is active");
        assertFloatEquals(0.5F, blood.advanceInfusion(20L).healingAmount(), "blood must restore 0.5 health each second");

        BodyState restored = new BodyState();
        restored.deserializeNBT(blood.serializeNBT());
        assertEquals(InfusionType.BLOOD_BAG, restored.infusionType(), "active infusion type must survive NBT round trip");
        InfusionProgression remainder = restored.advanceInfusion(BodyState.INFUSION_DURATION_TICKS);
        assertFloatEquals(14.5F, remainder.healingAmount(), "the remaining twenty-nine blood pulses must total 14.5 health");
        assertEquals(true, remainder.completed(), "blood infusion must finish at thirty seconds");
        assertEquals(false, restored.hasActiveInfusion(), "a completed infusion must clear itself");

        BodyState saline = new BodyState();
        saline.incapacitate(CollapseReason.TRAUMATIC_SHOCK, 0L);
        saline.startInfusion(InfusionType.SALINE, 0L);
        assertFloatEquals(7.5F, saline.advanceInfusion(BodyState.INFUSION_DURATION_TICKS).healingAmount(), "thirty saline pulses must total 7.5 health");
    }

    private static void verifyTraumaticShockAwakeningAndRetryCooldown() {
        BodyState state = new BodyState();
        state.incapacitate(CollapseReason.TRAUMATIC_SHOCK, 0L);
        assertEquals(false, state.advanceAwakening(5.0F, 0L).changed(), "health equal to five must not begin awakening");
        assertEquals(true, state.advanceAwakening(5.1F, 1L).started(), "pain below twenty and health above five must begin awakening");

        state.applyDownedDamage(1.0F, 20L);
        assertEquals(BodyLifeState.INCAPACITATED, state.lifeState(), "damage during awakening must return the patient to incapacitated");
        assertEquals(BodyState.AWAKENING_DURATION_TICKS, state.awakeningRetryRemainingTicks(20L), "damage must impose a twenty-second awakening retry cooldown");
        UUID firstMedic = UUID.randomUUID();
        UUID secondMedic = UUID.randomUUID();
        assertEquals(true, state.recordResuscitationContributor(firstMedic, "MedicOne"), "the first successful medic must be recorded");
        assertEquals(false, state.recordResuscitationContributor(firstMedic, "RenamedMedic"), "one medic must only appear once per downing event");
        assertEquals(true, state.recordResuscitationContributor(secondMedic, "MedicTwo"), "a second successful medic must be recorded");

        BodyState restored = new BodyState();
        restored.deserializeNBT(state.serializeNBT());
        assertEquals(List.of("MedicOne", "MedicTwo"), restored.resuscitationContributorNames(), "medic order and names must survive NBT");
        assertEquals(1L, restored.awakeningRetryRemainingTicks(20L + BodyState.AWAKENING_DURATION_TICKS - 1L), "awakening retry cooldown must survive NBT");
        assertEquals(false, restored.advanceAwakening(6.0F, 20L + BodyState.AWAKENING_DURATION_TICKS - 1L).changed(), "awakening must remain blocked until the cooldown ends");
        long retryEnd = 20L + BodyState.AWAKENING_DURATION_TICKS;
        assertEquals(true, restored.advanceAwakening(6.0F, retryEnd).started(), "awakening requirements must be checked again when cooldown ends");
        long completedAt = retryEnd + BodyState.AWAKENING_DURATION_TICKS;
        assertEquals(true, restored.advanceAwakening(6.0F, completedAt).completed(), "twenty uninterrupted seconds must restore action");
        assertEquals(BodyLifeState.ACTIVE, restored.lifeState(), "completed awakening must restore the active state");
        assertEquals(CollapseReason.NONE, restored.collapseReason(), "completed awakening must clear collapse reason");
        assertEquals(List.of("MedicOne", "MedicTwo"), restored.resuscitationContributorNames(), "contributors must remain available for the recovery overlay");
        assertEquals(
                BodyState.AWAKENING_RECOVERY_DURATION_TICKS,
                restored.awakeningRecoveryRemainingTicks(completedAt),
                "successful awakening must begin the visual recovery plus one-second slowdown grace period"
        );

        BodyState recoveryRestored = new BodyState();
        recoveryRestored.deserializeNBT(restored.serializeNBT());
        long recoveryEnd = completedAt + BodyState.AWAKENING_RECOVERY_DURATION_TICKS;
        assertEquals(true, recoveryRestored.isAwakeningRecoveryActive(recoveryEnd - 1L), "recovery slowdown must survive NBT until its final tick");
        assertEquals(false, recoveryRestored.isAwakeningRecoveryActive(recoveryEnd), "recovery slowdown must end at its deadline");
        assertEquals(true, recoveryRestored.incapacitate(CollapseReason.TRAUMATIC_SHOCK, completedAt + 1L), "an active recovering player may be downed again");
        assertEquals(0L, recoveryRestored.awakeningRecoveryRemainingTicks(completedAt + 1L), "being downed again must clear post-awakening recovery");
        assertEquals(List.of(), recoveryRestored.resuscitationContributorNames(), "a new downing event must start with an empty contributor list");
    }

    private static void verifyHemorrhagicShockAwakeningRequirements() {
        BodyState state = new BodyState();
        WoundInstance wound = requireWound(state.applyDamage(WoundType.SHARP, 5.0F, 0L));
        state.incapacitate(CollapseReason.HEMORRHAGIC_SHOCK, 1L);
        assertEquals(false, state.advanceAwakening(11.0F, 2L).changed(), "an untreated bleeding wound must block hemorrhagic-shock awakening");
        assertEquals(true, state.applyWoundPacking(wound.id(), 3L), "packing must control the test wound's bleeding");
        assertEquals(true, state.advanceAwakening(11.0F, 4L).started(), "health above ten and all bleeding controlled must begin awakening");
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

    private static void verifyTemporaryDressing() {
        BodyState state = new BodyState();
        WoundInstance wound = requireWound(state.applyDamage(WoundType.SHARP, 5.0F, 0L));
        assertEquals(2, wound.bleedingLevel(false), "untreated sharp severity two must bleed at level two");
        assertFloatEquals(0.1F, wound.baseHealingPerSecond(), "untreated sharp severity two must retain its natural rate");

        state.advanceBodyProgression(0L);
        long revisionBeforeTreatment = state.revision();
        assertEquals(true, state.applyTemporaryDressing(wound.id(), 20L), "temporary dressing must apply once");
        assertEquals(true, state.revision() > revisionBeforeTreatment, "treatment must advance BodyState revision");
        assertEquals(true, wound.temporaryDressingApplied(), "wound must record its dressing");
        assertEquals(1, wound.bleedingLevel(false), "temporary dressing must reduce bleeding by one level");
        assertFloatEquals(1.0F, wound.baseHealingPerSecond(), "temporary dressing must raise healing to one H/s");
        assertEquals(false, state.applyTemporaryDressing(wound.id(), 21L), "the same dressing must not apply twice");

        float progressBeforeTick = wound.healingProgress();
        BodyProgressionResult progression = state.advanceBodyProgression(20L);
        assertEquals(1, progression.progressedWounds(), "a dressed wound must progress on the server tick");
        assertFloatEquals(
                progressBeforeTick - 1.0F,
                wound.healingProgress(),
                "temporary dressing must remove one H after one elapsed second"
        );

        BodyState restored = new BodyState();
        restored.deserializeNBT(state.serializeNBT());
        WoundInstance restoredWound = restored.wound(wound.id())
                .orElseThrow(() -> new AssertionError("treated wound must survive NBT round trip"));
        assertEquals(true, restoredWound.temporaryDressingApplied(), "NBT must preserve temporary dressing");
        assertEquals(1, restoredWound.bleedingLevel(false), "restored dressing must keep its bleeding reduction");

        CompoundTag legacyTag = state.serializeNBT();
        legacyTag.getList("Wounds", Tag.TAG_COMPOUND).getCompound(0).remove("Covering");
        BodyState legacyRestored = new BodyState();
        legacyRestored.deserializeNBT(legacyTag);
        WoundInstance legacyWound = legacyRestored.wound(wound.id())
                .orElseThrow(() -> new AssertionError("legacy treated wound must survive migration"));
        assertEquals(
                WoundCovering.TEMPORARY_DRESSING,
                legacyWound.covering(),
                "legacy TemporaryDressing data must migrate to the covering slot"
        );

        long revisionBeforeRemoval = restored.revision();
        assertEquals(true, restored.removeTemporaryDressing(wound.id(), 40L), "temporary dressing must be removable");
        assertEquals(true, restored.revision() > revisionBeforeRemoval, "removal must advance BodyState revision");
        assertEquals(false, restoredWound.temporaryDressingApplied(), "removal must clear the dressing state");
        assertEquals(2, restoredWound.bleedingLevel(false), "removal must restore untreated bleeding");
        assertFloatEquals(0.1F, restoredWound.baseHealingPerSecond(), "removal must restore natural healing");
        assertEquals(false, restored.removeTemporaryDressing(wound.id(), 41L), "the same dressing must not be removed twice");

        BodyState damagedState = new BodyState();
        damagedState.deserializeNBT(state.serializeNBT());
        WoundInstance damagedWound = damagedState.wound(wound.id())
                .orElseThrow(() -> new AssertionError("treated wound must be available for damage test"));
        damagedWound.addAccumulatedDamage(0.5F, 22L);
        assertEquals(false, damagedWound.temporaryDressingApplied(), "new wound damage must destroy a temporary dressing");
        assertEquals(2, damagedWound.bleedingLevel(false), "destroying the dressing must restore untreated bleeding");
    }

    private static void verifyTreatmentMovementRules() {
        Vec3 origin = Vec3.ZERO;
        Vec3 walkingPosition = new Vec3(2.0D, 0.0D, 0.0D);
        assertEquals(
                false,
                TreatmentMovementRules.interrupts(true, false, origin, walkingPosition, origin, walkingPosition),
                "ordinary walking must not interrupt self treatment"
        );
        assertEquals(
                true,
                TreatmentMovementRules.interrupts(true, true, origin, walkingPosition, origin, walkingPosition),
                "sprinting must interrupt self treatment"
        );
        assertEquals(
                false,
                TreatmentMovementRules.interrupts(true, false, origin, origin, origin, origin),
                "standing still must not interrupt self treatment"
        );
        assertEquals(
                true,
                TreatmentMovementRules.interrupts(false, false, origin, walkingPosition, origin, origin),
                "caregiver movement must interrupt treatment of another player"
        );
        assertEquals(
                true,
                TreatmentMovementRules.interrupts(false, false, origin, origin, origin, walkingPosition),
                "patient movement must interrupt treatment by another player"
        );
        assertEquals(
                false,
                TreatmentMovementRules.interrupts(false, false, origin, origin, origin, origin),
                "two stationary players must keep treatment active"
        );
    }

    private static void verifyCoveringVariants() {
        BodyState selfAdhesiveState = new BodyState();
        WoundInstance selfAdhesive = requireWound(selfAdhesiveState.applyDamage(WoundType.SHARP, 5.0F, 0L));
        assertEquals(
                true,
                selfAdhesiveState.applyCovering(selfAdhesive.id(), WoundCovering.SELF_ADHESIVE_BANDAGE, 20L),
                "self-adhesive bandage must apply as a single covering"
        );
        assertEquals(1, selfAdhesive.bleedingLevel(false), "self-adhesive bandage must reduce bleeding by one");
        assertEquals(
                false,
                selfAdhesiveState.applyCovering(selfAdhesive.id(), WoundCovering.BANDAGE_WITH_MEDICAL_TAPE, 21L),
                "a wound must reject a second simultaneous covering"
        );

        BodyState tapedState = new BodyState();
        WoundInstance taped = requireWound(tapedState.applyDamage(WoundType.SHARP, 5.0F, 0L));
        assertEquals(
                true,
                tapedState.applyCovering(taped.id(), WoundCovering.BANDAGE_WITH_MEDICAL_TAPE, 20L),
                "bandage and medical tape must apply as one covering"
        );
        assertEquals(0, taped.bleedingLevel(false), "bandage and medical tape must reduce bleeding by two");
        BodyState tapedRestored = new BodyState();
        tapedRestored.deserializeNBT(tapedState.serializeNBT());
        WoundInstance restoredTaped = tapedRestored.wound(taped.id())
                .orElseThrow(() -> new AssertionError("combined covering must survive NBT round trip"));
        assertEquals(
                WoundCovering.BANDAGE_WITH_MEDICAL_TAPE,
                restoredTaped.covering(),
                "combined covering identity must persist"
        );
        assertEquals(
                false,
                tapedRestored.removeCovering(taped.id(), WoundCovering.SELF_ADHESIVE_BANDAGE, 40L),
                "removal must reject the wrong covering identity"
        );
        assertEquals(
                true,
                tapedRestored.removeCovering(taped.id(), WoundCovering.BANDAGE_WITH_MEDICAL_TAPE, 40L),
                "combined covering must be removable from its bandage anchor"
        );
        assertEquals(2, restoredTaped.bleedingLevel(false), "removal must restore the original bleeding level");

        BodyState reinforcedState = new BodyState();
        WoundInstance reinforced = requireWound(reinforcedState.applyDamage(WoundType.SHARP, 5.0F, 0L));
        assertEquals(
                true,
                reinforcedState.applyCovering(
                        reinforced.id(),
                        WoundCovering.BANDAGE_WITH_SELF_ADHESIVE_BANDAGE,
                        20L
                ),
                "bandage and self-adhesive bandage must apply as one reinforced covering"
        );
        assertEquals(0, reinforced.bleedingLevel(false), "reinforced bandage must reduce bleeding by three with zero floor");
    }

    private static void verifyWoundPacking() {
        BodyState state = new BodyState();
        WoundInstance wound = requireWound(state.applyDamage(WoundType.SHARP, 15.0F, 0L));
        assertEquals(3, wound.bleedingLevel(false), "severe sharp trauma must begin with bleeding 3");
        assertEquals(
                true,
                state.applyCovering(wound.id(), WoundCovering.SELF_ADHESIVE_BANDAGE, 20L),
                "a covering must apply before wound packing"
        );
        assertEquals(2, wound.bleedingLevel(false), "self-adhesive bandage must reduce bleeding by one");
        assertEquals(true, state.applyWoundPacking(wound.id(), 21L), "medical gauze must pack a bleeding wound");
        assertEquals(true, wound.woundPackingApplied(), "wound packing state must be visible on the wound");
        assertEquals(
                WoundCovering.SELF_ADHESIVE_BANDAGE,
                wound.covering(),
                "packing must not replace the wound covering"
        );
        assertEquals(0, wound.bleedingLevel(false), "covering reduction and packing reduction must stack with a zero floor");
        assertEquals(-1L, wound.nextBleedingGameTime(), "fully controlled bleeding must stop its damage timer");
        assertEquals(false, state.applyWoundPacking(wound.id(), 22L), "a wound must reject duplicate packing");

        BodyState restoredState = new BodyState();
        restoredState.deserializeNBT(state.serializeNBT());
        WoundInstance restored = restoredState.wound(wound.id())
                .orElseThrow(() -> new AssertionError("packed wound must survive NBT round trip"));
        assertEquals(true, restored.woundPackingApplied(), "NBT must preserve wound packing");
        assertEquals(
                WoundCovering.SELF_ADHESIVE_BANDAGE,
                restored.covering(),
                "NBT must preserve packing and covering independently"
        );
        assertEquals(true, restoredState.removeWoundPacking(wound.id(), 40L), "wound packing must be removable");
        assertEquals(false, restored.woundPackingApplied(), "packing state must clear after removal");
        assertEquals(2, restored.bleedingLevel(false), "removing packing must retain the covering's reduction");
        assertEquals(180L, restored.nextBleedingGameTime(), "removing packing must schedule a fresh bleeding-2 interval");
        assertEquals(false, restoredState.removeWoundPacking(wound.id(), 41L), "removed packing must not be removable twice");

        assertEquals(true, restoredState.applyWoundPacking(wound.id(), 42L), "packing must be applicable again after removal");
        assertEquals(
                true,
                restoredState.removeCovering(wound.id(), WoundCovering.SELF_ADHESIVE_BANDAGE, 43L),
                "covering must remain independently removable while packing is present"
        );
        assertEquals(true, restored.woundPackingApplied(), "removing a covering must not remove wound packing");
        assertEquals(1, restored.bleedingLevel(false), "packing alone must reduce bleeding 3 to bleeding 1");
    }

    private static void verifyIcePackTreatment() {
        BodyState minorState = new BodyState();
        WoundInstance minor = requireWound(minorState.applyDamage(WoundType.BLUNT, 1.5F, 0L));
        assertEquals(false, minor.canApplyIcePack(), "severity-one blunt trauma must reject an ice pack");
        assertEquals(false, minorState.applyIcePack(minor.id()), "the body state must reject an ineligible ice pack");

        BodyState state = new BodyState();
        WoundInstance wound = requireWound(state.applyDamage(WoundType.BLUNT, 4.0F, 0L));
        assertEquals(true, wound.canApplyIcePack(), "severity-two blunt trauma must accept one ice pack");
        assertFloatEquals(5.0F, state.pain(), "untreated severity-two blunt trauma must include Pain 1");
        assertEquals(true, state.applyIcePack(wound.id()), "an eligible ice pack must apply");
        assertFloatEquals(10.0F, wound.healingProgress(), "an ice pack must remove exactly 90 H");
        assertEquals(false, wound.woundTags().contains(WoundTag.PAIN_1), "an ice pack must clear Pain 1");
        assertFloatEquals(4.0F, state.pain(), "clearing Pain 1 must immediately reduce effective pain by one");
        assertEquals(false, state.applyIcePack(wound.id()), "the same wound must not accept a second ice pack");

        BodyState restored = new BodyState();
        restored.deserializeNBT(state.serializeNBT());
        WoundInstance restoredWound = restored.wound(wound.id())
                .orElseThrow(() -> new AssertionError("an ice-pack-treated wound must survive save and reload"));
        assertFloatEquals(10.0F, restoredWound.healingProgress(), "ice-pack H reduction must survive save and reload");
        assertEquals(false, restoredWound.woundTags().contains(WoundTag.PAIN_1), "ice-pack pain relief must survive save and reload");

        state.applyDamage(WoundType.BLUNT, 9.0F, 10L);
        assertEquals(3, wound.severity(), "new damage in the same window must still upgrade an ice-packed wound");
        assertEquals(true, wound.woundTags().contains(WoundTag.PAIN_1), "a severity upgrade must restore the new wound's Pain 1");
        assertEquals(false, wound.canApplyIcePack(), "severity-three blunt trauma must not accept an ice pack");

        BodyState nearlyHealedState = new BodyState();
        WoundInstance nearlyHealed = requireWound(nearlyHealedState.applyDamage(WoundType.BLUNT, 4.0F, 0L));
        nearlyHealed.advanceNaturalHealing(190.0F);
        assertFloatEquals(5.0F, nearlyHealed.healingProgress(), "test setup must leave five H");
        assertEquals(true, nearlyHealedState.applyIcePack(nearlyHealed.id()), "an ice pack may finish a nearly healed eligible wound");
        assertEquals(true, nearlyHealedState.wounds().isEmpty(), "an ice pack that reaches zero H must remove the wound immediately");
    }

    private static void verifyContextualTreatmentVisibility() {
        WoundInstance lowVelocity = WoundInstance.createGunshot(
                WoundType.GUNSHOT_LOW_VELOCITY,
                4.0F,
                false,
                false,
                false,
                0L,
                400L
        );
        assertEquals(true, TreatmentProcedure.supportsType(lowVelocity, TreatmentType.BANDAGE), "a bleeding gunshot must offer bandaging");
        assertEquals(true, TreatmentProcedure.supportsType(lowVelocity, TreatmentType.MEDICAL_GAUZE), "a bleeding gunshot must offer wound packing");
        assertEquals(false, TreatmentProcedure.supportsType(lowVelocity, TreatmentType.ICE_PACK), "a low-velocity gunshot must hide ice packs");
        assertEquals(false, TreatmentProcedure.supportsType(lowVelocity, TreatmentType.TOURNIQUET), "a severity-one gunshot must hide tourniquets");
        assertEquals(false, TreatmentProcedure.supportsType(lowVelocity, TreatmentType.SURGICAL_KIT), "a clean gunshot must hide surgical tools");
        assertEquals(false, TreatmentProcedure.supportsType(lowVelocity, TreatmentType.SALINE_SOLUTION), "a clean gunshot must hide debridement saline");

        WoundInstance bluntTwo = WoundInstance.create(WoundType.BLUNT, 4.0F, 0L, 400L);
        for (TreatmentType type : TreatmentType.values()) {
            assertEquals(
                    type == TreatmentType.ICE_PACK,
                    TreatmentProcedure.supportsType(bluntTwo, type),
                    "severity-two blunt trauma must expose only the ice pack"
            );
        }

        WoundInstance explosionOne = WoundInstance.create(WoundType.EXPLOSION, 4.0F, 0L, 400L);
        assertEquals(true, TreatmentProcedure.supportsType(explosionOne, TreatmentType.TEMPORARY_DRESSING), "explosion dressings must stay visible because they accelerate healing");
        assertEquals(true, TreatmentProcedure.supportsType(explosionOne, TreatmentType.BANDAGE), "explosion bandages must stay visible because they accelerate healing");
        assertEquals(true, TreatmentProcedure.supportsType(explosionOne, TreatmentType.SURGICAL_KIT), "a debridement wound must offer the surgical kit");
        assertEquals(true, TreatmentProcedure.supportsType(explosionOne, TreatmentType.SALINE_SOLUTION), "a debridement wound must offer saline");
        assertEquals(false, TreatmentProcedure.supportsType(explosionOne, TreatmentType.MEDICAL_GAUZE), "a non-bleeding explosion wound must hide packing");
        assertEquals(false, TreatmentProcedure.supportsType(explosionOne, TreatmentType.ICE_PACK), "an explosion wound must hide ice packs");
        assertEquals(false, TreatmentProcedure.supportsType(explosionOne, TreatmentType.TOURNIQUET), "a non-bleeding explosion wound must hide tourniquets");
    }

    private static void verifyTourniquetAndNecrosis() {
        BodyState minorState = new BodyState();
        WoundInstance minor = requireWound(minorState.applyDamage(WoundType.SHARP, 0.5F, 0L));
        assertEquals(false, minor.canApplyTourniquet(), "severity-one wounds must reject a tourniquet");

        BodyState state = new BodyState();
        WoundInstance wound = requireWound(state.applyDamage(WoundType.SHARP, 5.0F, 0L));
        assertEquals(2, wound.untreatedBleedingLevel(true), "test wound must begin at bleeding 2");
        assertEquals(true, state.applyTourniquet(wound.id(), 0L), "a bleeding severity-two wound must accept a tourniquet");
        assertEquals(true, wound.tourniquetApplied(), "tourniquet state must be visible on the wound");
        assertEquals(0, wound.bleedingLevel(true), "an applied tourniquet must reduce bleeding by three levels");

        assertEquals(true, wound.advanceTourniquet(WoundInstance.TOURNIQUET_NECROSIS_ONE_TICKS), "tourniquet time must advance");
        assertEquals(1, wound.tourniquetNecrosisLevel(), "five accumulated minutes must create Necrosis 1");
        assertEquals(true, wound.woundTags().contains(WoundTag.NECROSIS_1), "Necrosis 1 must be stored as a wound tag");
        assertFloatEquals(2.0F, (float) state.necrosisMaximumHealthReduction(), "Necrosis 1 must reduce maximum health by two");

        assertEquals(true, wound.advanceTourniquet(WoundInstance.TOURNIQUET_NECROSIS_TWO_TICKS), "tourniquet time must reach ten minutes");
        assertEquals(2, wound.tourniquetNecrosisLevel(), "ten accumulated minutes must upgrade to Necrosis 2");
        assertEquals(false, wound.woundTags().contains(WoundTag.NECROSIS_1), "Necrosis 2 must replace Necrosis 1");
        assertEquals(true, wound.woundTags().contains(WoundTag.NECROSIS_2), "Necrosis 2 must be stored as a wound tag");
        assertFloatEquals(4.0F, (float) state.necrosisMaximumHealthReduction(), "Necrosis 2 must reduce maximum health by four");
        assertEquals(true, state.hasNecrosisSlowness(), "Necrosis 2 must request Slowness I");

        BodyState restored = new BodyState();
        restored.deserializeNBT(state.serializeNBT());
        WoundInstance restoredWound = restored.wound(wound.id())
                .orElseThrow(() -> new AssertionError("tourniquet wound must survive save and reload"));
        assertEquals(true, restoredWound.tourniquetApplied(), "applied tourniquet must survive save and reload");
        assertEquals(WoundInstance.TOURNIQUET_NECROSIS_TWO_TICKS, restoredWound.tourniquetAccumulatedTicks(), "accumulated tourniquet time must survive save and reload");
        assertEquals(2, restoredWound.tourniquetNecrosisLevel(), "tourniquet necrosis level must survive save and reload");

        long removedAt = WoundInstance.TOURNIQUET_NECROSIS_TWO_TICKS;
        assertEquals(true, restored.removeTourniquet(restoredWound.id(), removedAt), "an applied tourniquet must be removable");
        assertEquals(2, restoredWound.bleedingLevel(true), "removing a tourniquet must restore untreated bleeding");
        restoredWound.advanceTourniquet(removedAt + WoundInstance.TOURNIQUET_RECOVERY_DELAY_TICKS);
        assertEquals(WoundInstance.TOURNIQUET_NECROSIS_TWO_TICKS, restoredWound.tourniquetAccumulatedTicks(), "the first sixty seconds off must not reduce accumulated time");
        restoredWound.advanceTourniquet(removedAt + WoundInstance.TOURNIQUET_RECOVERY_DELAY_TICKS + 20L);
        assertEquals(WoundInstance.TOURNIQUET_NECROSIS_TWO_TICKS - 20L, restoredWound.tourniquetAccumulatedTicks(), "accumulated time must recover one-for-one after the delay");
        restoredWound.advanceTourniquet(removedAt + WoundInstance.NECROSIS_TWO_RECOVERY_TICKS);
        assertEquals(0, restoredWound.tourniquetNecrosisLevel(), "Necrosis 2 must clear after four uninterrupted minutes without a tourniquet");
        assertEquals(false, restored.hasNecrosisSlowness(), "clearing Necrosis 2 must remove its slowness request");

        assertEquals(true, restored.applyTourniquet(restoredWound.id(), removedAt + WoundInstance.NECROSIS_TWO_RECOVERY_TICKS), "retained accumulated time must permit reapplication");
        assertEquals(1, restoredWound.tourniquetNecrosisLevel(), "reapplying after partial recovery must immediately restore the retained Necrosis 1 level");
        long reappliedAt = removedAt + WoundInstance.NECROSIS_TWO_RECOVERY_TICKS;
        assertEquals(1, restored.setAppliedTourniquetSecondsForDebug(300L, reappliedAt), "debug setter must update every active tourniquet");
        assertEquals(1, restoredWound.tourniquetNecrosisLevel(), "debugging five minutes must select Necrosis 1");
        assertEquals(true, restored.removeTourniquet(restoredWound.id(), reappliedAt), "the Necrosis 1 test tourniquet must be removable");
        restoredWound.advanceTourniquet(reappliedAt + WoundInstance.NECROSIS_ONE_RECOVERY_TICKS - 1L);
        assertEquals(1, restoredWound.tourniquetNecrosisLevel(), "Necrosis 1 must remain until three full minutes off");
        restoredWound.advanceTourniquet(reappliedAt + WoundInstance.NECROSIS_ONE_RECOVERY_TICKS);
        assertEquals(0, restoredWound.tourniquetNecrosisLevel(), "Necrosis 1 must clear after three uninterrupted minutes off");
        assertEquals(true, restored.applyTourniquet(restoredWound.id(), reappliedAt + WoundInstance.NECROSIS_ONE_RECOVERY_TICKS), "tourniquet must remain reusable for debug assertions");
        assertEquals(1, restored.setAppliedTourniquetSecondsForDebug(299L, reappliedAt + WoundInstance.NECROSIS_ONE_RECOVERY_TICKS), "debug setter must accept a below-threshold value");
        assertEquals(0, restoredWound.tourniquetNecrosisLevel(), "debug setter must recompute necrosis from the requested test time");

        BodyState necrosisThreeState = new BodyState();
        WoundInstance necrosisThree = requireWound(necrosisThreeState.applyDamage(WoundType.EXPLOSION, 16.0F, 0L));
        assertFloatEquals(4.0F, (float) necrosisThreeState.necrosisMaximumHealthReduction(), "Necrosis 3 must reduce maximum health by four");
        assertEquals(true, necrosisThreeState.applyTourniquet(necrosisThree.id(), 0L), "a bleeding Necrosis 3 wound must still accept bleeding control");
        assertEquals(1, necrosisThreeState.setAppliedTourniquetSecondsForDebug(600L, 0L), "debug setter must update the Necrosis 3 wound's tourniquet clock");
        assertEquals(false, necrosisThree.woundTags().contains(WoundTag.NECROSIS_2), "Necrosis 3 must visually supersede lower tourniquet necrosis tags");
        assertFloatEquals(4.0F, (float) necrosisThreeState.necrosisMaximumHealthReduction(), "lower necrosis must not stack twice on the same Necrosis 3 wound");

        BodyState offlineState = new BodyState();
        WoundInstance offlineWound = requireWound(offlineState.applyDamage(WoundType.SHARP, 15.0F, 0L));
        assertEquals(true, offlineState.applyTourniquet(offlineWound.id(), 0L), "offline test wound must accept a tourniquet");
        offlineState.resumeBodyProgression(0L);
        offlineState.advanceBodyProgression(100L);
        assertEquals(100L, offlineWound.tourniquetAccumulatedTicks(), "online tourniquet time must accumulate from server time");
        offlineState.pauseBodyProgression(100L);
        offlineState.resumeBodyProgression(1_100L);
        offlineState.advanceBodyProgression(1_120L);
        assertEquals(120L, offlineWound.tourniquetAccumulatedTicks(), "one thousand offline ticks must not count toward tourniquet wear");
    }

    private static void verifyPainAccumulationAndTags() {
        BodyState state = new BodyState();
        state.applyDamage(WoundType.BURN, 0.25F, 100L);
        assertFloatEquals(0.25F, state.basePain(), "final damage D must add to base pain");
        assertFloatEquals(2.0F, state.woundPainContribution(), "pain 2 must contribute two points");
        assertFloatEquals(2.25F, state.pain(), "effective pain must combine base and wound-tag pain");
        assertEquals(500L, state.stressEndGameTime(), "traumatic damage must start twenty seconds of stress");

        state.applyDamage(WoundType.EXPLOSION, 4.0F, 200L);
        assertFloatEquals(4.25F, state.basePain(), "later final damage must accumulate base pain");
        assertFloatEquals(3.0F, state.woundPainContribution(), "pain tags from separate wounds must add together");
        assertFloatEquals(7.25F, state.pain(), "effective pain must include all current wound tags");
        assertEquals(600L, state.stressEndGameTime(), "later trauma must refresh stress to twenty seconds");
    }

    private static void verifyStressAndPainRecovery() {
        BodyState state = new BodyState();
        state.applyDamage(WoundType.SHARP, 5.0F, 0L);
        state.resumeBodyProgression(0L);

        state.advanceBodyProgression(429L);
        assertFloatEquals(5.0F, state.basePain(), "base pain must not recover during stress or its first 1.5-second wait");

        BodyProgressionResult firstRecovery = state.advanceBodyProgression(430L);
        assertFloatEquals(1.0F, firstRecovery.recoveredBasePain(), "the first recovery step must remove one base-pain point");
        assertFloatEquals(4.0F, state.basePain(), "base pain must decrease by one every 1.5 seconds");
        assertFloatEquals(5.0F, state.pain(), "pain 1 must remain as a one-point wound contribution");

        BodyProgressionResult delayedRecovery = state.advanceBodyProgression(490L);
        assertFloatEquals(2.0F, delayedRecovery.recoveredBasePain(), "delayed processing must catch up complete recovery intervals");
        assertFloatEquals(2.0F, state.basePain(), "two additional intervals must remove two points");
    }

    private static void verifyTraumaticShockWarningAndCollapse() {
        BodyState belowThreshold = new BodyState();
        belowThreshold.applyDamage(WoundType.SHARP, 15.0F, 0L);
        belowThreshold.resumeBodyProgression(0L);
        BodyProgressionResult safe = belowThreshold.advanceBodyProgression(400L);
        assertEquals(false, safe.shockWarningStarted(), "effective pain 19 must not start a shock warning");
        assertEquals(BodyLifeState.ACTIVE, belowThreshold.lifeState(), "pain below twenty must retain active movement");

        BodyState state = new BodyState();
        state.applyDamage(WoundType.SHARP, 16.0F, 0L);
        state.resumeBodyProgression(0L);

        BodyProgressionResult stressed = state.advanceBodyProgression(399L);
        assertEquals(false, stressed.shockWarningStarted(), "active stress must suppress traumatic shock");
        BodyProgressionResult warning = state.advanceBodyProgression(400L);
        assertEquals(true, warning.shockWarningStarted(), "effective pain twenty must start warning when stress ends");
        assertEquals(600L, state.shockWarningEndGameTime(), "shock warning must last exactly ten seconds");
        assertEquals(true, state.canAct(), "the warning period must not incapacitate the player early");

        state.advanceBodyProgression(599L);
        assertFloatEquals(16.0F, state.basePain(), "automatic pain recovery must pause throughout the warning");
        BodyProgressionResult collapse = state.advanceBodyProgression(600L);
        assertEquals(true, collapse.becameIncapacitated(), "pain still at twenty must incapacitate at the warning deadline");
        assertEquals(BodyLifeState.INCAPACITATED, state.lifeState(), "collapse must enter the incapacitated state");
        assertEquals(CollapseReason.TRAUMATIC_SHOCK, state.collapseReason(), "collapse reason must be traumatic shock");
        assertEquals(false, state.canAct(), "incapacitated players must not be allowed to act");
        assertFloatEquals(20.0F, state.bloodOxygen(), "first incapacitation must initialize blood oxygen to twenty");

        assertEquals(true, state.forceRecoverForDebug(), "the administrator recovery path must change an incapacitated state");
        assertEquals(BodyLifeState.ACTIVE, state.lifeState(), "administrator recovery must restore active state");
        assertEquals(CollapseReason.NONE, state.collapseReason(), "administrator recovery must clear collapse reason");
        assertFloatEquals(0.0F, state.basePain(), "administrator recovery must clear base pain for repeatable testing");
        assertFloatEquals(30.0F, state.bloodOxygen(), "administrator recovery must restore full debug blood oxygen");
    }

    private static void verifyLethalDamageIncapacitation() {
        assertEquals(false, DamageDowning.wouldBeFatal(20.0F, 19.999F), "sub-lethal damage must remain unchanged");
        assertEquals(true, DamageDowning.wouldBeFatal(20.0F, 20.0F), "damage equal to health must be lethal");
        assertFloatEquals(19.0F, DamageDowning.clampToPreserveLife(20.0F, 40.0F), "lethal damage must preserve one health");
        assertFloatEquals(0.0F, DamageDowning.clampToPreserveLife(0.5F, 5.0F), "the downing floor must never heal low health");

        BodyState state = new BodyState();
        assertEquals(true, state.incapacitate(CollapseReason.LETHAL_DAMAGE, 1_000L), "a lethal hit must incapacitate an active player");
        assertEquals(false, state.incapacitate(CollapseReason.TRAUMATIC_SHOCK, 1_001L), "an existing collapse reason must not be overwritten");
        assertEquals(BodyLifeState.INCAPACITATED, state.lifeState(), "lethal damage must enter the incapacitated state");
        assertEquals(CollapseReason.LETHAL_DAMAGE, state.collapseReason(), "lethal damage must be recorded as collapse reason");
        assertFloatEquals(20.0F, state.bloodOxygen(), "lethal damage must initialize the downed blood-oxygen reserve");
        assertEquals(4_600L, state.bloodOxygenDeadlineGameTime(), "twenty oxygen points must provide exactly 180 seconds");

        BodyState restored = new BodyState();
        restored.deserializeNBT(state.serializeNBT());
        assertEquals(BodyLifeState.INCAPACITATED, restored.lifeState(), "NBT must preserve lethal incapacitation");
        assertEquals(CollapseReason.LETHAL_DAMAGE, restored.collapseReason(), "NBT must preserve lethal collapse reason");
        assertFloatEquals(20.0F, restored.bloodOxygen(), "NBT must preserve downed blood oxygen");
        assertEquals(4_600L, restored.bloodOxygenDeadlineGameTime(), "NBT must preserve the oxygen deadline");
    }

    private static void verifyDownedDamageCountdowns() {
        BodyState state = new BodyState();
        state.incapacitate(CollapseReason.LETHAL_DAMAGE, 1_000L);

        DownedDamageResult firstHit = state.applyDownedDamage(2.5F, 1_000L);
        assertEquals(true, firstHit.applied(), "damage to an incapacitated player must be converted to danger time");
        assertEquals(500L, firstHit.shortenedTicks(), "2.5 damage must shorten the current timer by twenty-five seconds");
        assertEquals(3_100L, firstHit.remainingTicks(), "the shortened oxygen timer must retain 155 seconds");
        assertEquals(BodyLifeState.INCAPACITATED, firstHit.resultingState(), "a non-exhausting hit must not skip cardiac arrest");
        assertEquals(0, state.wounds().size(), "converted downed damage must not create a trauma instance");

        BodyProgressionResult arrest = state.advanceBodyProgression(4_100L);
        assertEquals(true, arrest.becameCardiacArrest(), "oxygen expiry must enter cardiac arrest");
        assertEquals(BodyLifeState.CARDIAC_ARREST, state.lifeState(), "oxygen expiry must update the life state");
        assertEquals(7_700L, state.brainDeathDeadlineGameTime(), "cardiac arrest must start a fresh 180-second brain-death deadline");
        assertEquals(true, state.cardiacArrestEventId().isPresent(), "cardiac arrest must establish a patient-bound event ID");

        DownedDamageResult arrestHit = state.applyDownedDamage(2.0F, 4_100L);
        assertEquals(400L, arrestHit.shortenedTicks(), "two damage in cardiac arrest must remove twenty seconds");
        assertEquals(3_200L, arrestHit.remainingTicks(), "the brain-death deadline must retain 160 seconds");
        assertEquals(BodyLifeState.CARDIAC_ARREST, arrestHit.resultingState(), "a non-exhausting arrest hit must retain cardiac arrest");

        BodyProgressionResult brainDeath = state.advanceBodyProgression(7_300L);
        assertEquals(true, brainDeath.becameBrainDead(), "the shortened brain-death deadline must be authoritative");
        assertEquals(BodyLifeState.BRAIN_DEAD, state.lifeState(), "deadline expiry must enter brain death");
    }

    private static void verifyDownedPoseSnapshotAndReset() {
        assertEquals(
                DownedFallDirection.FORWARD,
                DownedPoseCapture.classifyFallDirection(0.0F, new Vec3(0.0D, 0.0D, 1.0D)),
                "a world-south fall must be forward for body yaw zero"
        );
        assertEquals(
                DownedFallDirection.BACKWARD,
                DownedPoseCapture.classifyFallDirection(0.0F, new Vec3(0.0D, 0.0D, -1.0D)),
                "a world-north fall must be backward for body yaw zero"
        );
        assertEquals(
                DownedFallDirection.LEFT,
                DownedPoseCapture.classifyFallDirection(0.0F, new Vec3(1.0D, 0.0D, 0.0D)),
                "a world-east fall must be left for body yaw zero"
        );
        assertEquals(
                DownedFallDirection.RIGHT,
                DownedPoseCapture.classifyFallDirection(0.0F, new Vec3(-1.0D, 0.0D, 0.0D)),
                "a world-west fall must be right for body yaw zero"
        );

        BodyState original = new BodyState();
        original.applyDamage(WoundType.SHARP, 5.0F, 900L);
        original.incapacitate(CollapseReason.LETHAL_DAMAGE, 1_000L);
        assertEquals(
                true,
                original.captureDownedPose(new DownedPoseSnapshot(
                        1_000L,
                        725.0F,
                        DownedPosture.CROUCHING,
                        DownedFallDirection.LEFT
                )),
                "the first collapse must capture one pose snapshot"
        );
        assertEquals(
                false,
                original.captureDownedPose(new DownedPoseSnapshot(
                        1_001L,
                        90.0F,
                        DownedPosture.STANDING,
                        DownedFallDirection.RIGHT
                )),
                "later updates must not rotate an already downed body"
        );

        BodyState restored = new BodyState();
        restored.deserializeNBT(original.serializeNBT());
        DownedPoseSnapshot restoredPose = restored.downedPoseSnapshot()
                .orElseThrow(() -> new AssertionError("NBT must preserve a downed pose snapshot"));
        assertEquals(1_000L, restoredPose.downedGameTime(), "NBT must preserve the downing time");
        assertFloatEquals(5.0F, restoredPose.bodyYaw(), "NBT must wrap and preserve body yaw");
        assertEquals(DownedPosture.CROUCHING, restoredPose.posture(), "NBT must preserve collapse posture");
        assertEquals(DownedFallDirection.LEFT, restoredPose.fallDirection(), "NBT must preserve fall direction");

        restored.resetAllForDebug();
        assertEquals(BodyLifeState.ACTIVE, restored.lifeState(), "full debug reset must restore active state");
        assertEquals(0, restored.wounds().size(), "full debug reset must remove every wound");
        assertFloatEquals(0.0F, restored.pain(), "full debug reset must clear all pain sources");
        assertEquals(false, restored.downedPoseSnapshot().isPresent(), "full debug reset must clear the downed pose");
        assertEquals("none", restored.lastDamageType(), "full debug reset must clear damage diagnostics");
    }

    private static void verifyDownedPostureClassification() {
        assertEquals(
                DownedPosture.UNSAFE,
                DownedPoseCapture.classifyPosture(true, true, true, true, true),
                "unsafe movement states must take priority over every ordinary posture"
        );
        assertEquals(
                DownedPosture.CRAWLING,
                DownedPoseCapture.classifyPosture(false, true, true, false, false),
                "a forced one-block visual crawl must not depend on the swimming flag"
        );
        assertEquals(
                DownedPosture.SWIMMING,
                DownedPoseCapture.classifyPosture(false, false, true, true, false),
                "visual swimming must take priority over sprinting"
        );
        assertEquals(
                DownedPosture.SPRINTING,
                DownedPoseCapture.classifyPosture(false, false, false, true, true),
                "sprinting must take priority over crouching"
        );
        assertEquals(
                DownedPosture.CROUCHING,
                DownedPoseCapture.classifyPosture(false, false, false, false, true),
                "crouching must be retained when no higher-priority posture applies"
        );
        assertEquals(
                DownedPosture.STANDING,
                DownedPoseCapture.classifyPosture(false, false, false, false, false),
                "ordinary movement must fall back to standing"
        );
    }

    private static void verifyDownedGeometry() {
        Vec2 northSouth = DownedGeometry.horizontalHalfExtents(0.0F);
        assertFloatEquals(0.3F, northSouth.x, "north-south downed hitbox half-width");
        assertFloatEquals(0.9F, northSouth.y, "north-south downed hitbox half-length");

        Vec2 eastWest = DownedGeometry.horizontalHalfExtents(90.0F);
        assertFloatEquals(0.9F, eastWest.x, "east-west downed hitbox half-length");
        assertFloatEquals(0.3F, eastWest.y, "east-west downed hitbox half-width");

        Vec2 diagonal = DownedGeometry.horizontalHalfExtents(45.0F);
        float expectedDiagonalHalfExtent = (float) (Math.sqrt(0.5D) * 1.2D);
        assertFloatEquals(
                expectedDiagonalHalfExtent,
                diagonal.x,
                "diagonal downed hitbox X extent"
        );
        assertFloatEquals(
                expectedDiagonalHalfExtent,
                diagonal.y,
                "diagonal downed hitbox Z extent"
        );
    }

    private static void verifyCorpseSnapshotRoundTrip() {
        UUID ownerId = UUID.fromString("0e252da4-f73c-45cc-a8da-2bd305ec6fe8");
        CorpseSnapshot original = new CorpseSnapshot(
                ownerId,
                "SnapshotPlayer",
                "base64-texture-property",
                "signed-texture-property",
                640L,
                new DownedPoseSnapshot(
                        120L,
                        37.5F,
                        DownedPosture.CROUCHING,
                        DownedFallDirection.LEFT
                )
        );
        CompoundTag saved = original.save();
        assertEquals(
                CorpseSnapshot.CURRENT_DATA_VERSION,
                saved.getInt("DataVersion"),
                "corpse snapshots must carry an independent data version"
        );
        CorpseSnapshot restored = CorpseSnapshot.load(saved);

        assertEquals(ownerId, restored.ownerId(), "corpse NBT must preserve owner UUID");
        assertEquals("SnapshotPlayer", restored.ownerName(), "corpse NBT must preserve owner name");
        assertEquals(
                "base64-texture-property",
                restored.skinTextureValue(),
                "corpse NBT must preserve the skin texture independently of player presence"
        );
        assertEquals(
                "signed-texture-property",
                restored.skinTextureSignature(),
                "corpse NBT must preserve the skin texture signature"
        );
        assertEquals(640L, restored.deathGameTime(), "corpse NBT must preserve death time");
        assertEquals(original.downedPose(), restored.downedPose(), "corpse NBT must preserve downed pose");
        assertFloatEquals(
                -52.5F,
                DownedGeometry.groundYaw(restored.downedPose()),
                "corpse direction must remain derived from the captured left-fall snapshot"
        );
    }

    private static void verifyCorpseArmorUpgradeRules() {
        CorpseEquipmentTransfer.ArmorQuality ironChestplate =
                new CorpseEquipmentTransfer.ArmorQuality(6, 0.0F, 0.0F, 0, 1.0D);
        CorpseEquipmentTransfer.ArmorQuality diamondChestplate =
                new CorpseEquipmentTransfer.ArmorQuality(8, 2.0F, 0.0F, 0, 1.0D);
        CorpseEquipmentTransfer.ArmorQuality netheriteChestplate =
                new CorpseEquipmentTransfer.ArmorQuality(8, 3.0F, 0.1F, 0, 1.0D);
        CorpseEquipmentTransfer.ArmorQuality damagedDiamondChestplate =
                new CorpseEquipmentTransfer.ArmorQuality(8, 2.0F, 0.0F, 0, 0.25D);
        CorpseEquipmentTransfer.ArmorQuality enchantedDiamondChestplate =
                new CorpseEquipmentTransfer.ArmorQuality(8, 2.0F, 0.0F, 4, 1.0D);

        assertEquals(
                true,
                CorpseEquipmentTransfer.isHigherQuality(diamondChestplate, ironChestplate),
                "take-all must recognize diamond chest armor as an iron upgrade"
        );
        assertEquals(
                false,
                CorpseEquipmentTransfer.isHigherQuality(ironChestplate, diamondChestplate),
                "take-all must not replace stronger armor with weaker armor"
        );
        assertEquals(
                true,
                CorpseEquipmentTransfer.isHigherQuality(netheriteChestplate, diamondChestplate),
                "netherite toughness and knockback resistance must outrank diamond"
        );
        assertEquals(
                true,
                CorpseEquipmentTransfer.isHigherQuality(
                        enchantedDiamondChestplate,
                        damagedDiamondChestplate
                ),
                "equal armor tiers must prefer protection enchantments before remaining durability"
        );
    }

    private static void verifyShockWarningCancellationAndNbt() {
        BodyState state = new BodyState();
        state.applyDamage(WoundType.SHARP, 16.0F, 0L);
        state.resumeBodyProgression(0L);
        state.advanceBodyProgression(400L);

        BodyState warningRestored = new BodyState();
        warningRestored.deserializeNBT(state.serializeNBT());
        assertEquals(600L, warningRestored.shockWarningEndGameTime(), "NBT must preserve an active shock warning deadline");
        assertEquals(CollapseReason.NONE, warningRestored.collapseReason(), "a warning must not invent a collapse reason");

        warningRestored.applyDamage(WoundType.BLUNT, 1.0F, 450L);
        BodyProgressionResult cancelled = warningRestored.advanceBodyProgression(450L);
        assertEquals(true, cancelled.shockWarningCancelled(), "new stress must cancel the current shock warning");
        assertEquals(-1L, warningRestored.shockWarningEndGameTime(), "cancelled warning must clear its deadline");
        assertEquals(BodyLifeState.ACTIVE, warningRestored.lifeState(), "warning cancellation must retain active state");

        BodyState collapsed = new BodyState();
        collapsed.applyDamage(WoundType.SHARP, 16.0F, 0L);
        collapsed.resumeBodyProgression(0L);
        collapsed.advanceBodyProgression(400L);
        collapsed.advanceBodyProgression(600L);
        BodyState collapsedRestored = new BodyState();
        collapsedRestored.deserializeNBT(collapsed.serializeNBT());
        assertEquals(BodyLifeState.INCAPACITATED, collapsedRestored.lifeState(), "NBT must preserve incapacitation");
        assertEquals(CollapseReason.TRAUMATIC_SHOCK, collapsedRestored.collapseReason(), "NBT must preserve the collapse reason");
    }

    private static void verifyPainTagFloorAndClamp() {
        BodyState floor = new BodyState();
        floor.applyDamage(WoundType.SHARP, 15.0F, 0L);
        floor.resumeBodyProgression(0L);
        floor.advanceBodyProgression(850L);
        assertFloatEquals(0.0F, floor.basePain(), "base pain must be able to recover to zero");
        assertFloatEquals(4.0F, floor.woundPainContribution(), "pain 3 must contribute four points");
        assertFloatEquals(4.0F, floor.pain(), "pain 3 must form an effective-pain floor of four");

        BodyState capped = new BodyState();
        capped.applyDamage(WoundType.SHARP, 40.0F, 0L);
        assertFloatEquals(30.0F, capped.basePain(), "base pain must be capped at thirty");
        assertFloatEquals(30.0F, capped.pain(), "effective pain must be capped at thirty after tags");
    }

    private static void verifyPainOfflinePauseAndNbt() {
        BodyState original = new BodyState();
        original.applyDamage(WoundType.BURN, 0.25F, 0L);
        original.resumeBodyProgression(0L);
        original.pauseBodyProgression(100L);

        BodyState restored = new BodyState();
        restored.deserializeNBT(original.serializeNBT());
        assertFloatEquals(0.25F, restored.basePain(), "NBT must preserve base pain");
        assertEquals(400L, restored.stressEndGameTime(), "NBT must preserve the pre-pause stress deadline");
        assertEquals(100L, restored.progressionPausedAtGameTime(), "NBT must preserve the pause timestamp");

        restored.resumeBodyProgression(1_100L);
        assertEquals(1_400L, restored.stressEndGameTime(), "offline time must shift the stress deadline forward");
        assertEquals(1_430L, restored.nextPainRecoveryGameTime(), "offline time must shift pain recovery forward");
        restored.advanceBodyProgression(1_429L);
        assertFloatEquals(0.25F, restored.basePain(), "offline time must not grant pain recovery");
        BodyProgressionResult recovery = restored.advanceBodyProgression(1_430L);
        assertFloatEquals(0.25F, recovery.recoveredBasePain(), "fractional base pain must recover without becoming negative");
        assertFloatEquals(2.0F, restored.pain(), "the remaining pain-2 wound tag must keep effective pain at two");
    }

    private static void verifyTransientSharpPain() {
        BodyState temporary = new BodyState();
        temporary.applyDamage(WoundType.SHARP, 0.5F, 100L);
        temporary.resumeBodyProgression(100L);
        assertEquals(300L, temporary.wounds().get(0).transientPainEndGameTime(), "level-1 sharp pain must last ten seconds");
        assertFloatEquals(1.0F, temporary.woundPainContribution(), "fresh level-1 sharp wounds must carry pain 1");

        temporary.advanceBodyProgression(299L);
        assertFloatEquals(1.0F, temporary.woundPainContribution(), "transient sharp pain must use a half-open expiry interval");
        BodyProgressionResult expired = temporary.advanceBodyProgression(300L);
        assertEquals(1, expired.expiredTransientWoundTags(), "sharp pain 1 must expire at exactly ten seconds");
        assertFloatEquals(0.0F, temporary.woundPainContribution(), "expired transient pain must stop contributing");

        BodyState upgraded = new BodyState();
        upgraded.applyDamage(WoundType.SHARP, 0.5F, 0L);
        upgraded.applyDamage(WoundType.SHARP, 4.5F, 100L);
        upgraded.resumeBodyProgression(100L);
        assertEquals(2, upgraded.wounds().get(0).severity(), "window damage must upgrade the sharp wound to level 2");
        assertEquals(-1L, upgraded.wounds().get(0).transientPainEndGameTime(), "level-2 pain 1 must become persistent");
        upgraded.advanceBodyProgression(300L);
        assertFloatEquals(1.0F, upgraded.woundPainContribution(), "upgraded level-2 pain 1 must not expire with the old timer");

        BodyState paused = new BodyState();
        paused.applyDamage(WoundType.SHARP, 0.5F, 0L);
        paused.resumeBodyProgression(0L);
        paused.pauseBodyProgression(100L);
        paused.resumeBodyProgression(1_100L);
        assertEquals(1_200L, paused.wounds().get(0).transientPainEndGameTime(), "offline time must shift transient pain expiry");
        paused.advanceBodyProgression(1_199L);
        assertFloatEquals(1.0F, paused.woundPainContribution(), "offline time must not consume transient sharp pain");
    }

    private static void verifyBleedingSchedulesAndStacking() {
        BodyState severeSharp = new BodyState();
        severeSharp.applyDamage(WoundType.SHARP, 15.0F, 0L);
        severeSharp.resumeBodyProgression(0L);

        BodyProgressionResult beforeFirstPulse = severeSharp.advanceBodyProgression(99L);
        assertFloatEquals(0.0F, beforeFirstPulse.bleedingDamage(), "bleeding 3 must not pulse before five seconds");
        BodyProgressionResult firstPulse = severeSharp.advanceBodyProgression(100L);
        assertFloatEquals(1.0F, firstPulse.bleedingDamage(), "bleeding 3 must remove one health point every five seconds");
        BodyProgressionResult delayedPulses = severeSharp.advanceBodyProgression(300L);
        assertFloatEquals(2.0F, delayedPulses.bleedingDamage(), "delayed processing must catch up complete bleeding intervals");

        BodyState stacked = new BodyState();
        stacked.applyDamage(WoundType.SHARP, 5.0F, 0L);
        stacked.applyDamage(WoundType.EXPLOSION, 16.0F, 0L);
        stacked.resumeBodyProgression(0L);

        stacked.advanceBodyProgression(139L);
        BodyProgressionResult simultaneous = stacked.advanceBodyProgression(140L);
        assertFloatEquals(2.0F, simultaneous.bleedingDamage(), "separate bleeding-2 wounds must each contribute at the same deadline");
    }

    private static void verifyMovementBleeding() {
        BodyState state = new BodyState();
        state.applyDamage(WoundType.BLUNT, 13.0F, 0L);
        state.resumeBodyProgression(0L);

        state.advanceBodyProgression(0L, false);
        assertEquals(false, state.movementBleedingActive(), "level-3 blunt movement bleeding must start hidden");
        assertEquals(-1L, state.wounds().get(0).nextBleedingGameTime(), "inactive movement bleeding must not have a running timer");

        state.advanceBodyProgression(1L, true);
        assertEquals(true, state.movementBleedingActive(), "sprinting or jumping must activate movement bleeding");
        assertEquals(201L, state.wounds().get(0).nextBleedingGameTime(), "movement bleeding 1 must schedule a ten-second pulse");

        state.advanceBodyProgression(200L, true);
        BodyProgressionResult movementPulse = state.advanceBodyProgression(201L, false);
        assertFloatEquals(1.0F, movementPulse.bleedingDamage(), "continued movement must produce bleeding-1 damage");
        state.advanceBodyProgression(239L, false);
        assertEquals(true, state.movementBleedingActive(), "movement bleeding must linger for two seconds after movement stops");
        state.advanceBodyProgression(240L, false);
        assertEquals(false, state.movementBleedingActive(), "movement bleeding must stop at the half-open two-second deadline");
        assertEquals(-1L, state.wounds().get(0).nextBleedingGameTime(), "stopped movement bleeding must reset its timer");
    }

    private static void verifyBleedingOfflinePauseAndNbt() {
        BodyState original = new BodyState();
        original.applyDamage(WoundType.SHARP, 5.0F, 0L);
        original.resumeBodyProgression(0L);
        original.pauseBodyProgression(100L);

        BodyState restored = new BodyState();
        restored.deserializeNBT(original.serializeNBT());
        assertEquals(140L, restored.wounds().get(0).nextBleedingGameTime(), "NBT must preserve the bleeding deadline");

        restored.resumeBodyProgression(1_100L);
        assertEquals(1_140L, restored.wounds().get(0).nextBleedingGameTime(), "offline time must shift bleeding deadlines forward");
        restored.advanceBodyProgression(1_139L);
        BodyProgressionResult resumedPulse = restored.advanceBodyProgression(1_140L);
        assertFloatEquals(1.0F, resumedPulse.bleedingDamage(), "offline time must not grant free bleeding pulses");
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
        state.resumeBodyProgression(0L);

        BodyProgressionResult subSecond = state.advanceBodyProgression(19L);
        assertEquals(false, subSecond.changed(), "sub-second tick time must not alter H");
        assertFloatEquals(100.0F, state.wounds().get(0).healingProgress(), "H must remain unchanged before one second");

        BodyProgressionResult firstSecond = state.advanceBodyProgression(20L);
        assertEquals(true, firstSecond.changed(), "one full second must advance natural healing");
        assertEquals(1, firstSecond.progressedWounds(), "one wound must advance");
        assertFloatEquals(99.0F, state.wounds().get(0).healingProgress(), "level-1 blunt H must decrease by one per second");

        state.advanceBodyProgression(50L);
        assertFloatEquals(98.0F, state.wounds().get(0).healingProgress(), "only complete seconds must be processed");
        assertEquals(40L, state.lastWoundProgressionGameTime(), "partial tick remainder must be retained");
        state.advanceBodyProgression(60L);
        assertFloatEquals(97.0F, state.wounds().get(0).healingProgress(), "retained partial ticks must contribute later");

        state.pauseBodyProgression(60L);
        state.resumeBodyProgression(1_000L);
        BodyProgressionResult resumed = state.advanceBodyProgression(1_020L);
        assertEquals(true, resumed.changed(), "online progression must resume from the new baseline");
        assertFloatEquals(96.0F, state.wounds().get(0).healingProgress(), "offline elapsed time must not be applied");

        BodyState completed = new BodyState();
        completed.applyDamage(WoundType.BLUNT, 1.5F, 0L);
        completed.resumeBodyProgression(0L);
        BodyProgressionResult completion = completed.advanceBodyProgression(2_000L);
        assertEquals(1, completion.healedWounds(), "H reaching zero must remove the whole wound");
        assertEquals(0, completed.wounds().size(), "healed wound must no longer be stored");

        BodyState stalled = new BodyState();
        stalled.applyDamage(WoundType.SHARP, 15.0F, 0L);
        stalled.resumeBodyProgression(0L);
        BodyProgressionResult noProgress = stalled.advanceBodyProgression(2_000L);
        assertEquals(0, noProgress.progressedWounds(), "a non-self-healing wound must not advance H");
        assertFloatEquals(100.0F, stalled.wounds().get(0).healingProgress(), "non-self-healing H must remain at 100");
    }

    private static void verifyWoundHistoryLifecycleAndLimit() {
        BodyState updated = new BodyState();
        WoundInstance sharpWound = requireWound(updated.applyDamage(WoundType.SHARP, 5.0F, 0L));
        updated.applyDamage(WoundType.SHARP, 10.0F, 1L);
        assertEquals(1, updated.woundHistory().size(), "updating a wound must not duplicate its history entry");
        WoundHistoryEntry updatedHistory = updated.woundHistory().get(0);
        assertEquals(sharpWound.id(), updatedHistory.woundId(), "history must retain the wound UUID");
        assertEquals(3, updatedHistory.severity(), "history must refresh an upgraded wound severity");
        assertFloatEquals(15.0F, updatedHistory.accumulatedDamage(), "history must refresh accumulated damage");
        assertEquals(1L, updatedHistory.lastTraumaGameTime(), "history must record the latest trauma time");
        assertEquals(false, updatedHistory.healed(), "an active wound must not be marked healed");

        BodyState healed = new BodyState();
        UUID healedWoundId = requireWound(healed.applyDamage(WoundType.BLUNT, 1.5F, 0L)).id();
        healed.resumeBodyProgression(0L);
        healed.advanceBodyProgression(2_000L);
        assertEquals(0, healed.wounds().size(), "the test wound must leave the active wound list");
        assertEquals(1, healed.woundHistory().size(), "a healed wound must remain in forensic history");
        WoundHistoryEntry healedHistory = healed.woundHistory().get(0);
        assertEquals(healedWoundId, healedHistory.woundId(), "healed history must retain the original UUID");
        assertEquals(true, healedHistory.healed(), "natural healing must archive the wound as healed");
        assertEquals(2_000L, healedHistory.healedGameTime(), "healing time must use the server progression clock");

        BodyState restoredHealed = new BodyState();
        restoredHealed.deserializeNBT(healed.serializeNBT());
        assertEquals(1, restoredHealed.woundHistory().size(), "healed history must survive NBT round trip");
        assertEquals(true, restoredHealed.woundHistory().get(0).healed(), "healed status must survive NBT");

        BodyState limited = new BodyState();
        UUID[] woundIds = new UUID[7];
        for (int i = 0; i < woundIds.length; i++) {
            woundIds[i] = requireWound(limited.applyDefibrillatorShockBurn(i)).id();
        }
        assertEquals(BodyState.MAX_WOUND_HISTORY, limited.woundHistory().size(), "history must be capped at six wounds");
        assertEquals(woundIds[1], limited.woundHistory().get(0).woundId(), "the oldest record must be evicted first");
        assertEquals(woundIds[6], limited.woundHistory().get(5).woundId(), "the newest record must remain last");

        BodyState migrationSource = new BodyState();
        UUID migratedWoundId = requireWound(migrationSource.applyDamage(WoundType.EXPLOSION, 4.0F, 50L)).id();
        CompoundTag versionEighteen = migrationSource.serializeNBT();
        versionEighteen.putInt("DataVersion", 18);
        versionEighteen.remove("WoundHistory");
        BodyState migrated = new BodyState();
        migrated.deserializeNBT(versionEighteen);
        assertEquals(1, migrated.woundHistory().size(), "old active wounds must be backfilled into history");
        assertEquals(migratedWoundId, migrated.woundHistory().get(0).woundId(), "migration must preserve wound identity");
    }

    private static void verifyDowningHitEvidence() {
        BodyState state = new BodyState();
        DamageClassification rifleHit = DamageClassification.cgmProjectile(
                DamageKind.CGM_HIGH_VELOCITY,
                "cgm_projectile_ammo_tag",
                "cgm:projectile",
                "nzgexpansion:medium_bullet",
                "nzgexpansion:battle_rifle"
        );
        state.recordFinalDamage(14.0F, "cgm.bullet.killed", rifleHit, 18.5D, 100L);
        assertEquals(
                true,
                state.incapacitateFromLastDamage(CollapseReason.HEMORRHAGIC_SHOCK, 100L),
                "a lethal external hit must start a downed episode"
        );
        DowningHitRecord captured = state.downingHitRecord()
                .orElseThrow(() -> new AssertionError("the downing hit must be captured"));
        assertEquals("nzgexpansion:battle_rifle", captured.weaponId(), "the downing weapon must be frozen");
        assertEquals("nzgexpansion:medium_bullet", captured.ammoId(), "the downing ammo must be frozen");
        assertDoubleEquals(18.5D, captured.attackerDistance(), "the impact distance must be frozen");
        assertEquals(true, captured.isRanged(), "a CGM projectile must be marked as ranged evidence");

        DamageClassification laterHit = DamageClassification.cgmProjectile(
                DamageKind.CGM_SHOTGUN,
                "cgm_projectile_ammo_tag",
                "cgm:projectile",
                "cgm:shell",
                "cgm:shotgun"
        );
        state.recordFinalDamage(20.0F, "cgm.bullet.executed", laterHit, 2.0D, 101L);
        DowningHitRecord afterLaterDamage = state.downingHitRecord().orElseThrow();
        assertEquals(
                "nzgexpansion:battle_rifle",
                afterLaterDamage.weaponId(),
                "damage received while downed must not overwrite the downing weapon"
        );
        assertDoubleEquals(
                18.5D,
                afterLaterDamage.attackerDistance(),
                "damage received while downed must not overwrite the downing distance"
        );

        BodyState restored = new BodyState();
        restored.deserializeNBT(state.serializeNBT());
        DowningHitRecord restoredHit = restored.downingHitRecord()
                .orElseThrow(() -> new AssertionError("downing evidence must survive NBT round trip"));
        assertEquals("nzgexpansion:battle_rifle", restoredHit.weaponId(), "NBT must preserve the downing weapon");
        assertDoubleEquals(18.5D, restoredHit.attackerDistance(), "NBT must preserve downing distance");

        assertEquals(true, restored.forceRecoverForDebug(), "debug recovery must return the player to active state");
        assertEquals(true, restored.downingHitRecord().isEmpty(), "successful recovery must close the old downed episode");
        restored.recordFinalDamage(6.0F, "fall", DamageClassification.blunt("fall"), 200L);
        restored.incapacitateFromLastDamage(CollapseReason.HEMORRHAGIC_SHOCK, 200L);
        assertEquals(
                "fall",
                restored.downingHitRecord().orElseThrow().damageType(),
                "a later downed episode must capture its own hit"
        );

        BodyState nonDamageCollapse = new BodyState();
        nonDamageCollapse.incapacitate(CollapseReason.TRAUMATIC_SHOCK, 300L);
        assertEquals(
                true,
                nonDamageCollapse.downingHitRecord().isEmpty(),
                "a non-damage collapse must not invent a downing hit"
        );
    }

    private static void verifyPendingWindowExpiry() {
        BodyState state = new BodyState();
        state.applyDamage(WoundType.EXPLOSION, 3.0F, 0L);
        state.resumeBodyProgression(0L);

        BodyProgressionResult stillOpen = state.advanceBodyProgression(399L);
        assertEquals(false, stillOpen.changed(), "pending damage must remain visible before the half-open window end");
        assertEquals(1, state.damageWindows().size(), "pending window must remain stored before tick 400");

        BodyProgressionResult expired = state.advanceBodyProgression(400L);
        assertEquals(true, expired.changed(), "pending damage expiry must update BodyState");
        assertEquals(1, expired.expiredDamageWindows(), "exactly one pending window must expire");
        assertEquals(0, state.damageWindows().size(), "expired pending damage must be removed at the window end");
    }

    private static void verifyNbtRoundTrip() {
        BodyState original = new BodyState();
        original.recordFinalDamage(4.0F, "fall", DamageClassification.blunt("fall"), 200L);
        WoundUpdateResult created = original.applyBluntDamage(4.0F, 200L);
        original.resumeBodyProgression(200L);
        assertEquals(WoundUpdateResult.Status.CREATED, created.status(), "4 damage must create a severity-2 wound");
        UUID originalId = requireWound(created).id();

        CompoundTag serialized = original.serializeNBT();
        assertEquals(BodyState.CURRENT_DATA_VERSION, serialized.getInt("DataVersion"), "serialized data version must be current");
        assertFloatEquals(4.0F, serialized.getFloat("BasePain"), "version 5 NBT must store base pain separately");

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
        assertFloatEquals(4.0F, restored.basePain(), "round trip must preserve base pain");
        assertFloatEquals(5.0F, restored.pain(), "round trip must recompute effective pain from the restored wound tag");
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
        assertEquals(540L, restored.wounds().get(0).nextBleedingGameTime(), "NBT must preserve a sharp wound's bleeding timer");
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

    private static void verifyVersionThreePainMigrationDefaults() {
        CompoundTag versionThree = new CompoundTag();
        versionThree.putInt("DataVersion", 3);
        versionThree.putFloat("Pain", 7.0F);

        BodyState restored = new BodyState();
        restored.deserializeNBT(versionThree);

        assertFloatEquals(7.0F, restored.basePain(), "version 3 Pain must migrate to version 4 base pain");
        assertFloatEquals(7.0F, restored.pain(), "migrated pain without wounds must remain visible");
        assertEquals(-1L, restored.stressEndGameTime(), "version 3 data must not invent an active stress timer");
    }

    private static void verifyVersionFourBleedingMigrationDefaults() {
        BodyState current = new BodyState();
        current.applyDamage(WoundType.SHARP, 5.0F, 0L);
        CompoundTag versionFour = current.serializeNBT();
        versionFour.putInt("DataVersion", 4);
        ListTag wounds = versionFour.getList("Wounds", Tag.TAG_COMPOUND);
        wounds.getCompound(0).remove("NextBleedingGameTime");
        wounds.getCompound(0).remove("BleedingTimerLevel");

        BodyState restored = new BodyState();
        restored.deserializeNBT(versionFour);
        assertEquals(-1L, restored.wounds().get(0).nextBleedingGameTime(), "version 4 wounds must migrate without retroactive bleeding");

        BodyProgressionResult scheduled = restored.advanceBodyProgression(0L);
        assertFloatEquals(0.0F, scheduled.bleedingDamage(), "migration must schedule a fresh interval instead of dealing immediate damage");
        assertEquals(140L, restored.wounds().get(0).nextBleedingGameTime(), "migrated bleeding 2 must receive a seven-second deadline");
        restored.advanceBodyProgression(139L);
        BodyProgressionResult firstPulse = restored.advanceBodyProgression(140L);
        assertFloatEquals(1.0F, firstPulse.bleedingDamage(), "migrated bleeding must begin after its first full interval");
    }

    private static void verifyVersionFiveShockMigrationDefaults() {
        CompoundTag versionFive = new CompoundTag();
        versionFive.putInt("DataVersion", 5);

        BodyState restored = new BodyState();
        restored.deserializeNBT(versionFive);

        assertEquals(BodyLifeState.ACTIVE, restored.lifeState(), "version 5 data must migrate to active state by default");
        assertEquals(CollapseReason.NONE, restored.collapseReason(), "version 5 data must not invent a collapse reason");
        assertEquals(-1L, restored.shockWarningEndGameTime(), "version 5 data must not invent a shock warning");
    }

    private static void verifyVersionSixDownedMigrationDefaults() {
        CompoundTag versionSix = new CompoundTag();
        versionSix.putInt("DataVersion", 6);
        versionSix.putString("LifeState", "incapacitated");
        versionSix.putString("CollapseReason", "lethal_damage");
        versionSix.putFloat("BloodOxygen", 20.0F);

        BodyState restored = new BodyState();
        restored.deserializeNBT(versionSix);
        assertEquals(-1L, restored.bloodOxygenDeadlineGameTime(), "version 6 data must not invent an offline deadline");

        restored.advanceBodyProgression(500L);
        assertEquals(4_100L, restored.bloodOxygenDeadlineGameTime(), "the first online tick must give a migrated downed player a full oxygen timer");
        assertEquals(BodyLifeState.INCAPACITATED, restored.lifeState(), "migration must not immediately cause cardiac arrest");
    }

    private static void verifyVersionSevenPoseMigrationDefaults() {
        CompoundTag versionSeven = new CompoundTag();
        versionSeven.putInt("DataVersion", 7);
        versionSeven.putString("LifeState", "incapacitated");
        versionSeven.putString("CollapseReason", "lethal_damage");
        versionSeven.putFloat("BloodOxygen", 20.0F);

        BodyState restored = new BodyState();
        restored.deserializeNBT(versionSeven);
        assertEquals(
                false,
                restored.downedPoseSnapshot().isPresent(),
                "version 7 data must wait for the server to capture a safe migration pose"
        );
    }

    private static void verifyInfectionAndDebridement() {
        BodyState state = new BodyState();
        WoundInstance wound = requireWound(state.applyDamage(WoundType.EXPLOSION, 8.0F, 0L));
        assertEquals(true, wound.woundTags().contains(WoundTag.NEEDS_DEBRIDEMENT_1), "explosion severity two must require debridement");
        assertEquals(false, wound.isInfected(), "a new wound must not begin infected");

        state.advanceBodyProgression(0L);
        BodyProgressionResult beforeDebridementPulse = state.advanceBodyProgression(WoundInstance.INFECTION_SPREAD_INTERVAL_TICKS - 1L);
        assertFloatEquals(0.0F, beforeDebridementPulse.infectionChange(), "debridement infection must wait a full three minutes");
        BodyProgressionResult debridementPulse = state.advanceBodyProgression(WoundInstance.INFECTION_SPREAD_INTERVAL_TICKS);
        assertFloatEquals(0.5F, debridementPulse.infectionChange(), "needs-debridement must add 0.5 infection every three minutes");
        assertFloatEquals(0.5F, wound.infectionContribution(), "the first debridement pulse must be attributed to the wound");
        assertEquals(false, wound.isInfected(), "the wound infection tag must remain hidden below 1.5 contribution");

        BodyProgressionResult beforeOnset = state.advanceBodyProgression(WoundInstance.INFECTION_ONSET_DELAY_TICKS - 1L);
        assertFloatEquals(0.0F, beforeOnset.infectionChange(), "natural infection must not begin before its full five-minute delay");
        assertFloatEquals(0.5F, state.infection(), "the earlier debridement pulse must remain below systemic-settlement threshold");

        BodyProgressionResult onset = state.advanceBodyProgression(WoundInstance.INFECTION_ONSET_DELAY_TICKS);
        assertFloatEquals(1.0F, onset.infectionChange(), "an untreated wound must add one infection point after five minutes");
        assertFloatEquals(1.5F, state.infection(), "natural and debridement infection must accumulate on the whole-body value");
        assertFloatEquals(1.5F, wound.infectionContribution(), "the wound must retain its own cumulative infection contribution");
        assertEquals(true, wound.isInfected(), "the wound infection tag must appear at 1.5 contribution");

        assertEquals(true, state.applyWoundPacking(wound.id(), WoundInstance.INFECTION_ONSET_DELAY_TICKS), "a bleeding wound must allow packing before debridement");
        assertEquals(false, wound.canDebride(), "wound packing must block debridement");
        assertEquals(true, state.removeWoundPacking(wound.id(), WoundInstance.INFECTION_ONSET_DELAY_TICKS), "packing must be removable before debridement");
        assertEquals(true, state.debrideWound(wound.id()), "an uncovered unpacked wound must allow debridement");
        assertEquals(false, wound.woundTags().contains(WoundTag.NEEDS_DEBRIDEMENT_1), "debridement must clear the requirement tag");
        assertEquals(false, wound.isInfected(), "debridement must clear the wound infection tag");
        assertEquals(true, wound.isDebrided(), "debridement must leave a visible completion tag");

        BodyState restored = new BodyState();
        restored.deserializeNBT(state.serializeNBT());
        assertFloatEquals(1.5F, restored.infection(), "infection must survive save and reload");
        assertEquals(true, restored.wounds().get(0).isDebrided(), "debridement state must survive save and reload");
        assertEquals(false, restored.wounds().get(0).isInfected(), "a reloaded debrided wound must remain uninfected");
        assertFloatEquals(1.5F, restored.wounds().get(0).infectionContribution(), "per-wound infection contribution must survive save and reload");

        long firstSystemicSettlement = WoundInstance.INFECTION_ONSET_DELAY_TICKS
                + BodyState.INFECTION_SETTLEMENT_INTERVAL_TICKS;
        BodyProgressionResult afterDebridement = restored.advanceBodyProgression(firstSystemicSettlement, false, 20);
        assertFloatEquals(-1.0F, afterDebridement.infectionChange(), "adequate food must reduce systemic infection after debridement");
        assertFloatEquals(0.5F, restored.infection(), "debridement must stop wound growth while nutrition resolves existing infection");
    }

    private static void verifyTemporaryDressingContamination() {
        BodyState protectedState = new BodyState();
        WoundInstance protectedWound = requireWound(protectedState.applyDamage(WoundType.SHARP, 0.5F, 0L));
        assertEquals(true, protectedState.applyTemporaryDressing(protectedWound.id(), 0L), "a temporary dressing must apply to a light wound");
        assertFloatEquals(0.0F, protectedState.infection(), "severity one must be protected from temporary-dressing contamination");
        assertEquals(false, protectedWound.isInfected(), "a light wound must remain uninfected");

        BodyState contaminatedState = new BodyState();
        WoundInstance contaminatedWound = requireWound(contaminatedState.applyDamage(WoundType.SHARP, 5.0F, 0L));
        assertEquals(true, contaminatedState.applyTemporaryDressing(contaminatedWound.id(), 0L), "a temporary dressing must apply to a severity-two wound");
        assertFloatEquals(1.0F, contaminatedState.infection(), "temporary dressing contamination must immediately add one infection point");
        assertFloatEquals(1.0F, contaminatedWound.infectionContribution(), "temporary dressing contamination must be attributed to its wound");
        assertEquals(false, contaminatedWound.isInfected(), "one contamination point must remain below the visible tag threshold");
        assertEquals(false, contaminatedWound.canDebride(), "a covered wound must not allow debridement");

        assertEquals(true, contaminatedState.removeTemporaryDressing(contaminatedWound.id(), 20L), "the contaminated dressing must be removable");
        assertEquals(false, contaminatedWound.canDebride(), "a wound below the infection-label threshold must not yet offer debridement");
        assertEquals(true, contaminatedState.applyTemporaryDressing(contaminatedWound.id(), 40L), "the dressing may be applied again after removal");
        assertFloatEquals(2.0F, contaminatedState.infection(), "each temporary-dressing application must retain its documented infection cost");
        assertEquals(true, contaminatedWound.isInfected(), "cumulative wound contribution at or above 1.5 must reveal infection");
        assertEquals(true, contaminatedState.removeTemporaryDressing(contaminatedWound.id(), 60L), "the second dressing must be removable");
        assertEquals(true, contaminatedWound.canDebride(), "removing the covering must expose a visibly infected wound for debridement");
    }

    private static void verifySystemicInfectionSettlement() {
        BodyState wellFed = stateWithInfection(1.5F);
        wellFed.advanceBodyProgression(0L, false, 15);
        BodyProgressionResult recovery = wellFed.advanceBodyProgression(
                BodyState.INFECTION_SETTLEMENT_INTERVAL_TICKS,
                false,
                15
        );
        assertFloatEquals(-1.0F, recovery.infectionChange(), "food level 15 must reduce infection by one per minute");
        assertFloatEquals(0.5F, wellFed.infection(), "systemic recovery must stop at the documented 0.5 threshold");
        assertEquals(-1L, wellFed.nextInfectionSettlementGameTime(), "infection at 0.5 must stop the settlement timer");

        assertSystemicGrowth(4.0F, 4.5F, 0.5F, "infection in (0,5] must grow by 0.5 without enough food");
        assertSystemicGrowth(7.0F, 8.0F, 1.0F, "infection in (5,10] must grow by one without enough food");
        BodyState severe = assertSystemicGrowth(12.0F, 13.5F, 1.5F, "infection above 10 must grow by 1.5 without enough food");
        assertFloatEquals(0.5F, severe.vanillaHealingMultiplier(), "infection above 10 must halve vanilla healing");
        assertEquals(false, severe.hasInfectionNausea(), "infection at or below 17 must not cause persistent nausea");

        BodyState nausea = stateWithInfection(17.5F);
        assertEquals(true, nausea.hasInfectionNausea(), "infection above 17 must cause persistent nausea");

        BodyState septic = stateWithInfection(19.0F);
        septic.advanceBodyProgression(0L, false, 0);
        BodyProgressionResult sepsis = septic.advanceBodyProgression(
                BodyState.INFECTION_SETTLEMENT_INTERVAL_TICKS,
                false,
                0
        );
        assertFloatEquals(1.0F, sepsis.infectionChange(), "systemic infection growth must clamp at 20");
        assertFloatEquals(20.0F, septic.infection(), "systemic infection must be capped at 20");
        assertEquals(true, sepsis.becameIncapacitated(), "reaching 20 infection must incapacitate the player");
        assertEquals(BodyLifeState.INCAPACITATED, septic.lifeState(), "sepsis must enter the downed state");
        assertEquals(CollapseReason.SEPSIS, septic.collapseReason(), "sepsis must preserve its collapse reason");
    }

    private static void verifySkillKnowledgeRoundTrip() {
        BodyState state = new BodyState();
        assertEquals(false, state.hasFirstAidSkill(), "new body state must not know first aid");
        assertEquals(true, state.unlockFirstAidSkill(), "the first-aid skill book must unlock first aid once");
        assertEquals(false, state.unlockFirstAidSkill(), "reusing a first-aid skill book must not unlock twice");
        assertEquals(false, state.hasSurgerySkill(), "new body state must not know surgery");
        assertEquals(true, state.unlockSurgerySkill(), "the surgery skill book must unlock surgery once");
        assertEquals(false, state.unlockSurgerySkill(), "reusing a surgery skill book must not unlock twice");

        BodyState restored = new BodyState();
        restored.deserializeNBT(state.serializeNBT());
        assertEquals(true, restored.hasFirstAidSkill(), "first-aid skill must survive save and reload");
        assertEquals(true, restored.hasSurgerySkill(), "surgery skill must survive save and reload");

        BodyState deathClone = new BodyState();
        deathClone.copyPersistentKnowledgeFrom(restored);
        assertEquals(true, deathClone.hasFirstAidSkill(), "permanent first-aid knowledge must survive a death clone");
        assertEquals(true, deathClone.hasSurgerySkill(), "permanent surgery knowledge must survive a death clone");

        restored.resetAllForDebug();
        assertEquals(false, restored.hasFirstAidSkill(), "the full debug reset must clear learned first-aid skill");
        assertEquals(false, restored.hasSurgerySkill(), "the full debug reset must clear learned surgery skill");
    }

    private static void verifyDebugInfectionSetter() {
        BodyState state = new BodyState();
        assertFloatEquals(12.5F, state.setInfectionForDebug(12.5F, 400L), "debug setter must return the applied infection");
        assertFloatEquals(12.5F, state.infection(), "debug setter must update systemic infection");
        assertEquals(
                400L + BodyState.INFECTION_SETTLEMENT_INTERVAL_TICKS,
                state.nextInfectionSettlementGameTime(),
                "debug setter must restart the systemic infection timer"
        );

        BodyState restored = new BodyState();
        restored.deserializeNBT(state.serializeNBT());
        assertFloatEquals(12.5F, restored.infection(), "debug infection must survive save and reload");
        assertEquals(
                state.nextInfectionSettlementGameTime(),
                restored.nextInfectionSettlementGameTime(),
                "debug infection timer must survive save and reload"
        );

        assertFloatEquals(20.0F, state.setInfectionForDebug(25.0F, 800L), "debug infection must clamp at 20");
        assertEquals(-1L, state.nextInfectionSettlementGameTime(), "infection 20 must not schedule another settlement");
        assertFloatEquals(0.0F, state.setInfectionForDebug(-5.0F, 900L), "debug infection must clamp at zero");
        assertEquals(-1L, state.nextInfectionSettlementGameTime(), "infection zero must stop systemic settlement");
    }

    private static BodyState assertSystemicGrowth(
            float initial,
            float expected,
            float expectedChange,
            String message
    ) {
        BodyState state = stateWithInfection(initial);
        state.advanceBodyProgression(0L, false, 0);
        BodyProgressionResult result = state.advanceBodyProgression(
                BodyState.INFECTION_SETTLEMENT_INTERVAL_TICKS,
                false,
                0
        );
        assertFloatEquals(expectedChange, result.infectionChange(), message);
        assertFloatEquals(expected, state.infection(), message);
        return state;
    }

    private static BodyState stateWithInfection(float infection) {
        CompoundTag tag = new BodyState().serializeNBT();
        tag.putFloat("Infection", infection);
        BodyState state = new BodyState();
        state.deserializeNBT(tag);
        return state;
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

    private static void verifyCprAndDefibrillation() {
        double survival = 1.0D;
        double expectedSeconds = 0.0D;
        for (int second = 1; second <= 1_000 && survival > 0.0D; second++) {
            expectedSeconds += survival;
            survival *= 1.0D - BodyState.cprSuccessChance(second);
        }
        assertEquals(
                true,
                expectedSeconds > 29.9D && expectedSeconds < 30.1D,
                "the reviewed CPR curve must average about thirty seconds"
        );

        BodyState cprState = new BodyState();
        assertEquals(
                true,
                cprState.forceCardiacRhythmForDebug(BodyLifeState.CARDIAC_ARREST, 0L),
                "debug setup must enter cardiac arrest"
        );
        CprResult firstSecond = cprState.applyCprSecond(0.999D, 0.0D, 20L);
        assertEquals(CprResult.Status.CONTINUE, firstSecond.status(), "a failed CPR roll must continue");
        assertEquals(1, cprState.accumulatedCprSeconds(), "a completed CPR second must accumulate");
        BodyState persistedCpr = new BodyState();
        persistedCpr.deserializeNBT(cprState.serializeNBT());
        assertEquals(1, persistedCpr.accumulatedCprSeconds(), "CPR progress must survive save and reload");

        CprResult vfResult = persistedCpr.applyCprSecond(0.0D, 0.0D, 40L);
        assertEquals(
                CprResult.Status.VENTRICULAR_FIBRILLATION,
                vfResult.status(),
                "seventy-percent rhythm branch must enter ventricular fibrillation"
        );
        assertEquals(
                40L + BodyState.VENTRICULAR_FIBRILLATION_DURATION_TICKS,
                persistedCpr.ventricularFibrillationEndGameTime(),
                "VF must have its own sixty-second deadline"
        );

        DefibrillationResult unsafe = persistedCpr.applyDefibrillation(
                DefibrillationEnergy.J250,
                0.999D,
                60L
        );
        assertEquals(
                DefibrillationResult.Status.UNSAFE_FAILURE_BRAIN_DEATH,
                unsafe.status(),
                "failed high-energy shock without 150/200 escalation must cause brain death"
        );

        BodyState safeState = new BodyState();
        safeState.forceCardiacRhythmForDebug(BodyLifeState.VENTRICULAR_FIBRILLATION, 0L);
        assertEquals(
                DefibrillationResult.Status.FAILED,
                safeState.applyDefibrillation(DefibrillationEnergy.J150, 0.999D, 20L).status(),
                "failed 150 J must leave VF active"
        );
        assertEquals(
                DefibrillationResult.Status.FAILED,
                safeState.applyDefibrillation(DefibrillationEnergy.J200, 0.999D, 40L).status(),
                "failed 200 J must leave VF active"
        );
        assertEquals(
                DefibrillationResult.Status.FAILED,
                safeState.applyDefibrillation(DefibrillationEnergy.J250, 0.999D, 60L).status(),
                "high-energy failure must be safe after both escalation attempts"
        );
        assertEquals(
                DefibrillationResult.Status.RESTORED_CIRCULATION,
                safeState.applyDefibrillation(DefibrillationEnergy.J300, 0.0D, 80L).status(),
                "a successful shock must restore circulation"
        );
        assertEquals(BodyLifeState.INCAPACITATED, safeState.lifeState(), "shock success returns to downed care");
        assertFloatEquals(
                BodyState.POST_RESUSCITATION_BLOOD_OXYGEN,
                safeState.bloodOxygen(),
                "successful resuscitation must restore ten oxygen points"
        );

        BodyState vfTimeout = new BodyState();
        vfTimeout.forceCardiacRhythmForDebug(BodyLifeState.VENTRICULAR_FIBRILLATION, 0L);
        vfTimeout.applyDefibrillation(DefibrillationEnergy.J150, 0.999D, 20L);
        long originalBrainDeadline = vfTimeout.brainDeathDeadlineGameTime();
        vfTimeout.advanceBodyProgression(BodyState.VENTRICULAR_FIBRILLATION_DURATION_TICKS, false, 20);
        assertEquals(
                BodyLifeState.CARDIAC_ARREST,
                vfTimeout.lifeState(),
                "untreated VF must return to cardiac arrest after sixty seconds"
        );
        assertEquals(
                originalBrainDeadline,
                vfTimeout.brainDeathDeadlineGameTime(),
                "VF timeout must not restart the original brain-death deadline"
        );
        assertEquals(
                true,
                vfTimeout.hasAttemptedDefibrillation(DefibrillationEnergy.J150),
                "defibrillation history must persist for the same cardiac-arrest event"
        );

        BodyState stableCpr = new BodyState();
        stableCpr.forceCardiacRhythmForDebug(BodyLifeState.CARDIAC_ARREST, 0L);
        assertEquals(
                CprResult.Status.RESTORED_CIRCULATION,
                stableCpr.applyCprSecond(0.0D, 0.9D, 20L).status(),
                "the thirty-percent CPR branch must restore stable circulation"
        );

        BodyState bystanderBurn = new BodyState();
        bystanderBurn.applyDamage(WoundType.BURN, 15.0F, 0L);
        WoundInstance shockBurn = requireWound(bystanderBurn.applyDefibrillatorShockBurn(1L));
        assertEquals(2, shockBurn.severity(), "defibrillator contact must create an isolated severity-two burn");
        assertEquals(2, bystanderBurn.wounds().size(), "contact burn must not merge into an older burn wound");
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

    private static void assertDoubleEquals(double expected, double actual, String message) {
        if (Math.abs(expected - actual) > 0.0001D) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }
}
