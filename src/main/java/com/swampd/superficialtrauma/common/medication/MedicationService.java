package com.swampd.superficialtrauma.common.medication;

import com.swampd.superficialtrauma.common.body.BodyLifeState;
import com.swampd.superficialtrauma.common.body.BodyState;
import com.swampd.superficialtrauma.common.body.BodyStateCapability;
import com.swampd.superficialtrauma.common.init.ModItems;
import com.swampd.superficialtrauma.common.sound.MedicalActionSound;
import com.swampd.superficialtrauma.common.sound.MedicalActionSoundChannel;
import com.swampd.superficialtrauma.common.sound.MedicalActionSoundService;
import com.swampd.superficialtrauma.common.treatment.InspectionService;
import com.swampd.superficialtrauma.common.treatment.TreatmentMovementRules;
import com.swampd.superficialtrauma.common.treatment.TreatmentService;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MedicationService {
    private static final double MAX_DISTANCE_SQUARED = 4.5D * 4.5D;
    private static final Map<UUID, MedicationSession> SESSION_BY_ACTOR = new HashMap<>();
    private static final Map<UUID, UUID> ACTOR_BY_PATIENT = new HashMap<>();
    private static final Map<UUID, InjectionPreparation> PREPARATION_BY_ACTOR = new HashMap<>();

    private MedicationService() {
    }

    public static boolean start(ServerPlayer actor, int patientEntityId, MedicationType type) {
        Entity entity = actor.serverLevel().getEntity(patientEntityId);
        if (!(entity instanceof ServerPlayer patient)
                || type == null
                || !isEligibleActor(actor)
                || !isEligiblePatient(actor, patient, type)
                || !hasRequiredInspection(actor, patient)) {
            actor.displayClientMessage(
                    Component.translatable("message.superficialtrauma.medication.invalid_target"),
                    true
            );
            return false;
        }
        if (isActorAdministering(actor.getUUID()) || TreatmentService.isActorTreating(actor.getUUID())) {
            actor.displayClientMessage(
                    Component.translatable("message.superficialtrauma.medication.actor_busy"),
                    true
            );
            return false;
        }
        if (isPatientReceiving(patient.getUUID()) || TreatmentService.isPatientBeingTreated(patient.getUUID())) {
            actor.displayClientMessage(
                    Component.translatable("message.superficialtrauma.medication.patient_busy"),
                    true
            );
            return false;
        }
        if (type.route() == MedicationRoute.INJECTION) {
            InjectionPreparation preparation = PREPARATION_BY_ACTOR.get(actor.getUUID());
            if (preparation == null || !preparation.patientId().equals(patient.getUUID())) {
                actor.displayClientMessage(
                        Component.translatable("message.superficialtrauma.medication.preparation_required"),
                        true
                );
                return false;
            }
        }
        if (!hasRequiredItems(actor, type)) {
            actor.displayClientMessage(
                    Component.translatable("message.superficialtrauma.medication.item_missing"),
                    true
            );
            return false;
        }

        long gameTime = actor.serverLevel().getGameTime();
        MedicationSession session = new MedicationSession(
                actor.getUUID(),
                patient.getUUID(),
                type,
                gameTime,
                gameTime + type.actionDurationTicks(),
                actor.position(),
                patient.position()
        );
        stopInjectionPreparation(actor);
        SESSION_BY_ACTOR.put(actor.getUUID(), session);
        ACTOR_BY_PATIENT.put(patient.getUUID(), actor.getUUID());
        ModNetworking.sendMedicationStarted(actor, patient.getId(), session);
        sendPatientMedicationNotice(actor, patient, type);
        MedicalActionSoundService.start(
                actor,
                patient,
                MedicalActionSoundChannel.MEDICATION,
                soundFor(type)
        );
        return true;
    }

    public static void setInjectionPreparation(ServerPlayer actor, int patientEntityId, boolean active) {
        if (!active) {
            stopInjectionPreparation(actor);
            return;
        }
        Entity entity = actor.serverLevel().getEntity(patientEntityId);
        if (!(entity instanceof ServerPlayer patient)
                || isActorAdministering(actor.getUUID())
                || TreatmentService.isActorTreating(actor.getUUID())
                || isPatientReceiving(patient.getUUID())
                || TreatmentService.isPatientBeingTreated(patient.getUUID())
                || !canPrepareInjection(actor, patient)) {
            stopInjectionPreparation(actor);
            return;
        }
        stopInjectionPreparation(actor);
        PREPARATION_BY_ACTOR.put(
                actor.getUUID(),
                new InjectionPreparation(patient.getUUID(), patient.position())
        );
        MedicalActionSoundService.start(
                actor,
                patient,
                MedicalActionSoundChannel.PREPARATION,
                MedicalActionSound.SYRINGE_START
        );
    }

    public static void tick(ServerPlayer actor) {
        tickPreparation(actor);
        MedicationSession session = SESSION_BY_ACTOR.get(actor.getUUID());
        if (session == null) {
            return;
        }
        ServerPlayer patient = player(actor, session.patientId());
        if (patient == null
                || !isEligibleActor(actor)
                || !isEligiblePatient(actor, patient, session.type())
                || !hasRequiredInspection(actor, patient)) {
            cancelActor(actor.getUUID(), MedicationCancelReason.INVALID_TARGET);
            return;
        }
        if (TreatmentMovementRules.interrupts(
                session.isSelfMedication(),
                actor.isSprinting(),
                session.actorStartPosition(),
                actor.position(),
                session.patientStartPosition(),
                patient.position()
        )) {
            cancelActor(
                    actor.getUUID(),
                    session.isSelfMedication()
                            ? MedicationCancelReason.SPRINTING
                            : MedicationCancelReason.MOVEMENT
            );
            return;
        }
        if (actor.isUsingItem()) {
            cancelActor(actor.getUUID(), MedicationCancelReason.ACTION);
            return;
        }
        if (!hasRequiredItems(actor, session.type())) {
            cancelActor(actor.getUUID(), MedicationCancelReason.ITEM_MISSING);
            return;
        }
        if (!session.isSelfMedication()
                && actor.serverLevel().getGameTime() % 10L == 0L) {
            sendPatientMedicationNotice(actor, patient, session.type());
        }
        if (actor.serverLevel().getGameTime() >= session.endsGameTime()) {
            complete(actor, patient, session);
        }
    }

    public static boolean isActorAdministering(UUID actorId) {
        return SESSION_BY_ACTOR.containsKey(actorId);
    }

    public static boolean isPatientReceiving(UUID patientId) {
        return ACTOR_BY_PATIENT.containsKey(patientId);
    }

    public static void cancelInvolving(ServerPlayer player, MedicationCancelReason reason) {
        stopInjectionPreparation(player);
        cancelActor(player.getUUID(), reason);
        for (UUID actorId : new ArrayList<>(PREPARATION_BY_ACTOR.keySet())) {
            InjectionPreparation preparation = PREPARATION_BY_ACTOR.get(actorId);
            if (preparation != null && preparation.patientId().equals(player.getUUID())) {
                ServerPlayer actor = player(player, actorId);
                if (actor != null) {
                    stopInjectionPreparation(actor);
                }
            }
        }
        UUID actorId = ACTOR_BY_PATIENT.get(player.getUUID());
        if (actorId != null && !actorId.equals(player.getUUID())) {
            cancelActor(actorId, reason);
        }
    }

    public static void clearAll() {
        SESSION_BY_ACTOR.clear();
        ACTOR_BY_PATIENT.clear();
        PREPARATION_BY_ACTOR.clear();
    }

    private static void tickPreparation(ServerPlayer actor) {
        InjectionPreparation preparation = PREPARATION_BY_ACTOR.get(actor.getUUID());
        if (preparation == null) {
            return;
        }
        ServerPlayer patient = player(actor, preparation.patientId());
        if (patient == null
                || patient.position().distanceToSqr(preparation.patientStartPosition())
                > TreatmentMovementRules.MOVEMENT_TOLERANCE_SQUARED
                || isActorAdministering(actor.getUUID())
                || TreatmentService.isActorTreating(actor.getUUID())
                || isPatientReceiving(patient.getUUID())
                || TreatmentService.isPatientBeingTreated(patient.getUUID())
                || !canPrepareInjection(actor, patient)) {
            stopInjectionPreparation(actor);
        }
    }

    private static void complete(
            ServerPlayer actor,
            ServerPlayer patient,
            MedicationSession session
    ) {
        BodyState state = BodyStateCapability.get(patient).orElse(null);
        if (state == null
                || !isEligibleActor(actor)
                || !isEligiblePatient(actor, patient, session.type())
                || !hasRequiredInspection(actor, patient)
                || !hasRequiredItems(actor, session.type())) {
            cancelActor(actor.getUUID(), MedicationCancelReason.INVALID_TARGET);
            return;
        }
        boolean patientWasDowned = !state.canAct();
        if (!state.applyMedication(
                session.type(),
                actor.serverLevel().getGameTime(),
                patientWasDowned
        )) {
            cancelActor(actor.getUUID(), MedicationCancelReason.INVALID_TARGET);
            return;
        }
        consumeRequiredItems(actor, session.type());
        if (patientWasDowned && actor != patient) {
            state.recordResuscitationContributor(actor.getUUID(), actor.getGameProfile().getName());
        }
        stopSessionSound(actor, patient);
        release(session);
        ModNetworking.sendMedicationCompleted(actor, patient.getId(), session);
        ModNetworking.syncBodyState(patient);
        InspectionService.syncPatient(patient);
    }

    private static void cancelActor(UUID actorId, MedicationCancelReason reason) {
        MedicationSession session = SESSION_BY_ACTOR.get(actorId);
        if (session == null) {
            return;
        }
        ServerPlayer actor = findOnlinePlayer(session.actorId());
        ServerPlayer patient = actor == null ? null : player(actor, session.patientId());
        if (actor != null) {
            stopSessionSound(actor, patient);
        }
        release(session);
        if (actor != null) {
            ModNetworking.sendMedicationCancelled(actor, session, reason);
        }
    }

    private static void release(MedicationSession session) {
        SESSION_BY_ACTOR.remove(session.actorId());
        ACTOR_BY_PATIENT.computeIfPresent(
                session.patientId(),
                (ignored, currentActor) -> currentActor.equals(session.actorId()) ? null : currentActor
        );
    }

    private static void stopInjectionPreparation(ServerPlayer actor) {
        InjectionPreparation preparation = PREPARATION_BY_ACTOR.remove(actor.getUUID());
        if (preparation != null) {
            MedicalActionSoundService.stop(
                    actor,
                    player(actor, preparation.patientId()),
                    MedicalActionSoundChannel.PREPARATION
            );
        }
    }

    private static void stopSessionSound(ServerPlayer actor, ServerPlayer patient) {
        MedicalActionSoundService.stop(actor, patient, MedicalActionSoundChannel.MEDICATION);
    }

    private static boolean isEligibleActor(ServerPlayer actor) {
        return actor.isAlive()
                && !actor.isRemoved()
                && !actor.isSpectator()
                && BodyStateCapability.get(actor).map(BodyState::canAct).orElse(false);
    }

    private static boolean isEligiblePatient(
            ServerPlayer actor,
            ServerPlayer patient,
            MedicationType type
    ) {
        if (!patient.isAlive()
                || patient.isRemoved()
                || actor.serverLevel() != patient.serverLevel()
                || (actor != patient && (actor.distanceToSqr(patient) > MAX_DISTANCE_SQUARED
                || !actor.hasLineOfSight(patient)))) {
            return false;
        }
        BodyState state = BodyStateCapability.get(patient).orElse(null);
        if (state == null || state.lifeState() == BodyLifeState.BRAIN_DEAD) {
            return false;
        }
        if (type == MedicationType.NALOXONE && !state.hasActiveOpioidDose()) {
            return false;
        }
        return type.route().allowsPatient(actor == patient, state.canAct());
    }

    private static boolean hasRequiredInspection(ServerPlayer actor, ServerPlayer patient) {
        return actor == patient || InspectionService.isInspecting(actor, patient.getId());
    }

    private static boolean hasRequiredItems(ServerPlayer actor, MedicationType type) {
        return switch (type) {
            case PARACETAMOL -> hasItem(actor, ModItems.PARACETAMOL.get());
            case MORPHINE -> hasItem(actor, ModItems.SYRINGE.get())
                    && hasItem(actor, ModItems.MORPHINE_VIAL.get());
            case REMIFENTANIL -> hasItem(actor, ModItems.SYRINGE.get())
                    && hasItem(actor, ModItems.REMIFENTANIL_INJECTION.get());
            case NALOXONE -> hasItem(actor, ModItems.SYRINGE.get())
                    && hasItem(actor, ModItems.NALOXONE.get());
            case EPINEPHRINE -> hasItem(actor, ModItems.SYRINGE.get())
                    && hasItem(actor, ModItems.EPINEPHRINE_INJECTION.get());
            case METOPROLOL -> hasItem(actor, ModItems.METOPROLOL.get());
            case ATROPINE_SULFATE -> hasItem(actor, ModItems.SYRINGE.get())
                    && hasItem(actor, ModItems.ATROPINE_SULFATE_INJECTION.get());
            case PRALIDOXIME_CHLORIDE -> hasItem(actor, ModItems.SYRINGE.get())
                    && hasItem(actor, ModItems.PRALIDOXIME_CHLORIDE_INJECTION.get());
            case AMOXICILLIN -> hasItem(actor, ModItems.AMOXICILLIN.get());
            case CEFTRIAXONE -> hasItem(actor, ModItems.SYRINGE.get())
                    && hasItem(actor, ModItems.CEFTRIAXONE.get());
        };
    }

    private static void consumeRequiredItems(ServerPlayer actor, MedicationType type) {
        switch (type) {
            case PARACETAMOL -> consumeOne(actor, ModItems.PARACETAMOL.get());
            case MORPHINE -> {
                consumeOne(actor, ModItems.SYRINGE.get());
                consumeOne(actor, ModItems.MORPHINE_VIAL.get());
            }
            case REMIFENTANIL -> {
                consumeOne(actor, ModItems.SYRINGE.get());
                consumeOne(actor, ModItems.REMIFENTANIL_INJECTION.get());
            }
            case NALOXONE -> {
                consumeOne(actor, ModItems.SYRINGE.get());
                consumeOne(actor, ModItems.NALOXONE.get());
            }
            case EPINEPHRINE -> {
                consumeOne(actor, ModItems.SYRINGE.get());
                consumeOne(actor, ModItems.EPINEPHRINE_INJECTION.get());
            }
            case METOPROLOL -> consumeOne(actor, ModItems.METOPROLOL.get());
            case ATROPINE_SULFATE -> {
                consumeOne(actor, ModItems.SYRINGE.get());
                consumeOne(actor, ModItems.ATROPINE_SULFATE_INJECTION.get());
            }
            case PRALIDOXIME_CHLORIDE -> {
                consumeOne(actor, ModItems.SYRINGE.get());
                consumeOne(actor, ModItems.PRALIDOXIME_CHLORIDE_INJECTION.get());
            }
            case AMOXICILLIN -> consumeOne(actor, ModItems.AMOXICILLIN.get());
            case CEFTRIAXONE -> {
                consumeOne(actor, ModItems.SYRINGE.get());
                consumeOne(actor, ModItems.CEFTRIAXONE.get());
            }
        }
        actor.getInventory().setChanged();
        actor.inventoryMenu.broadcastChanges();
    }

    private static boolean hasItem(ServerPlayer actor, Item item) {
        Inventory inventory = actor.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(item)) {
                return true;
            }
        }
        return false;
    }

    private static void consumeOne(ServerPlayer actor, Item item) {
        Inventory inventory = actor.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                stack.shrink(1);
                return;
            }
        }
    }

    private static MedicalActionSound soundFor(MedicationType type) {
        return switch (type) {
            case PARACETAMOL, METOPROLOL, AMOXICILLIN -> MedicalActionSound.TABLETS;
            case MORPHINE, REMIFENTANIL, NALOXONE, EPINEPHRINE,
                    ATROPINE_SULFATE, CEFTRIAXONE -> MedicalActionSound.VIAL;
            case PRALIDOXIME_CHLORIDE -> MedicalActionSound.AMPOULE;
        };
    }

    private static boolean canPrepareInjection(ServerPlayer actor, ServerPlayer patient) {
        if (!isEligibleActor(actor) || !hasRequiredInspection(actor, patient)) {
            return false;
        }
        BodyState state = BodyStateCapability.get(patient).orElse(null);
        if (state == null || state.lifeState() == BodyLifeState.BRAIN_DEAD) {
            return false;
        }
        boolean morphineAvailable = isEligiblePatient(actor, patient, MedicationType.MORPHINE)
                && hasRequiredItems(actor, MedicationType.MORPHINE);
        boolean remifentanilAvailable = isEligiblePatient(actor, patient, MedicationType.REMIFENTANIL)
                && hasRequiredItems(actor, MedicationType.REMIFENTANIL);
        boolean naloxoneAvailable = isEligiblePatient(actor, patient, MedicationType.NALOXONE)
                && hasRequiredItems(actor, MedicationType.NALOXONE);
        boolean epinephrineAvailable = isEligiblePatient(actor, patient, MedicationType.EPINEPHRINE)
                && hasRequiredItems(actor, MedicationType.EPINEPHRINE);
        boolean atropineAvailable = isEligiblePatient(actor, patient, MedicationType.ATROPINE_SULFATE)
                && hasRequiredItems(actor, MedicationType.ATROPINE_SULFATE);
        boolean pralidoximeAvailable = isEligiblePatient(actor, patient, MedicationType.PRALIDOXIME_CHLORIDE)
                && hasRequiredItems(actor, MedicationType.PRALIDOXIME_CHLORIDE);
        boolean ceftriaxoneAvailable = isEligiblePatient(actor, patient, MedicationType.CEFTRIAXONE)
                && hasRequiredItems(actor, MedicationType.CEFTRIAXONE);
        return morphineAvailable
                || remifentanilAvailable
                || naloxoneAvailable
                || epinephrineAvailable
                || atropineAvailable
                || pralidoximeAvailable
                || ceftriaxoneAvailable;
    }

    private static void sendPatientMedicationNotice(
            ServerPlayer actor,
            ServerPlayer patient,
            MedicationType type
    ) {
        if (actor == patient) {
            return;
        }
        ModNetworking.sendMedicalInspectionNotice(
                patient,
                actor,
                Component.translatable(type.translationKey())
        );
    }

    private static ServerPlayer player(ServerPlayer reference, UUID playerId) {
        return reference.getServer() == null
                ? null
                : reference.getServer().getPlayerList().getPlayer(playerId);
    }

    private static ServerPlayer findOnlinePlayer(UUID playerId) {
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.getPlayerList().getPlayer(playerId);
    }

    private record InjectionPreparation(UUID patientId, Vec3 patientStartPosition) {
    }
}
