package com.swampd.superficialtrauma.common.body;

import com.swampd.superficialtrauma.common.medication.MedicationRoute;
import com.swampd.superficialtrauma.common.medication.MedicationType;
import com.swampd.superficialtrauma.common.qte.MedicalTimingQte;
import com.swampd.superficialtrauma.common.qte.TimingQteResult;
import com.swampd.superficialtrauma.common.treatment.TreatmentAction;
import com.swampd.superficialtrauma.common.treatment.TreatmentProcedure;
import com.swampd.superficialtrauma.common.treatment.TreatmentSession;
import com.swampd.superficialtrauma.common.wound.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

public final class MedicalRevisionTest {
    private MedicalRevisionTest() { }

    public static void run() {
        graftsAndPersistence();
        infectionFloorAndReinjury();
        fatigueSources();
        movementAndOralRules();
        sharedQte();
        System.out.println("Medical revision tests passed: grafts, reinjury, fatigue, walking, oral routes, shared QTE.");
    }

    private static void graftsAndPersistence() {
        for (WoundType type : new WoundType[]{WoundType.BURN, WoundType.EXPLOSION, WoundType.FROSTBITE, WoundType.CRUSH}) {
            BodyState state = new BodyState();
            WoundInstance wound = state.applyDamage(type, 16, 0).wound();
            check(wound.hasNecrosis() && wound.canSkinGraft(), "severe necrotic wound must accept graft " + type);
            check(TreatmentProcedure.SKIN_GRAFT.isApplicable(wound, TreatmentAction.APPLY), "graft applicable " + type);
            state.debrideWound(wound.id());
            check(state.skinGraftWound(wound.id(), 1000), "graft must complete " + type);
            check(!wound.hasNecrosis(), "graft removes necrosis, not just downgrades it " + type);
            equal(0, state.necrosisMaximumHealthReduction(), "max-health penalty released");
            check(!wound.canSkinGraft(), "cannot consume dermis twice on healed necrosis");
            BodyState restoredState = new BodyState();
            restoredState.deserializeNBT(state.serializeNBT());
            WoundInstance restored = restoredState.wounds().get(0);
            check(restored.skinGrafted() && !restored.hasNecrosis(), "graft survives full BodyState save/load " + type);
            equal(0, restored.minimumHealingProgressWithoutSkinGraft(), "graft releases floor " + type);
            check(restored.baseHealingPerSecond() > 0, "graft restores healing route " + type);
            restored.advanceNaturalHealing(2000, false, 41000);
            check(restored.isHealed(), "grafted, debrided tissue can fully heal " + type);
        }

        WoundInstance dirty = WoundInstance.create(WoundType.FROSTBITE, 16, 0, 400);
        check(dirty.skinGraft(1000), "graft can remove necrosis without silently clearing infection");
        dirty.advanceNaturalHealing(2000, false, 41000);
        equal(40, dirty.healingProgress(), "graft does not clear debridement floor");
        check(dirty.woundTags().contains(WoundTag.NEEDS_DEBRIDEMENT_1), "deep cleaning remains necessary");

        WoundInstance tourniquet = WoundInstance.create(WoundType.SHARP, 6, 0, 400);
        tourniquet.applyTourniquet(0);
        tourniquet.advanceTourniquet(WoundInstance.TOURNIQUET_NECROSIS_TWO_TICKS);
        check(tourniquet.hasNecrosis(), "prolonged tourniquet causes necrosis");
        tourniquet.skinGraft(12000);
        tourniquet.advanceTourniquet(12020);
        check(!tourniquet.hasNecrosis(), "old exposure must not instantly restore excised necrosis");
        tourniquet.advanceTourniquet(18000);
        check(tourniquet.hasNecrosis(), "leaving a tourniquet on still causes new necrosis after five minutes");
    }

