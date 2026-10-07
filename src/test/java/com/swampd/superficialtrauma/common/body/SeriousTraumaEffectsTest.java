package com.swampd.superficialtrauma.common.body;

import com.swampd.superficialtrauma.common.damage.GunshotRegion;
import com.swampd.superficialtrauma.common.medication.MedicationType;
import com.swampd.superficialtrauma.common.treatment.*;
import com.swampd.superficialtrauma.common.wound.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SeriousTraumaEffectsTest {
    private BodyState chest(float distress) {
        var state = new BodyState();
        state.configureSeriousTrauma(true, 0);
        state.resumeBodyProgression(0);
        var hit = state.applyGunshotDamage(WoundType.GUNSHOT_HIGH_VELOCITY, 16, 0, 10, false, 0);
        state.recordGunshotLocations(hit, 16, 0, 16, 0);
        state.setRespiratoryDistressForDebug(distress, 0);
        return state;
    }
    @Test void openPneumothoraxGrowsAwakeAndDownedAndBlocksEpinephrineRecovery() {
        var state = chest(2);
        state.applyMedication(MedicationType.EPINEPHRINE, 0, false);
        state.advanceBodyProgression(20);
        assertEquals(2.2, state.respiratoryDistress(), .0001);
        state.incapacitate(CollapseReason.HYPOXIA, 20);
        assertFalse(state.advanceAssistedBreathing(20, 1, 40, true));
        state.advanceBodyProgression(40);
        assertEquals(2.4, state.respiratoryDistress(), .0001);
        assertTrue(state.hasOpenPneumothorax());
    }
    @Test void naloxoneStillRemovesOpioidToxicityWhileOpenPneumothoraxRemains() {
        var state = chest(3);
        state.applyMedication(MedicationType.MORPHINE, 0, false);
        assertEquals(5, state.respiratoryDistress(), .0001);
        assertTrue(state.applyMedication(MedicationType.NALOXONE, 1, false));
        assertEquals(3, state.respiratoryDistress(), .0001);
        state.advanceBodyProgression(20);
        assertEquals(3.2, state.respiratoryDistress(), .0001);
        assertTrue(state.hasOpenPneumothorax());
    }
    @Test void openPneumothoraxEventuallyCausesHypoxiaAndCapsAtTwenty() {
        var state = chest(0);
        state.advanceBodyProgression(2000);
        assertEquals(20, state.respiratoryDistress());
        assertEquals(CollapseReason.HYPOXIA, state.collapseReason());
        state.advanceBodyProgression(2020);
        assertEquals(20, state.respiratoryDistress());
    }
    @Test void capturedFloorWinsOverDetoxButLowFloorStillAllowsHypoxiaAwakening() {
        var state = chest(0); var wound = state.wounds().getFirst();
        state.applyMedication(MedicationType.MORPHINE, 0, false);
        state.applyChestSeal(wound.id(), 1);
        state.applyMedication(MedicationType.NALOXONE, 2, false);
        assertEquals(0, state.activeDoseCount(MedicationType.MORPHINE));
        assertEquals(2, state.respiratoryDistress());
        state.incapacitate(CollapseReason.HYPOXIA, 3);
        assertTrue(state.advanceAwakening(11, 4).started());
    }
    @Test void sealingCapturesFloorOnceAndSurgeryDoesNotEraseRespiratoryBurden() {
        for (float original : new float[]{-2, 2, 10, 14}) {
            var state = chest(original);
            var wound = state.wounds().getFirst();
            assertTrue(state.applyChestSeal(wound.id(), 0));
            float floor = Math.min(10, state.respiratoryDistress());
            state.incapacitate(CollapseReason.HYPOXIA, 0);
            assertTrue(state.advanceAssistedBreathing(1000, 2, 1, true));
            assertEquals(floor, state.respiratoryDistress(), .0001);
            state.configureSeriousTrauma(true, 2);
            assertEquals(floor, state.seriousTrauma().respiratoryFloor(), .0001);
            var copy = new BodyState(); copy.deserializeNBT(state.serializeNBT());
            assertEquals(floor, copy.respiratoryDistress(), .0001);
            assertTrue(copy.wounds().getFirst().chestSealApplied());
            assertTrue(copy.repairPneumothorax(wound.id(), 2));
            assertEquals(floor, copy.respiratoryDistress(), .0001);
            assertFalse(copy.seriousTrauma().hasPneumothorax());
            assertFalse(copy.wounds().getFirst().pneumothoraxWound());
            assertEquals(0, copy.seriousTrauma().damage(GunshotRegion.CHEST));
        }
    }
    @Test void packingAndReinforcementDoNotRecaptureFloorAndRemovalReopens() {
        var state = chest(2); var wound = state.wounds().getFirst();
        assertTrue(state.applyWoundPacking(wound.id(), 0));
        state.addRespiratoryDistress(6);
        assertTrue(state.applyCovering(wound.id(), WoundCovering.SELF_ADHESIVE_BANDAGE, 1));
        assertEquals(2, state.seriousTrauma().respiratoryFloor());
        assertTrue(state.removeWoundPacking(wound.id(), 2));
        assertTrue(state.hasOpenPneumothorax());
        assertTrue(state.applyChestSeal(wound.id(), 3));
        assertEquals(8, state.seriousTrauma().respiratoryFloor());
        state.applyWoundPacking(wound.id(), 4);
        state.removeWoundPacking(wound.id(), 5);
        assertTrue(state.seriousTrauma().sealed());
    }
    @Test void legHitInSameAmmoWindowDoesNotBreakChestSealButChestHitDoes() {
        var state = chest(2); var wound = state.wounds().getFirst();
        state.applyChestSeal(wound.id(), 1);
        var legs = state.applyGunshotDamage(WoundType.GUNSHOT_HIGH_VELOCITY, 3, 0, 10, false, 2);
        state.recordExternalInjury(3, 2, false);
        state.recordGunshotLocations(legs, 3, 0, 0, 2);
        assertEquals(wound.id(), legs.wound().id());
        assertTrue(wound.chestSealApplied());
        assertTrue(state.seriousTrauma().sealed());
        var chest = state.applyGunshotDamage(WoundType.GUNSHOT_HIGH_VELOCITY, 3, 0, 10, false, 3);
        state.recordGunshotLocations(chest, 3, 0, 3, 3);
        assertFalse(wound.chestSealApplied());
        assertTrue(state.hasOpenPneumothorax());
        assertEquals(1, state.seriousTrauma().conditions().size());
    }
    @Test void blockedBulletCannotBreakChestSeal() {
        var state = chest(3); var wound = state.wounds().getFirst();
        state.applyChestSeal(wound.id(), 1);
        var stopped = state.applyGunshotDamage(WoundType.GUNSHOT_HIGH_VELOCITY, 3, 14, 10, false, 2);
        state.recordExternalInjury(3, 2, false);
        state.recordGunshotLocations(stopped, 3, 0, 3, 2);
        assertTrue(wound.chestSealApplied());
    }
    @Test void instabilityHasFiveSecondThresholdAndTenSecondCooldown() {
        var instability = new PackingInstability();
        assertFalse(instability.tick(0, true));
        assertFalse(instability.tick(99, true));
        assertTrue(instability.tick(100, true));
        assertEquals(0, instability.value());
        assertEquals(300, instability.cooldownUntil());
        assertFalse(instability.externalDamage(10, 299));
        assertFalse(instability.tick(300, true));
        assertFalse(instability.tick(340, true));
        assertEquals(2, instability.value(), .0001);
        instability.tick(380, false);
        assertEquals(1, instability.value(), .0001);
        instability.tick(420, false);
        assertEquals(0, instability.value(), .0001);
        assertFalse(instability.externalDamage(1, 420));
        assertTrue(instability.externalDamage(1.01F, 420));
    }
    @Test void instabilityDropsAllUnsecuredPackingWithSingleNoticeIndependentOfConfig() {
        var state = new BodyState();
        var first = state.applyDamage(WoundType.SHARP, 12, 0).wound();
        var second = state.applyGunshotDamage(WoundType.GUNSHOT_HIGH_VELOCITY, 16, 0, 10, false, 0).wound();
        state.applyWoundPacking(first.id(), 0); state.applyWoundPacking(second.id(), 0);
        state.recordExternalInjury(2, 0, false);
        assertFalse(first.woundPackingApplied()); assertFalse(second.woundPackingApplied());
        assertTrue(state.consumePackingDropNotice()); assertFalse(state.consumePackingDropNotice());
        state.applyWoundPacking(first.id(), 1);
        state.recordExternalInjury(2, 100, false);
        assertTrue(first.woundPackingApplied());
    }
    @Test void securedPackingSurvivesWhileLeatherDoesNotSecureIt() {
        for (var covering : WoundCovering.values()) {
            var state = chest(2); var wound = state.wounds().getFirst();
            state.applyWoundPacking(wound.id(), 0);
            if (covering.isApplied()) state.applyCovering(wound.id(), covering, 0);
            state.recordExternalInjury(2, 1, false);
            boolean secured = covering != WoundCovering.NONE && covering != WoundCovering.TEMPORARY_DRESSING;
            assertEquals(secured, wound.woundPackingApplied(), covering.name());
            assertEquals(secured, state.seriousTrauma().sealed());
        }
        var state = chest(2); var wound = state.wounds().getFirst();
        state.applyWoundPacking(wound.id(), 0); state.applyChestSeal(wound.id(), 0);
        state.recordExternalInjury(10, 1, false);
        assertTrue(wound.woundPackingApplied()); assertTrue(wound.chestSealApplied());
    }
    @Test void healedHostRemainsOperableAndInfectionFloorStillWins() {
        var state = chest(0); var wound = state.wounds().getFirst();
        state.applyCovering(wound.id(), WoundCovering.SELF_ADHESIVE_BANDAGE, 0);
        wound.advanceNaturalHealing(10000, false, 1);
        assertEquals(1, wound.healingProgress()); assertFalse(wound.isHealed());
        assertTrue(TreatmentProcedure.PNEUMOTHORAX_REPAIR.isApplicable(wound, TreatmentAction.APPLY));
        var infected = chest(0).wounds().getFirst();
        infected.markNeedsDebridement(2);
        infected.advanceNaturalHealing(10000, false, 3);
        assertEquals(40, infected.healingProgress());
    }
    @Test void operationDurationsAndQteAndSkillAreShared() {
        assertEquals(60, TreatmentProcedure.CHEST_SEAL.durationTicks());
        assertFalse(TreatmentProcedure.CHEST_SEAL.usesQte());
        assertEquals(900, TreatmentProcedure.PNEUMOTHORAX_REPAIR.durationTicks());
        assertTrue(TreatmentProcedure.PNEUMOTHORAX_REPAIR.usesQte());
        assertTrue(TreatmentProcedure.PNEUMOTHORAX_REPAIR.requiresSurgerySkill());
        assertEquals(2, TreatmentProcedure.PNEUMOTHORAX_REPAIR.ingredients().size());
    }
    @Test void concussionPainIsFixedAndCanBeSuppressedAndCuresAfterQuietPeriod() {
        var state = new BodyState(); state.configureSeriousTrauma(true, 0); state.resumeBodyProgression(0);
        state.seriousTrauma().addDowningConcussion(0);
        assertEquals(2, state.pain());
        state.applyMedication(MedicationType.PARACETAMOL, 0, false);
        state.applyMedication(MedicationType.PARACETAMOL, 0, false);
        assertEquals(0, state.pain());
        state.recordExternalInjury(0, 2000, false);
        assertEquals(3600, state.seriousTrauma().concussionEndsAt());
        state.recordExternalInjury(.1F, 100, false);
        assertEquals(3700, state.seriousTrauma().concussionEndsAt());
        assertEquals(0, state.seriousTrauma().concussionStartedAt());
        state.advanceBodyProgression(3699);
        assertTrue(state.seriousTrauma().hasConcussion());
        state.advanceBodyProgression(3700);
        assertFalse(state.seriousTrauma().hasConcussion());
    }
    @Test void awakeningDurationSnapshotsConcussionOnce() {
        for (boolean concussed : new boolean[]{false, true}) {
            var state = new BodyState(); state.configureSeriousTrauma(true, 0);
            if (concussed) state.seriousTrauma().addDowningConcussion(0);
            state.incapacitate(CollapseReason.HYPOXIA, 0);
            assertTrue(state.advanceAwakening(20, 1).started());
            assertEquals(concussed ? 500 : 400, state.awakeningRemainingTicks(1));
            state.seriousTrauma().clear();
            state.advanceAwakening(20, 2);
            assertEquals(concussed ? 499 : 399, state.awakeningRemainingTicks(2));
        }
    }
    @Test void sealedHighFloorBlocksHypoxiaAwakeningUntilRepairedAndRecovered() {
        var state = chest(14); var wound = state.wounds().getFirst();
        state.applyChestSeal(wound.id(), 0); state.incapacitate(CollapseReason.HYPOXIA, 0);
        state.advanceAssistedBreathing(1000, 1, 1, true);
        assertFalse(state.advanceAwakening(20, 1).started());
        state.repairPneumothorax(wound.id(), 2);
        assertFalse(state.advanceAwakening(20, 2).started());
        state.advanceAssistedBreathing(140, 1, 3, true);
        assertTrue(state.advanceAwakening(20, 3).started());
    }
    @Test void savePauseAndConfigLifecyclePreserveTimersAndRemoveEffects() {
        var state = chest(4);
        state.seriousTrauma().addDowningConcussion(0);
        state.recordExternalInjury(2, 10, false);
        state.pauseBodyProgression(100);
        var loaded = new BodyState(); loaded.deserializeNBT(state.serializeNBT());
        loaded.resumeBodyProgression(1100);
        assertEquals(4610, loaded.seriousTrauma().concussionEndsAt());
        assertEquals(1210, loaded.packingInstability().cooldownUntil());
        loaded.configureSeriousTrauma(false, 1101);
        assertFalse(loaded.seriousTrauma().hasPneumothorax());
        assertFalse(loaded.seriousTrauma().hasConcussion());
        assertFalse(loaded.wounds().getFirst().pneumothoraxWound());
        assertEquals(1210, loaded.packingInstability().cooldownUntil());
    }
    @Test void legacyOrphanConditionDoesNotBecomeUntreatableAndWoundCapCreatesNoOrphan() {
        var state = chest(2); var tag = state.serializeNBT();
        tag.put("Wounds", new net.minecraft.nbt.ListTag());
        var copy = new BodyState(); copy.deserializeNBT(tag); copy.configureSeriousTrauma(true, 1);
        assertFalse(copy.seriousTrauma().hasPneumothorax());
        var capped = new BodyState(); capped.configureSeriousTrauma(true, 0);
        for (int i = 0; i < BodyState.MAX_WOUNDS; i++) capped.applyDamage(WoundType.SHARP, 12, i * 401);
        var result = capped.applyGunshotDamage(WoundType.GUNSHOT_HIGH_VELOCITY, 16, 0, 10, false, 4000);
        capped.recordGunshotLocations(result, 16, 0, 16, 4000);
        assertFalse(capped.seriousTrauma().hasPneumothorax());
    }
}
