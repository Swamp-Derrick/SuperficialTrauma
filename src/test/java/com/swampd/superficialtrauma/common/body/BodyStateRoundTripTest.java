package com.swampd.superficialtrauma.common.body;

import com.swampd.superficialtrauma.common.damage.DamageClassification;
import com.swampd.superficialtrauma.common.damage.DamageDowning;
import com.swampd.superficialtrauma.common.damage.DamageKind;
import com.swampd.superficialtrauma.common.damage.ShotgunVolleyAccumulator;
import com.swampd.superficialtrauma.common.entity.CorpseSnapshot;
import com.swampd.superficialtrauma.common.entity.EmptyCorpseLifecycle;
import com.swampd.superficialtrauma.common.forensics.AutopsyAction;
import com.swampd.superficialtrauma.common.forensics.AutopsyReport;
import com.swampd.superficialtrauma.common.config.CorpseServerConfig;
import com.swampd.superficialtrauma.common.loot.CorpseEquipmentTransfer;
import com.swampd.superficialtrauma.common.medication.MedicationType;
import com.swampd.superficialtrauma.common.qte.TimingQteResult;
import com.swampd.superficialtrauma.common.qte.TimingQteSnapshot;
import com.swampd.superficialtrauma.common.treatment.TreatmentMovementRules;
import com.swampd.superficialtrauma.common.treatment.TreatmentProcedure;
import com.swampd.superficialtrauma.common.treatment.TreatmentType;
import com.swampd.superficialtrauma.common.wound.WoundCovering;
import com.swampd.superficialtrauma.common.wound.WoundDisinfectant;
import com.swampd.superficialtrauma.common.wound.WoundInstance;
import com.swampd.superficialtrauma.common.wound.WoundTag;
import com.swampd.superficialtrauma.common.wound.WoundType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
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
        verifyPunctureAndFrostbiteDebridement();
        verifyCrushIcePackHealing();
        verifyTemporaryDressing();
        verifyCoveringVariants();
        verifyWoundPacking();
        verifyContextualTreatmentVisibility();
        verifyIcePackTreatment();
        verifyTourniquetAndNecrosis();
        verifyInfectionAndDebridement();
        verifyDisinfectionAndHealingFloor();
        verifyTemporaryDressingContamination();
        verifySystemicInfectionSettlement();
        verifyAntibioticInfectionControl();
        verifyDebugInfectionSetter();
        verifySkillKnowledgeRoundTrip();
        verifyTreatmentMovementRules();
        verifyPainAccumulationAndTags();
        verifyStressAndPainRecovery();
        verifyPainRecoveryCurveAndOpioids();
        verifyTraumaticShockMedicationProtection();
        verifyTraumaticShockWarningAndCollapse();
        verifyShockWarningCancellationAndNbt();
        verifyLethalDamageIncapacitation();
        verifyDownedDamageCountdowns();
        verifyAssistedBreathing();
        verifyCprAndDefibrillation();
        verifyInfusionProgression();
        verifyMedicationLayersAndOverdose();
        verifyNaloxoneAndMetoprolol();
        verifyEpinephrineEffects();
        verifyRespiratoryDistressAndHypoxia();
        verifyOrganophosphateToxicologyAndAntidotes();
        verifyCommonAwakeningGate();
        verifyGiveUpAndTotalCountdown();
        verifyAdministrativeKillBrainDeath();
        verifyStandardVitalSignRanges();
        verifyTraumaticShockAwakeningAndRetryCooldown();
        verifyHemorrhagicShockAwakeningRequirements();
        verifyDownedPostureClassification();
        verifyDownedGeometry();
        verifyCorpseSnapshotRoundTrip();
        verifyAutopsyReportPrivacy();
        verifyEmptyCorpseLifecycle();
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
        verifyTimingQteHalfOpenRanges();
        MedicalRevisionTest.run();
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

        assertEquals(0, WoundInstance.severityFor(WoundType.PUNCTURE, 3.9999F), "puncture below 4 must remain pending");
        assertEquals(1, WoundInstance.severityFor(WoundType.PUNCTURE, 4.0F), "4 must enter puncture severity 1");
        assertEquals(2, WoundInstance.severityFor(WoundType.PUNCTURE, 10.0F), "10 must enter puncture severity 2");
        assertEquals(3, WoundInstance.severityFor(WoundType.PUNCTURE, 15.0F), "15 must enter puncture severity 3");

        assertEquals(0, WoundInstance.severityFor(WoundType.CRUSH, 1.9999F), "crush below 2 must remain pending");
        assertEquals(1, WoundInstance.severityFor(WoundType.CRUSH, 2.0F), "2 must enter crush severity 1");
        assertEquals(2, WoundInstance.severityFor(WoundType.CRUSH, 6.0F), "6 must enter crush severity 2");
        assertEquals(3, WoundInstance.severityFor(WoundType.CRUSH, 12.0F), "12 must enter crush severity 3");

        assertEquals(0, WoundInstance.severityFor(WoundType.FROSTBITE, 5.9999F), "freeze damage below 6 must remain pending");
        assertEquals(1, WoundInstance.severityFor(WoundType.FROSTBITE, 6.0F), "6 must enter frostbite severity 1");
        assertEquals(2, WoundInstance.severityFor(WoundType.FROSTBITE, 12.0F), "12 must enter frostbite severity 2");
        assertEquals(3, WoundInstance.severityFor(WoundType.FROSTBITE, 16.0F), "16 must enter frostbite severity 3");
    }

    private static void verifyGunshotRangesAndContext() {
        assertGunshotSeverity(WoundType.GUNSHOT_LOW_VELOCITY, 0, false, false, 0);
        assertGunshotSeverity(WoundType.GUNSHOT_LOW_VELOCITY, 3.9999F, false, false, 1);
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
        assertEquals(WoundType.BLUNT, requireWound(converted).type(), "Stopped bullet must convert to blunt trauma");

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

        state.applyPenetratingGunshotDamage(
                WoundType.GUNSHOT_LOW_VELOCITY,
                11.0F,
                true,
                false,
                false,
                "none",
                2L
        );
        assertEquals(3, lowWound.severity(), "A fifteen with penetrating strong-armor context must upgrade to severity three");
        assertEquals(true, lowWound.fragmentationEligible(), "armor-qualified context must persist on the wound");
        assertEquals(false, lowWound.woundTags().contains(WoundTag.DISORIENTATION_2), "heart-rate state must not remain attached to a wound");
        assertEquals(2, state.heartRateLevel(), "severity-three low-velocity trauma must add two whole-body heart-rate levels");
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
        assertEquals(false, shotgunWound.woundTags().contains(WoundTag.DISORIENTATION_3), "shotgun heart-rate impact must not remain attached to a wound");
        assertEquals(3, state.heartRateLevel(), "close shotgun trauma must clamp the whole-body heart-rate level at three");

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

        BodyState downedBeforeResolution = new BodyState();
        assertEquals(
                true,
                downedBeforeResolution.incapacitate(CollapseReason.HEMORRHAGIC_SHOCK, 101L),
                "the regression patient must enter the downed state before volley resolution"
        );
        WoundUpdateResult delayedDowningVolley = downedBeforeResolution.applyGunshotDamage(
                WoundType.GUNSHOT_SHOTGUN,
                mixedVolley.totalFinalDamage(),
                mixedVolley.maximumArmorValue(),
                mixedVolley.minimumAttackerDistance(),
                false,
                102L
        );
        assertEquals(
                WoundType.GUNSHOT_SHOTGUN,
                requireWound(delayedDowningVolley).type(),
                "a volley queued while active must still create shotgun trauma after the lethal pellet downs the victim"
        );
        assertEquals(
                BodyLifeState.INCAPACITATED,
                downedBeforeResolution.lifeState(),
                "resolving the causal volley must not advance the downed countdown"
        );

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
        assertEquals(WoundType.GUNSHOT_SHOTGUN, requireWound(singlePelletResult).type(), "one penetrating sub-four pellet must create gunshot trauma");

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
        state.setRespiratoryDistressForDebug(10.0F, 0L);
        assertEquals(true, state.incapacitate(CollapseReason.HEMORRHAGIC_SHOCK, 0L), "test patient must become downed");
        long initialDeadline = state.bloodOxygenDeadlineGameTime();
        assertEquals(true, state.advanceAssistedBreathing(20L, 0, 20L, true), "assisted breathing must apply while incapacitated");
        assertEquals(initialDeadline + 20L, state.bloodOxygenDeadlineGameTime(), "assisted breathing must pause natural oxygen loss");
        assertFloatEquals(9.0F, state.respiratoryDistress(), "one second of assisted breathing must reduce base respiratory distress by one");
        assertEquals(true, state.advanceAssistedBreathing(40L, 1, 60L, true), "three held seconds must grant an oxygen pulse");
        assertEquals(initialDeadline + 240L, state.bloodOxygenDeadlineGameTime(), "one oxygen point must add nine seconds after pausing three seconds");
        assertFloatEquals(21.0F, state.bloodOxygen(), "one completed assisted-breathing pulse must restore one oxygen point");
        assertFloatEquals(7.0F, state.respiratoryDistress(), "three total seconds of assisted breathing must reduce base distress by three");

        BodyState zeroFloor = new BodyState();
        zeroFloor.incapacitate(CollapseReason.HEMORRHAGIC_SHOCK, 0L);
        zeroFloor.advanceAssistedBreathing(200L, 0, 200L, true);
        assertFloatEquals(0.0F, zeroFloor.baseRespiratoryDistress(), "assisted breathing must not create negative base distress");
        assertFloatEquals(0.0F, zeroFloor.respiratoryDistress(), "assisted breathing at zero must remain at zero");
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

        BodyState disconnected = new BodyState();
        disconnected.incapacitate(CollapseReason.HEMORRHAGIC_SHOCK, 0L);
        disconnected.startInfusion(InfusionType.BLOOD_BAG, 0L);
        assertEquals(true, disconnected.cancelInfusion(), "disconnect handling must be able to cancel an active infusion");
        assertEquals(false, disconnected.hasActiveInfusion(), "a cancelled infusion must not survive logout");
    }

    private static void verifyMedicationLayersAndOverdose() {
        assertEquals(
                MedicationType.PARACETAMOL,
                MedicationType.fromStoredName("removed_legacy_medicine"),
                "stored medication ids must retain the tolerant legacy fallback"
        );
        assertEquals(
                false,
                MedicationType.fromNetworkName("removed_legacy_medicine").isPresent(),
                "unknown network medication ids must be rejected instead of becoming paracetamol"
        );
        BodyState state = new BodyState();
        state.applyDamage(WoundType.SHARP, 5.0F, 0L);
        float untreatedPain = state.pain();

        assertEquals(true, state.applyMedication(MedicationType.PARACETAMOL, 100L), "paracetamol must create an active drug layer");
        assertFloatEquals(4.0F, state.bloodDrugConcentration(), "one paracetamol layer must add four concentration");
        assertFloatEquals(1.0F, state.medicationPainReduction(), "one paracetamol layer must reduce pain by one");
        assertFloatEquals(untreatedPain - 1.0F, state.pain(), "paracetamol must immediately reduce effective pain");

        assertEquals(true, state.applyMedication(MedicationType.MORPHINE, 101L), "morphine must create an independent active layer");
        assertFloatEquals(10.0F, state.bloodDrugConcentration(), "paracetamol and morphine concentration must stack");
        assertFloatEquals(3.0F, state.medicationPainReduction(), "paracetamol and morphine analgesia must stack");
        assertEquals(false, state.hasDrugNausea(), "concentration ten must stay below drug nausea threshold");

        state.applyMedication(MedicationType.MORPHINE, 102L);
        assertFloatEquals(16.0F, state.bloodDrugConcentration(), "a second morphine layer must stack independently");
        assertEquals(true, state.hasDrugNausea(), "concentration above fourteen must cause drug nausea");

        BodyState restored = new BodyState();
        restored.deserializeNBT(state.serializeNBT());
        assertEquals(1, restored.activeDoseCount(MedicationType.PARACETAMOL), "NBT must preserve the paracetamol layer");
        assertEquals(2, restored.activeDoseCount(MedicationType.MORPHINE), "NBT must preserve both morphine layers");
        assertFloatEquals(16.0F, restored.bloodDrugConcentration(), "NBT must restore concentration from active layers");

        restored.advanceBodyProgression(3_700L);
        assertEquals(0, restored.activeDoseCount(MedicationType.PARACETAMOL), "paracetamol must expire after three minutes");
        assertEquals(2, restored.activeDoseCount(MedicationType.MORPHINE), "later morphine doses must keep their independent deadlines");
        assertFloatEquals(12.0F, restored.bloodDrugConcentration(), "expired layers must remove their concentration");
        restored.advanceBodyProgression(3_702L);
        assertEquals(0, restored.activeDrugDoses().size(), "all drug layers must disappear at their own deadlines");
        assertFloatEquals(0.0F, restored.bloodDrugConcentration(), "all concentration must clear after the final dose expires");

        BodyState overdose = new BodyState();
        for (int index = 0; index < 5; index++) {
            overdose.applyMedication(MedicationType.PARACETAMOL, index);
        }
        assertFloatEquals(20.0F, overdose.bloodDrugConcentration(), "five paracetamol layers must reach the overdose threshold");
        assertEquals(BodyLifeState.INCAPACITATED, overdose.lifeState(), "concentration twenty must incapacitate an active patient");
        assertEquals(CollapseReason.OVERDOSE, overdose.collapseReason(), "drug collapse must retain the overdose reason");

        overdose.advanceBodyProgression(3_600L);
        assertFloatEquals(16.0F, overdose.bloodDrugConcentration(), "overdose recovery must not start while concentration remains above eight");
        assertEquals(false, overdose.advanceAwakening(1.0F, 3_600L).changed(), "concentration sixteen must not begin overdose awakening");
        overdose.advanceBodyProgression(3_602L);
        assertFloatEquals(8.0F, overdose.bloodDrugConcentration(), "independent dose metabolism must eventually reach the awakening threshold");
        assertEquals(true, overdose.advanceAwakening(1.0F, 3_602L).started(), "overdose awakening must begin at concentration eight");
    }

    private static void verifyNaloxoneAndMetoprolol() {
        BodyState mixedOverdose = new BodyState();
        mixedOverdose.applyMedication(MedicationType.MORPHINE, 0L);
        mixedOverdose.applyMedication(MedicationType.MORPHINE, 1L);
        mixedOverdose.applyMedication(MedicationType.PARACETAMOL, 2L);
        mixedOverdose.applyMedication(MedicationType.PARACETAMOL, 3L);
        assertFloatEquals(20.0F, mixedOverdose.bloodDrugConcentration(), "mixed opioid and non-opioid doses must stack into an overdose");
        assertEquals(true, mixedOverdose.applyMedication(MedicationType.NALOXONE, 4L), "naloxone must clear active opioid doses");
        assertEquals(0, mixedOverdose.activeDoseCount(MedicationType.MORPHINE), "naloxone must remove every morphine layer");
        assertEquals(2, mixedOverdose.activeDoseCount(MedicationType.PARACETAMOL), "naloxone must leave non-opioid medicine untouched");
        assertFloatEquals(8.0F, mixedOverdose.bloodDrugConcentration(), "naloxone must recalculate concentration from remaining non-opioid layers");
        assertFloatEquals(2.0F, mixedOverdose.medicationPainReduction(), "naloxone must remove morphine analgesia without cancelling paracetamol");
        assertEquals(true, mixedOverdose.advanceAwakening(1.0F, 4L).started(), "naloxone-reduced concentration eight must permit overdose awakening");

        BodyState nonOpioidOverdose = new BodyState();
        for (int index = 0; index < 5; index++) {
            nonOpioidOverdose.applyMedication(MedicationType.PARACETAMOL, index);
        }
        assertEquals(false, nonOpioidOverdose.applyMedication(MedicationType.NALOXONE, 5L), "naloxone must do nothing when no opioid layer exists");
        assertFloatEquals(20.0F, nonOpioidOverdose.bloodDrugConcentration(), "naloxone must not lower non-opioid overdose concentration");

        BodyState heartRate = new BodyState();
        WoundInstance shotgunWound = requireWound(heartRate.applyGunshotDamage(
                WoundType.GUNSHOT_SHOTGUN,
                8.0F,
                0,
                3.0D,
                false,
                0L
        ));
        assertEquals(3, heartRate.heartRateLevel(), "close shotgun trauma must create whole-body tachycardia three");
        assertEquals(true, heartRate.applyMedication(MedicationType.METOPROLOL, 1L), "metoprolol must create a five-minute active layer");
        assertEquals(1, heartRate.effectiveHeartRateLevel(), "one metoprolol layer must shift heart rate down by two levels");
        heartRate.applyMedication(MedicationType.METOPROLOL, 2L);
        assertEquals(-1, heartRate.effectiveHeartRateLevel(), "stacked metoprolol must be able to cross zero into bradycardia");

        BodyState restoredHeartRate = new BodyState();
        restoredHeartRate.deserializeNBT(heartRate.serializeNBT());
        assertEquals(3, restoredHeartRate.heartRateLevel(), "NBT must preserve intrinsic whole-body heart rate");
        assertEquals(-1, restoredHeartRate.effectiveHeartRateLevel(), "NBT must preserve medication-adjusted heart rate");

        BodyState recovery = new BodyState();
        recovery.setHeartRateLevelForDebug(-3, 0L);
        recovery.advanceBodyProgression(599L);
        assertEquals(-3, recovery.heartRateLevel(), "heart rate must wait thirty seconds before recovering");
        recovery.advanceBodyProgression(600L);
        assertEquals(-2, recovery.heartRateLevel(), "heart rate must recover one level after the first thirty seconds");
        recovery.advanceBodyProgression(1_200L);
        assertEquals(-1, recovery.heartRateLevel(), "heart rate must recover another level every thirty seconds");
        recovery.advanceBodyProgression(1_800L);
        assertEquals(0, recovery.heartRateLevel(), "heart rate must naturally return to zero");

        CompoundTag legacyTag = heartRate.serializeNBT();
        legacyTag.putInt("DataVersion", 22);
        ListTag legacyWounds = legacyTag.getList("Wounds", Tag.TAG_COMPOUND);
        ListTag legacyWoundTags = legacyWounds.getCompound(0).getList("WoundTags", Tag.TAG_STRING);
        legacyWoundTags.add(StringTag.valueOf("disorientation_3"));
        BodyState migratedHeartRate = new BodyState();
        migratedHeartRate.deserializeNBT(legacyTag);
        assertEquals(3, migratedHeartRate.heartRateLevel(), "v22 disorientation must migrate into whole-body heart rate");
        assertEquals(false, migratedHeartRate.wounds().get(0).woundTags().stream().anyMatch(tag -> tag.disorientationLevel() > 0), "migration must remove wound-local disorientation tags");
    }

    private static void verifyEpinephrineEffects() {
        BodyState awake = new BodyState();
        assertEquals(
                true,
                awake.applyMedication(MedicationType.EPINEPHRINE, 0L, false),
                "epinephrine must create an active three-minute dose"
        );
        assertFloatEquals(5.0F, awake.bloodDrugConcentration(), "each epinephrine layer must add five concentration");
        assertEquals(1, awake.effectiveHeartRateLevel(), "each epinephrine layer must add one tachycardia level");
        assertEquals(1, awake.activeEpinephrineSpeedDoseCount(), "an awake injection must retain its speed eligibility");

        BodyState downedDose = new BodyState();
        downedDose.incapacitate(CollapseReason.TRAUMATIC_SHOCK, 0L);
        assertEquals(
                true,
                downedDose.applyMedication(MedicationType.EPINEPHRINE, 0L, true),
                "a downed patient must accept epinephrine"
        );
        assertEquals(0, downedDose.activeEpinephrineSpeedDoseCount(), "a downed injection must never grant speed after awakening");
        BodyState restoredDose = new BodyState();
        restoredDose.deserializeNBT(downedDose.serializeNBT());
        assertEquals(0, restoredDose.activeEpinephrineSpeedDoseCount(), "NBT must preserve downed-dose speed exclusion");

        assertDoubleEquals(
                BodyState.cprSuccessChance(1) * 2.0D,
                BodyState.cprSuccessChance(1, 1),
                "one epinephrine layer must double CPR accumulation"
        );
        assertDoubleEquals(
                BodyState.cprSuccessChance(1) * 3.0D,
                BodyState.cprSuccessChance(1, 2),
                "two epinephrine layers must triple CPR accumulation"
        );

        BodyState defibrillation = new BodyState();
        defibrillation.forceCardiacRhythmForDebug(BodyLifeState.VENTRICULAR_FIBRILLATION, 0L);
        defibrillation.applyMedication(MedicationType.EPINEPHRINE, 0L, true);
        assertDoubleEquals(
                0.40D,
                defibrillation.defibrillationSuccessChance(DefibrillationEnergy.J150),
                "one epinephrine layer must add ten percentage points to defibrillation"
        );
        assertEquals(
                DefibrillationResult.Status.RESTORED_CIRCULATION,
                defibrillation.applyDefibrillation(DefibrillationEnergy.J150, 0.35D, 1L).status(),
                "the epinephrine bonus must participate in the actual defibrillation roll"
        );

        BodyState oneLayerCountdown = epinephrineCountdownState(1);
        oneLayerCountdown.advanceBodyProgression(60L);
        assertEquals(
                BodyState.CARDIAC_ARREST_DURATION_TICKS - 40L,
                oneLayerCountdown.downedDangerRemainingTicks(60L),
                "one layer must let only forty countdown ticks pass during sixty real ticks"
        );
        BodyState twoLayerCountdown = epinephrineCountdownState(2);
        twoLayerCountdown.advanceBodyProgression(60L);
        assertEquals(
                BodyState.CARDIAC_ARREST_DURATION_TICKS - 30L,
                twoLayerCountdown.downedDangerRemainingTicks(60L),
                "the second layer must add half the first layer's benefit"
        );
        BodyState threeLayerCountdown = epinephrineCountdownState(3);
        threeLayerCountdown.advanceBodyProgression(60L);
        assertEquals(
                BodyState.CARDIAC_ARREST_DURATION_TICKS - 30L,
                threeLayerCountdown.downedDangerRemainingTicks(60L),
                "the third layer must not add further countdown slowdown"
        );

        BodyState shock = new BodyState();
        shock.applyDamage(WoundType.SHARP, 16.0F, 0L);
        shock.resumeBodyProgression(0L);
        assertEquals(true, shock.advanceBodyProgression(400L).shockWarningStarted(), "pain twenty must first start a shock warning");
        shock.applyMedication(MedicationType.EPINEPHRINE, 401L, false);
        assertEquals(true, shock.advanceBodyProgression(402L).shockWarningCancelled(), "active epinephrine must cancel traumatic-shock countdowns");
        CompoundTag expiringDose = shock.serializeNBT();
        expiringDose.getList("ActiveDrugDoses", Tag.TAG_COMPOUND)
                .getCompound(0)
                .putLong("ExpiresGameTime", 403L);
        BodyState afterExpiry = new BodyState();
        afterExpiry.deserializeNBT(expiringDose);
        assertEquals(
                true,
                afterExpiry.advanceBodyProgression(403L).shockWarningStarted(),
                "traumatic-shock warning must restart from zero after epinephrine expires"
        );
        assertEquals(
                403L + BodyState.SHOCK_WARNING_DURATION_TICKS,
                afterExpiry.shockWarningEndGameTime(),
                "the restarted warning must receive a fresh ten seconds"
        );
    }

    private static void verifyRespiratoryDistressAndHypoxia() {
        BodyState thresholds = new BodyState();
        assertFloatEquals(0.0F, thresholds.respiratoryDistress(), "respiratory distress must begin at zero");
        thresholds.setRespiratoryDistressForDebug(5.0F, 0L);
        assertEquals(false, thresholds.hasVisibleRespiratoryDistress(), "distress equal to five must remain hidden");
        thresholds.setRespiratoryDistressForDebug(10.0F, 0L);
        assertEquals(true, thresholds.hasVisibleRespiratoryDistress(), "distress above five must appear in the wound column");
        assertEquals(-1, thresholds.respiratoryHeartRateShift(), "distress equal to ten must add one bradycardia layer");
        thresholds.setRespiratoryDistressForDebug(9.999F, 0L);
        assertEquals(0, thresholds.respiratoryHeartRateShift(), "distress below ten must not affect heart rate");

        BodyState naturalRecovery = new BodyState();
        naturalRecovery.resumeBodyProgression(0L);
        naturalRecovery.setRespiratoryDistressForDebug(10.0F, 0L);
        naturalRecovery.advanceBodyProgression(20L);
        assertFloatEquals(9.92F, naturalRecovery.respiratoryDistress(), "respiratory distress must naturally recover by 0.08 each second");

        BodyState epinephrineRecovery = new BodyState();
        epinephrineRecovery.resumeBodyProgression(0L);
        epinephrineRecovery.setRespiratoryDistressForDebug(10.0F, 0L);
        epinephrineRecovery.applyMedication(MedicationType.EPINEPHRINE, 0L, false);
        epinephrineRecovery.advanceBodyProgression(20L);
        assertFloatEquals(9.52F, epinephrineRecovery.respiratoryDistress(), "one epinephrine layer must increase recovery to 0.48 per second");
        epinephrineRecovery.applyMedication(MedicationType.EPINEPHRINE, 20L, false);
        epinephrineRecovery.advanceBodyProgression(40L);
        assertFloatEquals(8.64F, epinephrineRecovery.respiratoryDistress(), "two epinephrine layers must recover 0.88 per second");

        BodyState epinephrineFloor = new BodyState();
        epinephrineFloor.resumeBodyProgression(0L);
        epinephrineFloor.applyMedication(MedicationType.EPINEPHRINE, 0L, false);
        epinephrineFloor.advanceBodyProgression(20L);
        assertFloatEquals(0.0F, epinephrineFloor.baseRespiratoryDistress(), "epinephrine must not create negative base distress");

        BodyState opioidLayers = new BodyState();
        opioidLayers.applyMedication(MedicationType.MORPHINE, 0L);
        assertFloatEquals(2.0F, opioidLayers.respiratoryDistress(), "the first opioid layer must add two respiratory distress");
        opioidLayers.applyMedication(MedicationType.MORPHINE, 1L);
        assertFloatEquals(6.0F, opioidLayers.respiratoryDistress(), "the second opioid layer must add another four");
        opioidLayers.applyMedication(MedicationType.MORPHINE, 2L);
        assertFloatEquals(14.0F, opioidLayers.respiratoryDistress(), "the third opioid layer must add another eight");
        BodyState restoredOpioids = new BodyState();
        restoredOpioids.deserializeNBT(opioidLayers.serializeNBT());
        assertFloatEquals(14.0F, restoredOpioids.respiratoryDistress(), "opioid respiratory contribution must survive NBT through active doses");
        restoredOpioids.applyMedication(MedicationType.NALOXONE, 3L);
        assertFloatEquals(0.0F, restoredOpioids.respiratoryDistress(), "naloxone must remove every dynamic opioid respiratory contribution");

        BodyState opioidRecoveryIsolation = new BodyState();
        opioidRecoveryIsolation.applyMedication(MedicationType.MORPHINE, 0L, false);
        opioidRecoveryIsolation.resumeBodyProgression(0L);
        opioidRecoveryIsolation.advanceBodyProgression(20L);
        assertFloatEquals(0.0F, opioidRecoveryIsolation.baseRespiratoryDistress(), "natural recovery must not create negative base distress to offset opioids");
        assertFloatEquals(2.0F, opioidRecoveryIsolation.respiratoryDistress(), "an opioid respiratory modifier must remain intact while its dose is active");
        opioidRecoveryIsolation.applyMedication(MedicationType.NALOXONE, 21L, false);
        assertFloatEquals(0.0F, opioidRecoveryIsolation.respiratoryDistress(), "naloxone must remove opioids without revealing negative respiratory debt");

        BodyState atropineModifierIsolation = new BodyState();
        atropineModifierIsolation.applyMedication(MedicationType.ATROPINE_SULFATE, 0L, false);
        atropineModifierIsolation.applyMedication(MedicationType.ATROPINE_SULFATE, 1L, false);
        atropineModifierIsolation.resumeBodyProgression(1L);
        atropineModifierIsolation.advanceBodyProgression(21L);
        assertFloatEquals(0.0F, atropineModifierIsolation.baseRespiratoryDistress(), "atropine must not create positive base compensation");
        assertFloatEquals(-1.0F, atropineModifierIsolation.respiratoryDistress(), "two atropine layers must retain their temporary negative modifier");
        atropineModifierIsolation.advanceBodyProgression(3_601L);
        assertFloatEquals(0.0F, atropineModifierIsolation.baseRespiratoryDistress(), "atropine expiry must not leave positive base respiratory debt");
        assertFloatEquals(0.0F, atropineModifierIsolation.respiratoryDistress(), "atropine expiry must remove its negative modifier cleanly");

        assertEquals(1, MedicationType.MORPHINE.opioidEquivalentLayers(), "one morphine dose must retain one opioid-equivalent layer");
        assertEquals(3, MedicationType.REMIFENTANIL.opioidEquivalentLayers(), "one remifentanil dose must contribute three opioid-equivalent layers");
        BodyState remifentanil = new BodyState();
        remifentanil.applyDamage(WoundType.SHARP, 16.0F, 0L);
        assertEquals(true, remifentanil.applyMedication(MedicationType.REMIFENTANIL, 100L, false), "remifentanil must create one short-lived active dose");
        assertEquals(1, remifentanil.activeDoseCount(MedicationType.REMIFENTANIL), "remifentanil must remain one stored drug dose rather than three duplicate doses");
        assertEquals(3, remifentanil.activeOpioidEquivalentLayerCount(), "the stored remifentanil dose must expose three opioid-equivalent layers");
        assertFloatEquals(14.0F, remifentanil.bloodDrugConcentration(), "one remifentanil dose must add fourteen drug concentration");
        assertFloatEquals(16.0F, remifentanil.medicationPainReduction(), "one remifentanil dose must reduce pain by sixteen");
        assertFloatEquals(4.0F, remifentanil.pain(), "remifentanil analgesia must immediately reduce effective pain");
        assertFloatEquals(14.0F, remifentanil.opioidRespiratoryDistressContribution(), "three opioid-equivalent layers must contribute fourteen respiratory distress");
        assertEquals(true, remifentanil.isTraumaticShockProtectionPaused(), "three opioid-equivalent layers must freeze traumatic-shock protection");
        BodyState restoredRemifentanil = new BodyState();
        restoredRemifentanil.deserializeNBT(remifentanil.serializeNBT());
        assertEquals(3, restoredRemifentanil.activeOpioidEquivalentLayerCount(), "remifentanil opioid equivalence must survive NBT through its medication type");
        assertEquals(true, restoredRemifentanil.applyMedication(MedicationType.NALOXONE, 101L, false), "naloxone must clear remifentanil as an opioid");
        assertEquals(0, restoredRemifentanil.activeDoseCount(MedicationType.REMIFENTANIL), "naloxone must remove the remifentanil dose");
        assertFloatEquals(0.0F, restoredRemifentanil.bloodDrugConcentration(), "naloxone must remove remifentanil concentration");
        assertFloatEquals(0.0F, restoredRemifentanil.medicationPainReduction(), "naloxone must remove remifentanil analgesia");
        assertFloatEquals(0.0F, restoredRemifentanil.opioidRespiratoryDistressContribution(), "naloxone must remove remifentanil respiratory suppression");

        BodyState remifentanilMorphineInteraction = new BodyState();
        remifentanilMorphineInteraction.applyMedication(MedicationType.REMIFENTANIL, 0L, false);
        remifentanilMorphineInteraction.applyMedication(MedicationType.MORPHINE, 1L, false);
        assertEquals(4, remifentanilMorphineInteraction.activeOpioidEquivalentLayerCount(), "remifentanil and morphine must combine into four opioid-equivalent layers");
        assertFloatEquals(20.0F, remifentanilMorphineInteraction.respiratoryDistress(), "remifentanil and morphine respiratory suppression must reach the gameplay cap");
        assertFloatEquals(20.0F, remifentanilMorphineInteraction.bloodDrugConcentration(), "remifentanil and morphine concentration must reach the overdose threshold");
        assertEquals(CollapseReason.HYPOXIA, remifentanilMorphineInteraction.collapseReason(), "combined remifentanil and morphine must collapse from hypoxia before generic overdose");

        BodyState remifentanilExpiry = new BodyState();
        remifentanilExpiry.applyMedication(MedicationType.REMIFENTANIL, 100L, false);
        remifentanilExpiry.resumeBodyProgression(100L);
        remifentanilExpiry.advanceBodyProgression(1_299L);
        assertEquals(1, remifentanilExpiry.activeDoseCount(MedicationType.REMIFENTANIL), "remifentanil must remain active for the full sixty-second half-open interval");
        remifentanilExpiry.advanceBodyProgression(1_300L);
        assertEquals(0, remifentanilExpiry.activeDoseCount(MedicationType.REMIFENTANIL), "remifentanil must expire at exactly sixty seconds");
        assertFloatEquals(0.0F, remifentanilExpiry.opioidRespiratoryDistressContribution(), "remifentanil opioid respiratory contribution must end with the dose");
        assertFloatEquals(0.0F, remifentanilExpiry.baseRespiratoryDistress(), "remifentanil must not leave negative base respiratory debt");
        assertFloatEquals(0.0F, remifentanilExpiry.respiratoryDistress(), "remifentanil expiry must return a clean patient to zero distress");

        BodyState opioidHypoxia = new BodyState();
        for (int layer = 0; layer < 4; layer++) {
            opioidHypoxia.applyMedication(MedicationType.MORPHINE, layer);
        }
        assertEquals(CollapseReason.HYPOXIA, opioidHypoxia.collapseReason(), "four opioid layers must reach hypoxic collapse before generic overdose handling");

        BodyState hypoxia = new BodyState();
        hypoxia.setRespiratoryDistressForDebug(20.0F, 0L);
        assertEquals(BodyLifeState.INCAPACITATED, hypoxia.lifeState(), "respiratory distress twenty must incapacitate an active patient");
        assertEquals(CollapseReason.HYPOXIA, hypoxia.collapseReason(), "respiratory collapse must retain the hypoxia reason");
        hypoxia.setRespiratoryDistressForDebug(3.0F, 1L);
        assertEquals(false, hypoxia.advanceAwakening(10.0F, 1L).changed(), "hypoxia awakening must require health above ten");
        assertEquals(true, hypoxia.advanceAwakening(10.1F, 2L).started(), "distress three and health above ten must begin hypoxia awakening");

        BodyState debugFloor = new BodyState();
        debugFloor.setRespiratoryDistressForDebug(-10.0F, 0L);
        assertFloatEquals(0.0F, debugFloor.baseRespiratoryDistress(), "debug writes must not create negative base distress");

        BodyState medicationFloor = new BodyState();
        for (int dose = 0; dose < 12; dose++) {
            medicationFloor.applyMedication(MedicationType.ATROPINE_SULFATE, dose, false);
        }
        assertFloatEquals(0.0F, medicationFloor.baseRespiratoryDistress(), "negative medication modifiers must remain separate from base distress");
        assertFloatEquals(-10.0F, medicationFloor.respiratoryDistress(), "temporary medication modifiers must retain the negative ten final floor");

        BodyState legacyNegativeBase = new BodyState();
        CompoundTag legacyNegativeTag = legacyNegativeBase.serializeNBT();
        legacyNegativeTag.putFloat("BaseRespiratoryDistress", -8.0F);
        legacyNegativeBase.deserializeNBT(legacyNegativeTag);
        assertFloatEquals(0.0F, legacyNegativeBase.baseRespiratoryDistress(), "stored negative base distress must migrate to zero");

        BodyState upperClamp = new BodyState();
        upperClamp.addRespiratoryDistress(100.0F);
        assertFloatEquals(20.0F, upperClamp.respiratoryDistress(), "respiratory distress must respect the upper bound of twenty");
    }

    private static void verifyOrganophosphateToxicologyAndAntidotes() {
        BodyState poisoning = new BodyState();
        assertEquals(true, poisoning.exposeToOrganophosphate(100L), "the first DDVP exposure must start stage one poisoning");
        assertEquals(1, poisoning.organophosphatePoisoningStage(), "DDVP must begin at stage one");
        long stageTwoDeadline = poisoning.organophosphateNextProgressionGameTime();
        assertBetween(
                100L + BodyState.ORGANOPHOSPHATE_STAGE_ONE_MIN_TICKS,
                100L + BodyState.ORGANOPHOSPHATE_STAGE_ONE_MAX_TICKS,
                stageTwoDeadline,
                "stage one must choose a thirty-to-sixty-second deadline"
        );
        assertEquals(true, poisoning.exposeToOrganophosphate(100L), "repeat DDVP exposure must accelerate the current stage");
        long acceleratedStageTwoDeadline = poisoning.organophosphateNextProgressionGameTime();
        assertBetween(
                stageTwoDeadline - BodyState.ORGANOPHOSPHATE_REPEAT_EXPOSURE_MAX_REDUCTION_TICKS,
                stageTwoDeadline - BodyState.ORGANOPHOSPHATE_REPEAT_EXPOSURE_MIN_REDUCTION_TICKS,
                acceleratedStageTwoDeadline,
                "repeat exposure must remove twenty to thirty seconds from the deadline"
        );
        stageTwoDeadline = acceleratedStageTwoDeadline;

        poisoning.advanceBodyProgression(stageTwoDeadline);
        assertEquals(2, poisoning.organophosphatePoisoningStage(), "the first deadline must advance poisoning to stage two");
        assertEquals(true, poisoning.hasActiveOrganophosphateSymptoms(), "stage two without atropine must expose symptoms");
        long stageThreeDeadline = poisoning.organophosphateNextProgressionGameTime();
        assertBetween(
                stageTwoDeadline + BodyState.ORGANOPHOSPHATE_STAGE_TWO_MIN_TICKS,
                stageTwoDeadline + BodyState.ORGANOPHOSPHATE_STAGE_TWO_MAX_TICKS,
                stageThreeDeadline,
                "stage two must choose a one-to-two-minute deadline"
        );

        BodyState persisted = new BodyState();
        persisted.deserializeNBT(poisoning.serializeNBT());
        assertEquals(2, persisted.organophosphatePoisoningStage(), "NBT must preserve the poisoning stage");
        assertEquals(stageThreeDeadline, persisted.organophosphateNextProgressionGameTime(), "NBT must preserve the progression deadline");
        persisted.advanceBodyProgression(stageThreeDeadline);
        assertEquals(3, persisted.organophosphatePoisoningStage(), "the second deadline must advance poisoning to stage three");
        assertFloatEquals(1.0F, persisted.toxicologyRespiratoryDistressContribution(), "stage three must add one respiratory-distress point");
        assertEquals(BodyLifeState.INCAPACITATED, persisted.lifeState(), "stage three must incapacitate an active patient");
        assertEquals(CollapseReason.ORGANOPHOSPHATE_POISONING, persisted.collapseReason(), "toxic collapse must retain its cause");

        BodyState atropine = new BodyState();
        atropine.exposeToOrganophosphate(0L);
        atropine.advanceBodyProgression(atropine.organophosphateNextProgressionGameTime());
        assertEquals(true, atropine.applyMedication(MedicationType.ATROPINE_SULFATE, 2_000L), "atropine must create an active layer");
        assertEquals(1, atropine.organophosphatePoisoningStage(), "atropine must suppress established poisoning back to stage one");
        assertEquals(-1L, atropine.organophosphateNextProgressionGameTime(), "active atropine must pause poisoning progression");
        atropine.applyMedication(MedicationType.ATROPINE_SULFATE, 2_001L);
        assertFloatEquals(6.0F, atropine.bloodDrugConcentration(), "two atropine layers must add six concentration");
        assertEquals(2, atropine.effectiveHeartRateLevel(), "each atropine layer must add one tachycardia level");
        assertFloatEquals(-1.0F, atropine.toxicologyRespiratoryDistressContribution(), "the second atropine layer must reduce respiratory distress by one");
        assertEquals(true, atropine.applyMedication(MedicationType.PRALIDOXIME_CHLORIDE, 2_002L), "pralidoxime must create an active layer");
        assertEquals(0, atropine.organophosphatePoisoningStage(), "pralidoxime must remove poisoning once atropine has reduced it to stage one");

        BodyState pralidoximeLimit = new BodyState();
        pralidoximeLimit.exposeToOrganophosphate(0L);
        pralidoximeLimit.advanceBodyProgression(pralidoximeLimit.organophosphateNextProgressionGameTime());
        pralidoximeLimit.applyMedication(MedicationType.PRALIDOXIME_CHLORIDE, 1L);
        assertEquals(2, pralidoximeLimit.organophosphatePoisoningStage(), "pralidoxime must not remove stage two poisoning");

        BodyState pralidoximeLayers = new BodyState();
        pralidoximeLayers.applyMedication(MedicationType.PRALIDOXIME_CHLORIDE, 0L);
        pralidoximeLayers.applyMedication(MedicationType.PRALIDOXIME_CHLORIDE, 1L);
        pralidoximeLayers.applyMedication(MedicationType.PRALIDOXIME_CHLORIDE, 2L);
        assertFloatEquals(6.0F, pralidoximeLayers.bloodDrugConcentration(), "three pralidoxime layers must add six concentration");
        assertEquals(1, pralidoximeLayers.effectiveHeartRateLevel(), "the third pralidoxime layer must add one tachycardia level");
        assertFloatEquals(1.0F, pralidoximeLayers.toxicologyRespiratoryDistressContribution(), "the third pralidoxime layer must add one respiratory-distress point");

        BodyState awakeningBlock = new BodyState();
        awakeningBlock.incapacitate(CollapseReason.TRAUMATIC_SHOCK, 0L);
        awakeningBlock.exposeToOrganophosphate(1L);
        assertEquals(false, awakeningBlock.advanceAwakening(6.0F, 1L).changed(), "any active poisoning stage must block awakening");
        awakeningBlock.applyMedication(MedicationType.PRALIDOXIME_CHLORIDE, 2L, true);
        assertEquals(0, awakeningBlock.organophosphatePoisoningStage(), "pralidoxime must clear stage one poisoning in a downed patient");
        assertEquals(true, awakeningBlock.advanceAwakening(6.0F, 2L).started(), "awakening may resume after poisoning is fully cleared");
    }

    private static void verifyCommonAwakeningGate() {
        BodyState concentrationBlocked = new BodyState();
        concentrationBlocked.incapacitate(CollapseReason.TRAUMATIC_SHOCK, 0L);
        for (int index = 0; index < 5; index++) {
            concentrationBlocked.applyMedication(MedicationType.PARACETAMOL, index, true);
        }
        assertEquals(
                false,
                concentrationBlocked.advanceAwakening(6.0F, 5L).changed(),
                "concentration twenty must block every collapse reason from awakening"
        );
        assertEquals(
                CollapseReason.TRAUMATIC_SHOCK,
                concentrationBlocked.collapseReason(),
                "a later common-gate failure must not replace the original collapse reason"
        );

        BodyState respiratoryBlocked = new BodyState();
        respiratoryBlocked.incapacitate(CollapseReason.TRAUMATIC_SHOCK, 0L);
        respiratoryBlocked.setRespiratoryDistressForDebug(20.0F, 1L);
        assertEquals(
                false,
                respiratoryBlocked.advanceAwakening(6.0F, 1L).changed(),
                "respiratory distress twenty must block every collapse reason from awakening"
        );
        assertEquals(
                CollapseReason.TRAUMATIC_SHOCK,
                respiratoryBlocked.collapseReason(),
                "respiratory distress must not rewrite the original collapse reason while downed"
        );
    }

    private static BodyState epinephrineCountdownState(int layers) {
        BodyState state = new BodyState();
        state.resumeBodyProgression(0L);
        state.forceCardiacRhythmForDebug(BodyLifeState.CARDIAC_ARREST, 0L);
        for (int index = 0; index < layers; index++) {
            state.applyMedication(MedicationType.EPINEPHRINE, 0L, true);
        }
        return state;
    }

    private static void verifyGiveUpAndTotalCountdown() {
        BodyState active = new BodyState();
        assertEquals(false, active.giveUp(), "an active player must not be able to give up");

        BodyState awakening = new BodyState();
        awakening.incapacitate(CollapseReason.TRAUMATIC_SHOCK, 0L);
        assertEquals(true, awakening.advanceAwakening(6.0F, 0L).started(), "test patient must enter awakening");
        assertEquals(false, awakening.giveUp(), "a player in awakening must not be able to give up");

        BodyState downed = new BodyState();
        downed.incapacitate(CollapseReason.TRAUMATIC_SHOCK, 100L);
        assertEquals(
                Math.round(BodyState.INITIAL_INCAPACITATED_BLOOD_OXYGEN
                        * BodyState.BLOOD_OXYGEN_POINT_DURATION_TICKS)
                        + BodyState.CARDIAC_ARREST_DURATION_TICKS,
                downed.totalDownedDangerRemainingTicks(100L),
                "incapacitated overlay must combine the remaining incapacitated and cardiac-arrest phases"
        );
        assertEquals(true, downed.giveUp(), "an incapacitated player must be able to give up");
        assertEquals(BodyLifeState.BRAIN_DEAD, downed.lifeState(), "giving up must immediately enter brain death");
        assertEquals(true, downed.voluntaryDeath(), "giving up must leave permanent voluntary-death evidence");

        BodyState restored = new BodyState();
        restored.deserializeNBT(downed.serializeNBT());
        assertEquals(true, restored.voluntaryDeath(), "voluntary-death evidence must survive player NBT round trip");
        assertEquals(BodyLifeState.BRAIN_DEAD, restored.lifeState(), "give-up brain death must survive player NBT round trip");
    }

    private static void verifyAdministrativeKillBrainDeath() {
        BodyState state = new BodyState();
        state.recordFinalDamage(12.0F, "minecraft:arrow", DamageClassification.blunt("test"), 10L);
        assertEquals(
                true,
                state.incapacitateFromLastDamage(CollapseReason.HEMORRHAGIC_SHOCK, 10L),
                "the fixture must begin with ordinary downing evidence"
        );
        assertEquals(false, state.downingHitRecord().isEmpty(), "the fixture must contain a downing hit before /kill");

        assertEquals(true, state.forceAdministrativeBrainDeath(), "generic_kill must immediately enter brain death");
        assertEquals(BodyLifeState.BRAIN_DEAD, state.lifeState(), "generic_kill must bypass all downed timers");
        assertEquals(true, state.administrativeDeath(), "generic_kill must leave distinct forensic evidence");
        assertEquals(false, state.voluntaryDeath(), "generic_kill must not look like the patient gave up");
        assertEquals(true, state.downingHitRecord().isEmpty(), "generic_kill must not attribute an older hit as fatal");
        assertEquals(false, state.forceAdministrativeBrainDeath(), "brain death must not be applied twice");

        BodyState restored = new BodyState();
        restored.deserializeNBT(state.serializeNBT());
        assertEquals(BodyLifeState.BRAIN_DEAD, restored.lifeState(), "administrative brain death must survive NBT");
        assertEquals(true, restored.administrativeDeath(), "administrative forensic evidence must survive NBT");
        assertEquals(false, restored.voluntaryDeath(), "restored administrative death must remain non-voluntary");
        assertEquals(true, restored.downingHitRecord().isEmpty(), "restored administrative death must not expose an old downing hit");
    }

    private static void verifyStandardVitalSignRanges() {
        assertEquals(HealthStatus.OK, HealthStatus.from(20.0F, false), "twenty health must be OK");
        assertEquals(HealthStatus.OK, HealthStatus.from(18.0F, false), "eighteen must remain in the left-closed OK interval");
        assertEquals(HealthStatus.VERY_MINOR_DAMAGE, HealthStatus.from(17.999F, false), "values below eighteen must be very minor damage");
        assertEquals(HealthStatus.VERY_MINOR_DAMAGE, HealthStatus.from(16.0F, false), "sixteen must remain in the very-minor interval");
        assertEquals(HealthStatus.MINOR_DAMAGE, HealthStatus.from(15.999F, false), "values below sixteen must be minor damage");
        assertEquals(HealthStatus.MINOR_DAMAGE, HealthStatus.from(12.0F, false), "twelve must remain in the minor interval");
        assertEquals(HealthStatus.MODERATE_DAMAGE, HealthStatus.from(11.999F, false), "values below twelve must be moderate damage");
        assertEquals(HealthStatus.MODERATE_DAMAGE, HealthStatus.from(8.0F, false), "eight must remain in the moderate interval");
        assertEquals(HealthStatus.SEVERE_DAMAGE, HealthStatus.from(7.999F, false), "values below eight must be severe damage");
        assertEquals(HealthStatus.SEVERE_DAMAGE, HealthStatus.from(4.0F, false), "four must remain in the severe interval");
        assertEquals(HealthStatus.TERMINAL_DAMAGE, HealthStatus.from(3.999F, false), "values below four must be terminal damage");
        assertEquals(HealthStatus.DOWNED, HealthStatus.from(20.0F, true), "downed state must override the numerical health range");

        assertEquals(PainSensation.NONE, PainSensation.from(0.0F), "zero pain must not display a sensation");
        assertEquals(PainSensation.MINOR_PAIN, PainSensation.from(0.001F), "positive pain below five must be minor pain");
        assertEquals(PainSensation.MINOR_PAIN, PainSensation.from(4.999F), "values below five must remain minor pain");
        assertEquals(PainSensation.PAIN, PainSensation.from(5.0F), "five must enter the pain interval");
        assertEquals(PainSensation.PAIN, PainSensation.from(11.999F), "values below twelve must remain pain");
        assertEquals(PainSensation.SEVERE_PAIN, PainSensation.from(12.0F), "twelve must enter severe pain");
        assertEquals(PainSensation.SEVERE_PAIN, PainSensation.from(17.999F), "values below eighteen must remain severe pain");
        assertEquals(PainSensation.EXTREME_PAIN, PainSensation.from(18.0F), "eighteen must enter extreme pain");
    }

    private static void verifyTraumaticShockAwakeningAndRetryCooldown() {
        BodyState threshold = new BodyState();
        threshold.applyDamage(WoundType.BLUNT, 14.0F, 0L);
        assertFloatEquals(15.0F, threshold.pain(), "awakening threshold setup must produce exactly fifteen pain");
        threshold.incapacitate(CollapseReason.TRAUMATIC_SHOCK, 0L);
        assertEquals(false, threshold.advanceAwakening(6.0F, 1L).changed(), "pain equal to fifteen must block traumatic-shock awakening");

        BodyState belowThreshold = new BodyState();
        belowThreshold.applyDamage(WoundType.BLUNT, 13.0F, 0L);
        assertFloatEquals(14.0F, belowThreshold.pain(), "below-threshold setup must produce fourteen pain");
        belowThreshold.incapacitate(CollapseReason.TRAUMATIC_SHOCK, 0L);
        assertEquals(true, belowThreshold.advanceAwakening(6.0F, 1L).started(), "pain below fifteen and health above five must begin awakening");

        BodyState state = new BodyState();
        state.incapacitate(CollapseReason.TRAUMATIC_SHOCK, 0L);
        assertEquals(false, state.advanceAwakening(5.0F, 0L).changed(), "health equal to five must not begin awakening");
        assertEquals(true, state.advanceAwakening(5.1F, 1L).started(), "pain below fifteen and health above five must begin awakening");

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

    private static void verifyPunctureAndFrostbiteDebridement() {
        BodyState punctureOneState = new BodyState();
        WoundInstance punctureOne = requireWound(punctureOneState.applyDamage(
                WoundType.PUNCTURE,
                4.0F,
                0.29F,
                0L
        ));
        assertEquals(1, punctureOne.severity(), "four projectile damage must create low-energy puncture trauma");
        assertEquals(true, punctureOne.woundTags().contains(WoundTag.NEEDS_DEBRIDEMENT_1), "a puncture roll below thirty percent must require debridement");
        assertEquals(1, punctureOne.bleedingLevel(false), "puncture severity one must bleed at level one");
        assertFloatEquals(0.5F, punctureOne.baseHealingPerSecond(), "puncture severity one must heal at 0.5 H/s");
        punctureOne.advanceInfection(WoundInstance.INFECTION_SPREAD_INTERVAL_TICKS);
        assertFloatEquals(1.0F, punctureOne.infectionContribution(), "a dirty severity-one puncture must receive contamination and its first deep-wound infection pulse");

        BodyState cleanPunctureState = new BodyState();
        WoundInstance cleanPuncture = requireWound(cleanPunctureState.applyDamage(
                WoundType.PUNCTURE,
                10.0F,
                0.80F,
                0L
        ));
        assertEquals(false, cleanPuncture.woundTags().contains(WoundTag.NEEDS_DEBRIDEMENT_1), "the puncture level-two debridement range must be half open");
        cleanPunctureState.applyDamage(WoundType.PUNCTURE, 5.0F, 0.0F, 1L);
        assertEquals(3, cleanPuncture.severity(), "a level-two puncture may accumulate into a through wound");
        assertEquals(false, cleanPuncture.woundTags().contains(WoundTag.NEEDS_DEBRIDEMENT_1), "level-two to level-three accumulation must carry the prior cleaning state without a new roll");

        BodyState dirtyPunctureState = new BodyState();
        WoundInstance dirtyPuncture = requireWound(dirtyPunctureState.applyDamage(
                WoundType.PUNCTURE,
                10.0F,
                0.79F,
                0L
        ));
        dirtyPunctureState.applyDamage(WoundType.PUNCTURE, 5.0F, 0.99F, 1L);
        assertEquals(true, dirtyPuncture.woundTags().contains(WoundTag.NEEDS_DEBRIDEMENT_1), "an accumulated through wound must retain its level-two debridement tag");

        BodyState directThroughState = new BodyState();
        WoundInstance directThrough = requireWound(directThroughState.applyDamage(
                WoundType.PUNCTURE,
                15.0F,
                0.64F,
                0L
        ));
        assertEquals(true, directThrough.woundTags().contains(WoundTag.NEEDS_DEBRIDEMENT_1), "a direct through wound must use the sixty-five-percent roll");

        BodyState frostbiteTwoState = new BodyState();
        WoundInstance frostbiteTwo = requireWound(frostbiteTwoState.applyDamage(
                WoundType.FROSTBITE,
                12.0F,
                0.79F,
                0L
        ));
        assertEquals(true, frostbiteTwo.woundTags().contains(WoundTag.NEEDS_DEBRIDEMENT_1), "frostbite severity two must use the eighty-percent debridement roll");
        assertEquals(true, frostbiteTwo.woundTags().contains(WoundTag.PAIN_3), "frostbite severity two must carry pain three");

        BodyState severeFrostbiteState = new BodyState();
        WoundInstance severeFrostbite = requireWound(severeFrostbiteState.applyDamage(
                WoundType.FROSTBITE,
                16.0F,
                0.99F,
                0L
        ));
        assertEquals(true, severeFrostbite.woundTags().contains(WoundTag.NEEDS_DEBRIDEMENT_1), "severe frostbite must always require debridement");
        assertEquals(true, severeFrostbite.woundTags().contains(WoundTag.NECROSIS_3), "severe frostbite must carry necrosis three");
        assertFloatEquals(0.08F, severeFrostbite.baseHealingPerSecond(), "severe frostbite must heal at 0.08 H/s before its grafting floor");
        severeFrostbite.advanceNaturalHealing(2_000.0F);
        assertFloatEquals(40.0F, severeFrostbite.healingProgress(), "a wound requiring debridement must stop at H=40");
        assertEquals(true, severeFrostbiteState.debrideWound(severeFrostbite.id()), "debridement must release the infection-control healing floor");
        severeFrostbite.advanceNaturalHealing(2_000.0F);
        assertFloatEquals(10.0F, severeFrostbite.healingProgress(), "severe frostbite must stop at H=10 until skin grafting exists");
    }

    private static void verifyCrushIcePackHealing() {
        BodyState mildState = new BodyState();
        WoundInstance mild = requireWound(mildState.applyDamage(WoundType.CRUSH, 2.0F, 0L));
        assertFloatEquals(0.8F, mild.baseHealingPerSecond(), "mild crush trauma must naturally heal at 0.8 H/s");
        assertEquals(true, mildState.applyIcePack(mild.id(), 0L), "mild crush trauma must accept one ice pack");
        assertEquals(true, mild.icePackApplied(), "crush trauma must retain its cold-treatment state");
        assertFloatEquals(3.0F, mild.baseHealingPerSecond(false), "an iced mild crush injury must heal at 3 H/s while stationary");
        assertFloatEquals(0.8F, mild.baseHealingPerSecond(true), "movement must return an iced mild crush injury to its base rate");
        mildState.resumeBodyProgression(0L);
        mildState.advanceBodyProgression(20L, false);
        assertFloatEquals(97.0F, mild.healingProgress(), "one stationary second must apply the ice-pack acceleration");
        mildState.advanceBodyProgression(40L, true);
        assertFloatEquals(96.2F, mild.healingProgress(), "one moving second must apply only the base crush recovery rate");

        BodyState restoredState = new BodyState();
        restoredState.deserializeNBT(mildState.serializeNBT());
        WoundInstance restored = restoredState.wound(mild.id())
                .orElseThrow(() -> new AssertionError("an iced crush wound must survive NBT"));
        assertEquals(true, restored.icePackApplied(), "NBT must preserve crush ice-pack treatment");

        WoundInstance moderate = WoundInstance.create(WoundType.CRUSH, 6.0F, 0L, 400L);
        assertEquals(true, moderate.woundTags().contains(WoundTag.PAIN_2), "moderate crush trauma must carry pain two");
        assertEquals(true, moderate.applyIcePack(), "moderate crush trauma must accept an ice pack");
        assertFloatEquals(1.0F, moderate.baseHealingPerSecond(false), "an iced moderate crush injury must heal at 1 H/s while stationary");
        assertFloatEquals(0.3F, moderate.baseHealingPerSecond(true), "movement must restore the moderate crush base rate");

        WoundInstance severe = WoundInstance.create(WoundType.CRUSH, 12.0F, 0L, 400L);
        assertEquals(true, severe.woundTags().contains(WoundTag.NECROSIS_2), "severe crush trauma must retain intrinsic necrosis two");
        assertEquals(true, severe.woundTags().contains(WoundTag.BLEEDING_1), "severe crush trauma must bleed at level one");
        assertFloatEquals(0.1F, severe.baseHealingPerSecond(), "severe crush trauma must heal at 0.1 H/s");
        assertEquals(false, severe.canApplyIcePack(), "severe crush trauma must reject ice packs");
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
                    type == TreatmentType.ICE_PACK
                            || type == TreatmentType.POVIDONE_IODINE
                            || type == TreatmentType.MEDICAL_ALCOHOL,
                    TreatmentProcedure.supportsType(bluntTwo, type),
                    "severity-two blunt trauma must expose the ice pack and preventive disinfectants"
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
        assertEquals(true, state.hasNecrosisFatigue(), "Necrosis 2 must contribute fatigue");

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
        assertEquals(false, restored.hasNecrosisFatigue(), "clearing Necrosis 2 must remove its fatigue contribution");

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

        state.advanceBodyProgression(479L);
        assertFloatEquals(5.0F, state.basePain(), "base pain must not recover during stress or its first four-second wait");

        BodyProgressionResult firstRecovery = state.advanceBodyProgression(480L);
        assertFloatEquals(0.5F, firstRecovery.recoveredBasePain(), "one wound-pain layer must halve the four-second recovery amount");
        assertFloatEquals(4.5F, state.basePain(), "one wound-pain layer must recover one base-pain point every eight seconds");
        assertFloatEquals(5.5F, state.pain(), "pain 1 must remain as a one-point wound contribution");

        BodyProgressionResult delayedRecovery = state.advanceBodyProgression(640L);
        assertFloatEquals(1.0F, delayedRecovery.recoveredBasePain(), "delayed processing must catch up complete four-second intervals at the current rate");
        assertFloatEquals(3.5F, state.basePain(), "two additional one-layer intervals must remove one point");
    }

    private static void verifyPainRecoveryCurveAndOpioids() {
        assertEquals(1, WoundTag.PAIN_1.painRecoveryLayers(), "pain 1 must count as one recovery-slowdown layer");
        assertEquals(2, WoundTag.PAIN_2.painRecoveryLayers(), "pain 2 must count as two recovery-slowdown layers");
        assertEquals(3, WoundTag.PAIN_3.painRecoveryLayers(), "pain 3 must count as three recovery-slowdown layers rather than its four-point contribution");
        assertEquals(2, WoundTag.ALCOHOL_PAIN_2.painRecoveryLayers(), "alcohol irritation pain 2 must add two recovery-slowdown layers");

        BodyState noWoundPain = new BodyState();
        noWoundPain.applyDamage(WoundType.BLUNT, 1.0F, 0L);
        assertEquals(0, noWoundPain.woundPainRecoveryLayers(), "damage below the wound threshold must not invent pain layers");
        assertFloatEquals(1.0F, noWoundPain.basePainRecoveryPerInterval(), "zero pain layers must recover one point every four seconds");

        BodyState painOne = new BodyState();
        painOne.applyDamage(WoundType.SHARP, 5.0F, 0L);
        assertEquals(1, painOne.woundPainRecoveryLayers(), "pain 1 must expose one recovery layer");
        assertFloatEquals(0.5F, painOne.basePainRecoveryPerInterval(), "one layer must recover half a point every four seconds");

        BodyState painTwo = new BodyState();
        painTwo.applyDamage(WoundType.BURN, 0.25F, 0L);
        assertEquals(2, painTwo.woundPainRecoveryLayers(), "pain 2 must expose two recovery layers");
        assertFloatEquals(0.25F, painTwo.basePainRecoveryPerInterval(), "two layers must recover one quarter point every four seconds");

        BodyState painThree = new BodyState();
        painThree.applyDamage(WoundType.SHARP, 15.0F, 0L);
        assertEquals(3, painThree.woundPainRecoveryLayers(), "pain 3 must expose three recovery layers");
        assertFloatEquals(1.0F / 6.0F, painThree.basePainRecoveryPerInterval(), "three layers must recover one point every twenty-four seconds");

        BodyState capped = new BodyState();
        capped.applyDamage(WoundType.SHARP, 15.0F, 0L);
        capped.applyDamage(WoundType.BURN, 5.0F, 0L);
        capped.applyDamage(WoundType.PUNCTURE, 10.0F, 0L);
        capped.applyDamage(WoundType.BLUNT, 4.0F, 0L);
        assertEquals(8, capped.woundPainRecoveryLayers(), "wound-pain recovery slowdown must cap at eight layers");
        assertEquals(8, capped.effectivePainRecoveryLayers(), "the non-opioid effective layer count must retain the eight-layer cap");
        assertFloatEquals(0.0625F, capped.basePainRecoveryPerInterval(), "eight layers must recover one point every sixty-four seconds");

        assertEquals(true, capped.applyMedication(MedicationType.MORPHINE, 0L, false), "one morphine dose must apply for opioid recovery testing");
        assertEquals(2, capped.effectivePainRecoveryLayers(), "one opioid dose must cap recovery slowdown at two layers");
        assertFloatEquals(0.25F, capped.basePainRecoveryPerInterval(), "opioids must restore high-layer recovery to one point every sixteen seconds");
        assertEquals(true, capped.applyMedication(MedicationType.MORPHINE, 1L, false), "a second morphine dose must apply");
        assertEquals(2, capped.effectivePainRecoveryLayers(), "additional opioid doses must not improve the two-layer recovery cap");
        assertFloatEquals(0.25F, capped.basePainRecoveryPerInterval(), "opioid recovery acceleration must not stack by dose count");
    }

    private static void verifyTraumaticShockMedicationProtection() {
        BodyState paracetamol = new BodyState();
        paracetamol.applyDamage(WoundType.SHARP, 1.0F, 100L);
        assertEquals(500L, paracetamol.traumaticShockProtectionEndGameTime(), "unmedicated trauma must retain the base twenty-second protection");
        paracetamol.applyMedication(MedicationType.PARACETAMOL, 200L, false);
        assertEquals(900L, paracetamol.traumaticShockProtectionEndGameTime(), "one paracetamol layer must extend total protection to forty seconds from the last injury");
        paracetamol.applyMedication(MedicationType.PARACETAMOL, 201L, false);
        assertEquals(1_100L, paracetamol.traumaticShockProtectionEndGameTime(), "two paracetamol layers must extend total protection to fifty seconds");
        paracetamol.applyMedication(MedicationType.PARACETAMOL, 202L, false);
        assertEquals(1_200L, paracetamol.traumaticShockProtectionEndGameTime(), "three paracetamol layers must extend total protection to fifty-five seconds");
        paracetamol.applyMedication(MedicationType.PARACETAMOL, 203L, false);
        assertEquals(1_200L, paracetamol.traumaticShockProtectionEndGameTime(), "a fourth paracetamol layer must not extend shock protection further");

        BodyState opioid = new BodyState();
        opioid.applyDamage(WoundType.SHARP, 1.0F, 100L);
        opioid.applyMedication(MedicationType.PARACETAMOL, 200L, false);
        opioid.applyMedication(MedicationType.PARACETAMOL, 201L, false);
        opioid.applyMedication(MedicationType.PARACETAMOL, 202L, false);
        opioid.applyMedication(MedicationType.MORPHINE, 250L, false);
        assertEquals(1_300L, opioid.traumaticShockProtectionEndGameTime(), "one opioid layer must use the stronger sixty-second total instead of adding to paracetamol");
        opioid.applyDamage(WoundType.BLUNT, 1.0F, 300L);
        assertEquals(300L, opioid.lastTraumaticDamageGameTime(), "later trauma must replace the protection-period origin");
        assertEquals(1_500L, opioid.traumaticShockProtectionEndGameTime(), "later trauma must calculate opioid protection from the new injury time");

        BodyState lateDose = new BodyState();
        lateDose.applyDamage(WoundType.SHARP, 1.0F, 0L);
        lateDose.applyMedication(MedicationType.PARACETAMOL, 900L, false);
        assertEquals(800L, lateDose.traumaticShockProtectionEndGameTime(), "late medication must not grant a fresh duration from administration time");
        assertEquals(0L, lateDose.traumaticShockProtectionRemainingTicks(900L), "an extension already elapsed since the last injury must remain elapsed");

        BodyState recovery = new BodyState();
        recovery.applyDamage(WoundType.SHARP, 5.0F, 0L);
        recovery.resumeBodyProgression(0L);
        recovery.applyMedication(MedicationType.MORPHINE, 100L, false);
        assertEquals(1_200L, recovery.traumaticShockProtectionEndGameTime(), "morphine must extend only the independent shock protection deadline");
        assertEquals(400L, recovery.stressEndGameTime(), "morphine must not extend the twenty-second pain-recovery stress period");
        BodyProgressionResult recoveryPulse = recovery.advanceBodyProgression(480L);
        assertFloatEquals(0.5F, recoveryPulse.recoveredBasePain(), "base pain must recover on its original schedule during extended shock protection");
        assertEquals(true, recovery.isTraumaticShockProtectionActive(480L), "shock protection must remain active after base pain recovery has begun");

        BodyState paused = new BodyState();
        paused.applyDamage(WoundType.SHARP, 40.0F, 0L);
        paused.resumeBodyProgression(0L);
        paused.applyMedication(MedicationType.MORPHINE, 100L, false);
        paused.applyMedication(MedicationType.MORPHINE, 200L, false);
        assertEquals(true, paused.isTraumaticShockProtectionPaused(), "two opioid layers must pause the independent shock-protection timer");
        assertEquals(1_000L, paused.traumaticShockProtectionRemainingTicks(200L), "the paused timer must retain the remaining part of the sixty-second total");
        paused.advanceBodyProgression(600L);
        assertEquals(1_000L, paused.traumaticShockProtectionRemainingTicks(600L), "time under two opioid layers must not consume protection");
        assertEquals(-1L, paused.shockWarningEndGameTime(), "two opioid layers must prevent traumatic-shock warning onset while active");

        BodyState pausedRestored = new BodyState();
        pausedRestored.deserializeNBT(paused.serializeNBT());
        assertEquals(200L, pausedRestored.traumaticShockProtectionPausedAtGameTime(), "NBT must preserve the opioid pause origin");
        assertEquals(1_000L, pausedRestored.traumaticShockProtectionRemainingTicks(600L), "NBT must preserve frozen protection time");
        assertEquals(true, pausedRestored.applyMedication(MedicationType.NALOXONE, 600L, false), "naloxone must clear the opioid layers that hold the timer paused");
        assertEquals(false, pausedRestored.isTraumaticShockProtectionPaused(), "naloxone must resume the frozen protection timer");
        assertEquals(1_000L, pausedRestored.traumaticShockProtectionRemainingTicks(600L), "resuming must preserve rather than consume the frozen interval");
        pausedRestored.advanceBodyProgression(1_599L);
        assertEquals(-1L, pausedRestored.shockWarningEndGameTime(), "the resumed protection must remain valid until its exact deadline");
        BodyProgressionResult resumedWarning = pausedRestored.advanceBodyProgression(1_600L);
        assertEquals(true, resumedWarning.shockWarningStarted(), "traumatic-shock checks must resume when the preserved protection expires");
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
        WoundHistoryEntry forensicWound = new WoundHistoryEntry(
                UUID.fromString("d26f024b-2cd2-42da-a7aa-2abcc7195d7d"),
                WoundType.GUNSHOT_HIGH_VELOCITY,
                2,
                11.0F,
                80L,
                100L,
                -1L,
                true,
                false
        );
        DowningHitRecord downingHit = new DowningHitRecord(
                9.0F,
                "minecraft:arrow",
                DamageKind.CGM_HIGH_VELOCITY,
                "projectile",
                "cgm:projectile",
                "cgm:rifle_ammo",
                "cgm:rifle",
                18.5D,
                120L
        );
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
                ),
                List.of(forensicWound),
                downingHit,
                CollapseReason.HEMORRHAGIC_SHOCK,
                true,
                false
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
        assertEquals(List.of(forensicWound), restored.woundHistory(), "corpse NBT must freeze wound history");
        assertEquals(downingHit, restored.downingHitRecord(), "corpse NBT must freeze the incapacitating hit");
        assertEquals(CollapseReason.HEMORRHAGIC_SHOCK, restored.collapseReason(), "corpse NBT must freeze the collapse reason");
        assertEquals(true, restored.voluntaryDeath(), "corpse NBT must preserve voluntary-death forensic evidence");
        assertEquals(false, restored.administrativeDeath(), "ordinary corpse NBT must not invent administrative death");
        assertFloatEquals(
                -52.5F,
                DownedGeometry.groundYaw(restored.downedPose()),
                "corpse direction must remain derived from the captured left-fall snapshot"
        );

        CompoundTag legacySnapshot = saved.copy();
        legacySnapshot.remove("CollapseReason");
        assertEquals(
                CollapseReason.NONE,
                CorpseSnapshot.load(legacySnapshot).collapseReason(),
                "legacy corpses without a collapse reason must remain readable without inventing evidence"
        );

        CorpseSnapshot administrative = new CorpseSnapshot(
                ownerId,
                "AdministrativeDeathPlayer",
                "",
                "",
                700L,
                original.downedPose(),
                List.of(forensicWound),
                downingHit,
                CollapseReason.HEMORRHAGIC_SHOCK,
                true,
                true
        );
        CorpseSnapshot restoredAdministrative = CorpseSnapshot.load(administrative.save());
        assertEquals(true, restoredAdministrative.administrativeDeath(), "corpse NBT must preserve administrative death");
        assertEquals(false, restoredAdministrative.voluntaryDeath(), "administrative death must override voluntary evidence");
        assertEquals(true, restoredAdministrative.downingHitRecord() == null, "administrative death must suppress an older incapacitating hit");
        assertEquals(CollapseReason.NONE, restoredAdministrative.collapseReason(), "administrative death must suppress an older collapse reason");
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

    private static void verifyAutopsyReportPrivacy() {
        DowningHitRecord hiddenHit = new DowningHitRecord(
                8.0F,
                "minecraft:player_attack",
                DamageKind.CGM_LOW_VELOCITY,
                "projectile",
                "cgm:projectile",
                "cgm:basic_bullet",
                "cgm:pistol",
                7.25D,
                200L
        );
        AutopsyReport basicReport = new AutopsyReport(
                17,
                "EvidencePlayer",
                -1L,
                List.of(),
                false,
                hiddenHit,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                -1L,
                AutopsyAction.NONE,
                -1L
        );
        assertEquals(
                true,
                basicReport.downingHit() == null,
                "unrevealed downing evidence must not enter a basic client report"
        );
        assertEquals(
                false,
                basicReport.save().contains("DowningHit", Tag.TAG_COMPOUND),
                "unrevealed downing evidence must not be sent over the network"
        );
        assertEquals(false, basicReport.suspectedMyocardialInfarction(), "voluntary-death evidence must remain hidden before detailed autopsy");
        assertEquals(false, basicReport.drowningDeath(), "drowning cause must remain hidden before detailed autopsy");
        assertEquals(false, basicReport.organophosphatePoisoningDeath(), "organophosphate cause must remain hidden before detailed autopsy");
        assertEquals(false, basicReport.administrativeDeath(), "administrative death must remain hidden before detailed autopsy");

        AutopsyReport detailedReport = new AutopsyReport(
                17,
                "EvidencePlayer",
                1_200L,
                List.of(),
                true,
                hiddenHit,
                true,
                true,
                true,
                false,
                true,
                true,
                true,
                1_500L,
                AutopsyAction.CHECKLIST,
                900L
        );
        AutopsyReport restored = AutopsyReport.load(detailedReport.save());
        assertEquals(hiddenHit, restored.downingHit(), "completed detailed examination must preserve downing evidence");
        assertEquals(true, restored.drowningDeath(), "completed detailed examination must preserve a drowning finding");
        assertEquals(true, restored.organophosphatePoisoningDeath(), "completed detailed examination must preserve an organophosphate finding");
        assertEquals(true, restored.suspectedMyocardialInfarction(), "detailed autopsy must preserve the voluntary-death finding");
        assertEquals(1_200L, restored.deathAgeTicks(), "pupil examination timestamp must survive packet NBT");
        assertEquals(AutopsyAction.CHECKLIST, restored.activeAction(), "autopsy action state must survive packet NBT");
        assertEquals(900L, restored.actionEndGameTime(), "autopsy countdown deadline must survive packet NBT");
        assertEquals(1_500L, restored.penlightCooldownEndGameTime(), "penlight cooldown must survive packet NBT");

        AutopsyReport administrativeReport = new AutopsyReport(
                18,
                "AdministrativeDeathPlayer",
                600L,
                List.of(),
                true,
                hiddenHit,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                -1L,
                AutopsyAction.NONE,
                -1L
        );
        AutopsyReport restoredAdministrative = AutopsyReport.load(administrativeReport.save());
        assertEquals(true, restoredAdministrative.administrativeDeath(), "detailed autopsy must preserve administrative-death evidence");
        assertEquals(true, restoredAdministrative.downingHit() == null, "administrative death must suppress misleading old downing evidence");
        assertEquals(false, restoredAdministrative.drowningDeath(), "administrative death must suppress a misleading drowning finding");
        assertEquals(false, restoredAdministrative.organophosphatePoisoningDeath(), "administrative death must suppress a misleading organophosphate finding");
        assertEquals(false, restoredAdministrative.suspectedMyocardialInfarction(), "administrative death must not look like voluntary death");

        DowningHitRecord drowningHit = new DowningHitRecord(
                1.0F,
                "drown",
                DamageKind.UNKNOWN,
                "non_traumatic_damage",
                "none",
                "none",
                "none",
                DowningHitRecord.UNKNOWN_DISTANCE,
                400L
        );
        assertEquals(
                true,
                AutopsyReport.indicatesDrowningDeath(CollapseReason.HYPOXIA, drowningHit),
                "hypoxic collapse caused by drowning damage must produce the detailed drowning conclusion"
        );
        assertEquals(
                false,
                AutopsyReport.indicatesDrowningDeath(CollapseReason.HYPOXIA, hiddenHit),
                "hypoxic collapse without drowning damage must not be mislabeled as drowning"
        );
        assertEquals(
                false,
                AutopsyReport.indicatesDrowningDeath(CollapseReason.OVERDOSE, drowningHit),
                "a non-hypoxic collapse reason must not be mislabeled as drowning"
        );
        assertEquals(
                true,
                AutopsyReport.indicatesOrganophosphatePoisoningDeath(CollapseReason.ORGANOPHOSPHATE_POISONING),
                "organophosphate collapse must produce the detailed poisoning conclusion"
        );
        assertEquals(
                false,
                AutopsyReport.indicatesOrganophosphatePoisoningDeath(CollapseReason.HYPOXIA),
                "a non-organophosphate collapse reason must not be mislabeled as poisoning"
        );
    }

    private static void verifyEmptyCorpseLifecycle() {
        assertEquals(
                false,
                CorpseServerConfig.DEFAULT_COLLISION_ENABLED,
                "corpse collision must default to disabled"
        );
        assertEquals(
                false,
                CorpseServerConfig.DEFAULT_CORPSE_ENTITY_PUSHING_ENABLED,
                "corpse entity pushing must default to disabled"
        );
        assertEquals(
                false,
                CorpseServerConfig.DEFAULT_DOWNED_COLLISION_ENABLED,
                "downed-player collision must default to disabled"
        );
        assertEquals(
                false,
                CorpseServerConfig.DEFAULT_DOWNED_ENTITY_PUSHING_ENABLED,
                "downed-player entity pushing must default to disabled"
        );
        assertEquals(
                true,
                CorpseServerConfig.DEFAULT_EMPTY_REMOVAL_ENABLED,
                "empty corpse removal must default to enabled"
        );
        assertEquals(
                15,
                CorpseServerConfig.DEFAULT_EMPTY_LIFETIME_MINUTES,
                "empty corpses must default to a fifteen-minute lifetime"
        );

        long lifetimeTicks = 15L * 60L * 20L;
        EmptyCorpseLifecycle.Progression started = EmptyCorpseLifecycle.advance(
                true,
                true,
                EmptyCorpseLifecycle.NOT_EMPTY,
                1_000L,
                lifetimeTicks
        );
        assertEquals(1_000L, started.emptySinceGameTime(), "an empty corpse must start its timer once");
        assertEquals(false, started.shouldRemove(), "a newly empty corpse must remain");

        EmptyCorpseLifecycle.Progression beforeDeadline = EmptyCorpseLifecycle.advance(
                true,
                true,
                started.emptySinceGameTime(),
                1_000L + lifetimeTicks - 1L,
                lifetimeTicks
        );
        assertEquals(false, beforeDeadline.shouldRemove(), "the lifetime must use an inclusive deadline");
        EmptyCorpseLifecycle.Progression atDeadline = EmptyCorpseLifecycle.advance(
                true,
                true,
                started.emptySinceGameTime(),
                1_000L + lifetimeTicks,
                lifetimeTicks
        );
        assertEquals(true, atDeadline.shouldRemove(), "an empty corpse must be removed at the deadline");

        EmptyCorpseLifecycle.Progression refilled = EmptyCorpseLifecycle.advance(
                true,
                false,
                started.emptySinceGameTime(),
                2_000L,
                lifetimeTicks
        );
        assertEquals(
                EmptyCorpseLifecycle.NOT_EMPTY,
                refilled.emptySinceGameTime(),
                "refilling a corpse must cancel the empty timer"
        );
        EmptyCorpseLifecycle.Progression disabled = EmptyCorpseLifecycle.advance(
                false,
                true,
                started.emptySinceGameTime(),
                2_000L,
                lifetimeTicks
        );
        assertEquals(
                EmptyCorpseLifecycle.NOT_EMPTY,
                disabled.emptySinceGameTime(),
                "disabling cleanup must cancel an existing empty timer"
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
        floor.advanceBodyProgression(7_600L);
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
        assertEquals(1_480L, restored.nextPainRecoveryGameTime(), "offline time must shift pain recovery forward");
        restored.advanceBodyProgression(1_479L);
        assertFloatEquals(0.25F, restored.basePain(), "offline time must not grant pain recovery");
        BodyProgressionResult recovery = restored.advanceBodyProgression(1_480L);
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

        assertFloatEquals(0.0F, wound.advanceInfection(WoundInstance.INFECTION_ONSET_DELAY_TICKS - 1L), "clean contamination must wait a full two minutes");
        assertFloatEquals(0.5F, wound.advanceInfection(WoundInstance.INFECTION_ONSET_DELAY_TICKS), "an untreated wound must gain 0.5 contamination after two minutes");
        assertFloatEquals(0.5F, wound.infectionContribution(), "contamination must be attributed to the wound");
        assertFloatEquals(0.5F, wound.advanceInfection(WoundInstance.INFECTION_SPREAD_INTERVAL_TICKS), "a needs-debridement wound must receive its first deep infection pulse after three minutes");
        assertFloatEquals(1.0F, wound.infectionContribution(), "the first two contamination pulses must remain below the visible threshold");
        assertEquals(false, wound.isInfected(), "the wound infection tag must remain hidden below 1.5 contribution");
        assertFloatEquals(0.5F, wound.advanceInfection(WoundInstance.INFECTION_SPREAD_INTERVAL_TICKS * 2L), "deep infection must continue every three minutes");
        assertFloatEquals(1.5F, wound.infectionContribution(), "the wound must retain cumulative local infection contribution");
        assertEquals(true, wound.isInfected(), "the wound infection tag must appear at 1.5 contribution");

        long infectedAt = WoundInstance.INFECTION_SPREAD_INTERVAL_TICKS * 2L;
        state.setInfectionForDebug(1.5F, infectedAt);
        assertEquals(true, state.applyWoundPacking(wound.id(), infectedAt), "a bleeding wound must allow packing before debridement");
        assertEquals(false, wound.canDebride(), "wound packing must block debridement");
        assertEquals(true, state.removeWoundPacking(wound.id(), infectedAt), "packing must be removable before debridement");
        assertEquals(true, state.debrideWound(wound.id()), "an uncovered unpacked wound must allow debridement");
        assertEquals(false, wound.woundTags().contains(WoundTag.NEEDS_DEBRIDEMENT_1), "debridement must clear the requirement tag");
        assertEquals(false, wound.isInfected(), "debridement must clear the wound infection tag");
        assertEquals(true, wound.isDebrided(), "debridement must leave a visible completion tag");
        assertFloatEquals(0.0F, wound.infectionContribution(), "debridement must clear the wound's local infection source");

        BodyState restored = new BodyState();
        restored.deserializeNBT(state.serializeNBT());
        assertFloatEquals(1.5F, restored.infection(), "infection must survive save and reload");
        assertEquals(true, restored.wounds().get(0).isDebrided(), "debridement state must survive save and reload");
        assertEquals(false, restored.wounds().get(0).isInfected(), "a reloaded debrided wound must remain uninfected");
        assertFloatEquals(0.0F, restored.wounds().get(0).infectionContribution(), "cleared local infection contribution must survive save and reload");

        long firstSystemicSettlement = infectedAt + BodyState.INFECTION_SETTLEMENT_INTERVAL_TICKS;
        BodyProgressionResult afterDebridement = restored.advanceBodyProgression(firstSystemicSettlement, false, 20);
        assertFloatEquals(-0.25F, afterDebridement.infectionChange(), "food above eighteen must reduce systemic infection by 0.25 per minute");
        assertFloatEquals(1.25F, restored.infection(), "debridement must stop wound growth while nutrition resolves existing infection");
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

    private static void verifyDisinfectionAndHealingFloor() {
        BodyState deepState = new BodyState();
        WoundInstance deep = requireWound(deepState.applyDamage(WoundType.EXPLOSION, 8.0F, 0L));
        deep.advanceNaturalHealing(10_000.0F);
        assertFloatEquals(40.0F, deep.healingProgress(), "a needs-debridement wound must not heal below H=40");

        long infectedAt = WoundInstance.INFECTION_SPREAD_INTERVAL_TICKS * 2L;
        deep.advanceInfection(infectedAt);
        assertFloatEquals(1.5F, deep.infectionContribution(), "deep wound setup must reach visible infection");
        assertEquals(true, deepState.disinfectWound(deep.id(), WoundDisinfectant.POVIDONE_IODINE, infectedAt), "an uncovered deep wound must accept povidone iodine");
        assertFloatEquals(0.5F, deep.infectionContribution(), "povidone iodine must reduce a deep wound to 0.5 local infection");
        assertEquals(false, deep.isInfected(), "povidone iodine must clear the visible infection tag below its threshold");
        assertEquals(true, deep.woundTags().contains(WoundTag.NEEDS_DEBRIDEMENT_1), "disinfection must not replace deep debridement");
        assertEquals(true, deep.woundTags().contains(WoundTag.DISINFECTED), "povidone iodine must grant disinfected protection");
        assertFloatEquals(0.0F, deep.advanceInfection(infectedAt + 5L * 60L * 20L - 1L), "infection must not worsen during five-minute iodine protection");

        BodyState iodineRestored = new BodyState();
        iodineRestored.deserializeNBT(deepState.serializeNBT());
        WoundInstance restoredDeep = iodineRestored.wound(deep.id())
                .orElseThrow(() -> new AssertionError("disinfected wound must survive save and reload"));
        assertEquals(deep.disinfectionEndGameTime(), restoredDeep.disinfectionEndGameTime(), "disinfection deadline must survive save and reload");

        BodyState shallowState = new BodyState();
        WoundInstance shallow = requireWound(shallowState.applyDamage(WoundType.SHARP, 0.5F, 0L));
        shallow.advanceInfection(WoundInstance.INFECTION_ONSET_DELAY_TICKS
                + WoundInstance.INFECTION_SPREAD_INTERVAL_TICKS * 2L);
        assertEquals(true, shallow.isInfected(), "an untreated shallow wound must eventually become infected");
        shallow.advanceNaturalHealing(10_000.0F);
        assertFloatEquals(40.0F, shallow.healingProgress(), "an infected shallow wound must not heal below H=40");
        assertEquals(true, shallowState.disinfectWound(shallow.id(), WoundDisinfectant.POVIDONE_IODINE, 10_000L), "iodine must disinfect an uncovered shallow wound");
        assertFloatEquals(0.0F, shallow.infectionContribution(), "iodine must clear shallow local infection to zero");
        shallow.advanceNaturalHealing(10_000.0F);
        assertFloatEquals(0.0F, shallow.healingProgress(), "a disinfected shallow wound must resume complete healing");

        BodyState alcoholState = new BodyState();
        WoundInstance alcoholWound = requireWound(alcoholState.applyDamage(WoundType.BLUNT, 4.0F, 0L));
        assertEquals(true, alcoholState.disinfectWound(alcoholWound.id(), WoundDisinfectant.MEDICAL_ALCOHOL, 0L), "an uncovered wound must accept medical alcohol");
        assertEquals(true, alcoholWound.woundTags().contains(WoundTag.ALCOHOL_PAIN_2), "medical alcohol must add temporary pain two");
        assertEquals(false, alcoholWound.advanceNaturalHealing(10.0F, false, 200L), "medical alcohol must pause wound healing for one minute");
        alcoholWound.expireTransientTags(WoundInstance.ALCOHOL_IRRITATION_DURATION_TICKS);
        assertEquals(false, alcoholWound.woundTags().contains(WoundTag.ALCOHOL_PAIN_2), "alcohol pain must expire after one minute");
        assertEquals(true, alcoholWound.advanceNaturalHealing(10.0F, false, WoundInstance.ALCOHOL_IRRITATION_DURATION_TICKS), "healing must resume after alcohol irritation ends");
    }

    private static void verifySystemicInfectionSettlement() {
        BodyState wellFed = stateWithInfection(1.5F);
        wellFed.advanceBodyProgression(0L, false, 19);
        BodyProgressionResult recovery = wellFed.advanceBodyProgression(
                BodyState.INFECTION_SETTLEMENT_INTERVAL_TICKS,
                false,
                19
        );
        assertFloatEquals(-0.25F, recovery.infectionChange(), "food above eighteen must reduce infection by 0.25 per minute");
        assertFloatEquals(1.25F, wellFed.infection(), "systemic recovery must retain fractional infection values");
        assertEquals(
                BodyState.INFECTION_SETTLEMENT_INTERVAL_TICKS * 2L,
                wellFed.nextInfectionSettlementGameTime(),
                "remaining infection must keep the settlement timer active"
        );

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

        septic.setInfectionForDebug(4.0F, BodyState.INFECTION_SETTLEMENT_INTERVAL_TICKS);
        assertEquals(false, septic.advanceAwakening(20.0F, BodyState.INFECTION_SETTLEMENT_INTERVAL_TICKS + 1L).started(), "sepsis must not awaken at infection four");
        septic.setInfectionForDebug(3.9F, BodyState.INFECTION_SETTLEMENT_INTERVAL_TICKS + 2L);
        assertEquals(true, septic.advanceAwakening(20.0F, BodyState.INFECTION_SETTLEMENT_INTERVAL_TICKS + 3L).started(), "sepsis must enter awakening below infection four");
    }

    private static void verifyAntibioticInfectionControl() {
        BodyState amoxicillin = stateWithInfection(4.0F);
        assertEquals(true, amoxicillin.applyMedication(MedicationType.AMOXICILLIN, 0L, false), "the first amoxicillin layer must apply");
        assertEquals(true, amoxicillin.applyMedication(MedicationType.AMOXICILLIN, 1L, false), "a second amoxicillin dose must queue behind the first");
        assertEquals(2, amoxicillin.activeAmoxicillinDoseCount(), "queued antibiotic doses must retain independent concentration layers");
        assertFloatEquals(10.0F, amoxicillin.bloodDrugConcentration(), "two queued amoxicillin doses must add ten drug concentration immediately");
        assertEquals(3L * 60L * 20L, amoxicillin.activeDrugDoses().get(0).expiresGameTime(), "the first antibiotic dose must keep its original expiry");
        assertEquals(6L * 60L * 20L, amoxicillin.activeDrugDoses().get(1).expiresGameTime(), "the second antibiotic dose must begin after the first and expire three minutes later");
        amoxicillin.advanceBodyProgression(BodyState.INFECTION_SETTLEMENT_INTERVAL_TICKS, false, 0);
        assertFloatEquals(4.0F, amoxicillin.infection(), "amoxicillin must stop systemic infection growth");
        amoxicillin.advanceBodyProgression(3L * 60L * 20L, false, 0);
        assertFloatEquals(5.0F, amoxicillin.bloodDrugConcentration(), "the first dose concentration must disappear when its own stage ends");
        assertFloatEquals(4.0F, amoxicillin.infection(), "the queued second dose must continue suppressing infection growth");
        amoxicillin.advanceBodyProgression(
                6L * 60L * 20L,
                false,
                0
        );
        assertFloatEquals(0.0F, amoxicillin.bloodDrugConcentration(), "the second dose concentration must disappear after its queued stage");
        assertFloatEquals(4.0F, amoxicillin.infection(), "final antibiotic expiry must reset rather than immediately settle infection");
        amoxicillin.advanceBodyProgression(
                6L * 60L * 20L + BodyState.INFECTION_SETTLEMENT_INTERVAL_TICKS,
                false,
                0
        );
        assertFloatEquals(4.5F, amoxicillin.infection(), "systemic infection growth must resume one minute after the queued doses end");

        BodyState ceftriaxone = stateWithInfection(5.0F);
        assertEquals(true, ceftriaxone.applyMedication(MedicationType.CEFTRIAXONE, 0L, false), "the first ceftriaxone layer must apply");
        assertEquals(true, ceftriaxone.applyMedication(MedicationType.CEFTRIAXONE, 1L, false), "a second ceftriaxone dose must queue behind the first");
        assertFloatEquals(10.0F, ceftriaxone.bloodDrugConcentration(), "queued ceftriaxone doses must each add five drug concentration");
        assertEquals(
                BodyState.CEFTRIAXONE_INFECTION_REDUCTION_INTERVAL_TICKS,
                ceftriaxone.nextCeftriaxoneInfectionReductionGameTime(),
                "ceftriaxone must schedule its first reduction after forty seconds"
        );

        BodyState restored = new BodyState();
        restored.deserializeNBT(ceftriaxone.serializeNBT());
        assertEquals(
                ceftriaxone.nextCeftriaxoneInfectionReductionGameTime(),
                restored.nextCeftriaxoneInfectionReductionGameTime(),
                "ceftriaxone reduction timer must survive save and reload"
        );
        restored.advanceBodyProgression(BodyState.CEFTRIAXONE_INFECTION_REDUCTION_INTERVAL_TICKS, false, 0);
        float reduction = 5.0F - restored.infection();
        assertEquals(true, reduction >= 1.5F - EPSILON && reduction <= 2.0F + EPSILON, "ceftriaxone must reduce systemic infection by 1.5 to 2.0");
        assertFloatEquals(Math.round(reduction * 10.0F) / 10.0F, reduction, "ceftriaxone reduction must use one decimal place");
    }

    private static void verifySkillKnowledgeRoundTrip() {
        BodyState state = new BodyState();
        assertEquals(false, state.hasFirstAidSkill(), "new body state must not know first aid");
        assertEquals(true, state.unlockFirstAidSkill(), "the first-aid skill book must unlock first aid once");
        assertEquals(false, state.unlockFirstAidSkill(), "reusing a first-aid skill book must not unlock twice");
        assertEquals(false, state.hasSurgerySkill(), "new body state must not know surgery");
        assertEquals(true, state.unlockSurgerySkill(), "the surgery skill book must unlock surgery once");
        assertEquals(false, state.unlockSurgerySkill(), "reusing a surgery skill book must not unlock twice");
        assertEquals(false, state.hasForensicSkill(), "new body state must not know forensic medicine");
        assertEquals(true, state.unlockForensicSkill(), "the forensic skill book must unlock forensic medicine once");
        assertEquals(false, state.unlockForensicSkill(), "reusing a forensic skill book must not unlock twice");

        BodyState restored = new BodyState();
        restored.deserializeNBT(state.serializeNBT());
        assertEquals(true, restored.hasFirstAidSkill(), "first-aid skill must survive save and reload");
        assertEquals(true, restored.hasSurgerySkill(), "surgery skill must survive save and reload");
        assertEquals(true, restored.hasForensicSkill(), "forensic skill must survive save and reload");

        BodyState deathClone = new BodyState();
        deathClone.copyPersistentKnowledgeFrom(restored);
        assertEquals(true, deathClone.hasFirstAidSkill(), "permanent first-aid knowledge must survive a death clone");
        assertEquals(true, deathClone.hasSurgerySkill(), "permanent surgery knowledge must survive a death clone");
        assertEquals(true, deathClone.hasForensicSkill(), "permanent forensic knowledge must survive a death clone");

        restored.resetAllForDebug();
        assertEquals(false, restored.hasFirstAidSkill(), "the full debug reset must clear learned first-aid skill");
        assertEquals(false, restored.hasSurgerySkill(), "the full debug reset must clear learned surgery skill");
        assertEquals(false, restored.hasForensicSkill(), "the full debug reset must clear learned forensic skill");
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

    private static void verifyTimingQteHalfOpenRanges() {
        TimingQteSnapshot snapshot = new TimingQteSnapshot(
                1,
                100L,
                100,
                0.20F,
                0.30F,
                0.50F
        );
        assertEquals(
                TimingQteResult.EARLY_FAILURE,
                snapshot.classifyPress(19.999F),
                "pressing before the perfect arc must fail immediately"
        );
        assertEquals(
                TimingQteResult.PERFECT,
                snapshot.classifyPress(20.0F),
                "the perfect arc must include its left boundary"
        );
        assertEquals(
                TimingQteResult.PERFECT,
                snapshot.classifyPress(29.999F),
                "the perfect arc must remain active before the normal boundary"
        );
        assertEquals(
                TimingQteResult.SUCCESS,
                snapshot.classifyPress(30.0F),
                "the normal arc must include its left boundary"
        );
        assertEquals(
                TimingQteResult.SUCCESS,
                snapshot.classifyPress(49.999F),
                "the normal arc must remain active before its right boundary"
        );
        assertEquals(
                TimingQteResult.MISSED_FAILURE,
                snapshot.classifyPress(50.0F),
                "reaching the right boundary must count as a miss"
        );
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

    private static void assertBetween(long minimum, long maximum, long actual, String message) {
        if (actual < minimum || actual > maximum) {
            throw new AssertionError(
                    message + ": expected between " + minimum + " and " + maximum + ", actual=" + actual
            );
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }
}