    private static void infectionFloorAndReinjury() {
        WoundInstance blunt = WoundInstance.createBlunt(4, 0, 400);
        blunt.advanceInfection(10000);
        check(blunt.isInfected(), "test setup is infected");
        blunt.advanceNaturalHealing(200);
        equal(40, blunt.healingProgress(), "infected natural healing stops at 40");
        check(blunt.applyIcePack(), "infected contusion can be cooled");
        equal(40, blunt.healingProgress(), "ice may not bypass infection floor");
        check(!blunt.woundTags().contains(WoundTag.PAIN_1), "ice still relieves local pain");

        WoundInstance deep = WoundInstance.create(WoundType.EXPLOSION, 8, 0, 400);
        deep.debride();
        equal(0, deep.advanceInfection(10000), "debrided uninjured wound stays clean indefinitely");
        deep.applyCovering(WoundCovering.SELF_ADHESIVE_BANDAGE, 200);
        deep.applyWoundPacking(200);
        deep.advanceNaturalHealing(40);
        deep.addAccumulatedDamage(1, 300);
        equal(100, deep.healingProgress(), "same-window injury resets H even without upgrading");
        check(!deep.isDebrided() && !deep.covering().isApplied() && !deep.woundPackingApplied(), "reinjury removes protections");
        equal(300 + WoundInstance.INFECTION_ONSET_DELAY_TICKS, deep.infectionOnsetGameTime(), "new contamination clock starts at reinjury");
        equal(0.5, deep.advanceInfection(deep.infectionOnsetGameTime()), "reopened tissue contaminates again");

        WoundInstance disinfected = WoundInstance.create(WoundType.PUNCTURE, 4, 0, 400);
        disinfected.disinfect(WoundDisinfectant.POVIDONE_IODINE, 0);
        disinfected.addAccumulatedDamage(1, 100);
        equal(-1, disinfected.disinfectionEndGameTime(), "hit removes iodine protection");
        equal(2500, disinfected.infectionOnsetGameTime(), "old five-minute protection cannot remain in future infection deadline");

        WoundInstance upgraded = WoundInstance.create(WoundType.BURN, 4, 0, 400);
        upgraded.disinfect(WoundDisinfectant.POVIDONE_IODINE, 0);
        upgraded.addAccumulatedDamage(2, 200);
        equal(200 + WoundInstance.INFECTION_SPREAD_INTERVAL_TICKS, upgraded.nextInfectionSpreadGameTime(),
                "newly deep wound restarts spread clock at the hit, not original creation");

        WoundInstance shallow = WoundInstance.create(WoundType.SHARP, 6, 0, 400);
        shallow.advanceInfection(10000);
        check(shallow.isInfected(), "shallow infection setup");
        shallow.disinfect(WoundDisinfectant.POVIDONE_IODINE, 10000);
        check(!shallow.isInfected(), "disinfection clears shallow infected tag");
        equal(0, shallow.infectionContribution(), "disinfection clears shallow local contribution");

        BodyState shot = new BodyState();
        WoundInstance gunshot = shot.applyGunshotDamage(WoundType.GUNSHOT_LOW_VELOCITY, 4, 0, 6, true, 0).wound();
        shot.debrideWound(gunshot.id());
        gunshot.advanceNaturalHealing(20);
        shot.applyGunshotDamage(WoundType.GUNSHOT_LOW_VELOCITY, 4, 0, 6, true, 200);
        equal(100, gunshot.healingProgress(), "repeat shot resets H");
        check(!gunshot.isDebrided() && gunshot.woundTags().contains(WoundTag.NEEDS_DEBRIDEMENT_1), "repeat shot applies new contamination roll");

        BodyState outsideWindow = new BodyState();
        WoundInstance old = outsideWindow.applyDamage(WoundType.EXPLOSION, 8, 0).wound();
        outsideWindow.debrideWound(old.id());
        WoundInstance fresh = outsideWindow.applyDamage(WoundType.EXPLOSION, 8, 401).wound();
        check(!old.id().equals(fresh.id()) && old.isDebrided(), "outside-window new wound preserves old clean instance");
    }

    private static void fatigueSources() {
        BodyState state = new BodyState();
        equal(0, state.fatigueLevel(), "healthy baseline");
        state.applyBluntDamage(4, 0);
        equal(1, state.fatigueLevel(), "one old slowness tag is one fatigue");
        state.applyBluntDamage(4, 401);
        equal(2, state.fatigueLevel(), "independent contusions stack");
        state.applyDamage(WoundType.CRUSH, 12, 500);
        equal(3, state.fatigueLevel(), "necrosis II adds one fatigue");
        state.applyMedication(MedicationType.MORPHINE, 600);
        equal(4, state.fatigueLevel(), "opioid equivalent adds one fatigue");
        state.applyMedication(MedicationType.EPINEPHRINE, 600);
        equal(3, state.fatigueLevel(), "epinephrine subtracts one before cap");
        state.applyMedication(MedicationType.REMIFENTANIL, 600);
        equal(4, state.fatigueLevel(), "fatigue caps at four");
        state.applyMedication(MedicationType.NALOXONE, 700);
        equal(2, state.fatigueLevel(), "naloxone clears opioid fatigue only");
        BodyState restored = new BodyState();
        restored.deserializeNBT(state.serializeNBT());
        equal(2, restored.fatigueLevel(), "fatigue derives correctly after reload");

        BodyState remi = new BodyState();
        remi.applyMedication(MedicationType.REMIFENTANIL, 0);
        equal(3, remi.fatigueLevel(), "one remifentanil is three opioid equivalents");
        remi.advanceBodyProgression(1200);
        equal(0, remi.fatigueLevel(), "expired opioid fatigue disappears");

        BodyState poison = new BodyState();
        poison.exposeToOrganophosphate(0);
        poison.advanceBodyProgression(poison.organophosphateNextProgressionGameTime());
        equal(1, poison.fatigueLevel(), "poisoning's former slowness II contributes one fatigue");
        poison.applyMedication(MedicationType.ATROPINE_SULFATE, 1500);
        equal(0, poison.fatigueLevel(), "atropine removes poisoning fatigue");

        CompoundTag legacy = WoundInstance.createBlunt(4, 0, 400).serializeNBT();
        ListTag tags = new ListTag();
        tags.add(StringTag.valueOf("slowness_1"));
        legacy.put("WoundTags", tags);
        check(WoundInstance.deserializeNBT(legacy).woundTags().contains(WoundTag.FATIGUE_1), "old slowness saves migrate");
        int[] slow = {0, 1, 1, 2, 2, 2};
        int[] weak = {0, 0, 1, 1, 2, 2};
        for (int level = 0; level < slow.length; level++) {
            equal(slow[level], FatigueEffect.slownessLevel(level), "slowness tier " + level);
            equal(weak[level], FatigueEffect.weaknessLevel(level), "weakness tier " + level);
        }
    }

    private static void movementAndOralRules() {
        check(!WoundMovementRules.isMoving(0, 0, 0, 0, -0.0784, false), "standing gravity must not trigger bleeding");
        check(WoundMovementRules.isMoving(0.02, 0, 0, 0, 0, false), "walking counts");
        check(WoundMovementRules.isMoving(0.005, 0, 0, 0, 0, false), "sneaking counts");
        check(WoundMovementRules.isMoving(0, 0.1, 0, 0, 0.1, false), "jumping counts");
        for (MedicationType type : MedicationType.values()) {
            boolean injectable = type.route() == MedicationRoute.INJECTION;
            check(type.route().allowsPatient(true, true), "self conscious medication " + type);
            check(type.route().allowsPatient(false, true) == injectable, "other-conscious route " + type);
            check(type.route().allowsPatient(false, false) == injectable, "other-downed route " + type);
        }
    }

    private static void sharedQte() {
        equal(1000, TreatmentProcedure.SKIN_GRAFT.durationTicks(), "graft starts with 50 seconds");
        equal(26, MedicalTimingQte.DEFINITION.sweepDurationTicks(), "shared 1.3-second sweep");
        equal(0.13, MedicalTimingQte.CHANCE_PER_SECOND, "same QTE probability");
        equal(1100, MedicalTimingQte.adjustedDeadline(1000, 200, TimingQteResult.EARLY_FAILURE), "failure adds 5 seconds");
        equal(960, MedicalTimingQte.adjustedDeadline(1000, 200, TimingQteResult.PERFECT), "perfect removes 2 seconds");
        equal(1000, MedicalTimingQte.adjustedDeadline(1000, 200, TimingQteResult.SUCCESS), "normal no reward");
        equal(980, MedicalTimingQte.adjustedDeadline(1000, 980, TimingQteResult.PERFECT), "cannot complete before current tick");
        TreatmentSession initial = new TreatmentSession(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                TreatmentProcedure.SKIN_GRAFT, TreatmentAction.APPLY, 0, 1000, Vec3.ZERO, Vec3.ZERO);
        TreatmentSession changed = initial.withDeadline(1100);
        equal(1100, changed.endsGameTime(), "server session can resync new deadline");
        check(changed.woundId().equals(initial.woundId()), "QTE does not switch wound");
    }

    private static void equal(double expected, double actual, String message) {
        check(Math.abs(expected - actual) < 0.0001, message + ": expected=" + expected + " actual=" + actual);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
