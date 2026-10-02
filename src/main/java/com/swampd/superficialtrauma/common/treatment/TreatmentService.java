package com.swampd.superficialtrauma.common.treatment;

import com.swampd.superficialtrauma.common.body.BodyState;
import com.swampd.superficialtrauma.common.body.BodyStateCapability;
import com.swampd.superficialtrauma.common.medication.MedicationService;
import com.swampd.superficialtrauma.common.qte.MedicalTimingQte;
import com.swampd.superficialtrauma.common.qte.TimingQteService;
import com.swampd.superficialtrauma.common.sound.MedicalActionSound;
import com.swampd.superficialtrauma.common.sound.MedicalActionSoundChannel;
import com.swampd.superficialtrauma.common.sound.MedicalActionSoundService;
import com.swampd.superficialtrauma.common.wound.WoundInstance;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class TreatmentService {
    private static final double MAX_TREATMENT_DISTANCE_SQUARED = 4.5D * 4.5D;
    private static final Map<UUID, TreatmentSession> SESSION_BY_ACTOR = new HashMap<>();
    private static final Map<UUID, UUID> ACTOR_BY_PATIENT = new HashMap<>();
    private static final Map<UUID, Long> NEXT_QTE_ROLL = new HashMap<>();

    private TreatmentService() {
    }

    public static boolean start(
            ServerPlayer actor,
            int patientEntityId,
            UUID woundId,
            TreatmentProcedure procedure,
            TreatmentAction action
    ) {
        Entity entity = actor.serverLevel().getEntity(patientEntityId);
        if (!(entity instanceof ServerPlayer patient)
                || !isEligibleActor(actor)
                || !isEligiblePatient(actor, patient)) {
            actor.displayClientMessage(
                    Component.translatable("message.superficialtrauma.treatment.invalid_target"),
                    true
            );
            return false;
        }

        if (SESSION_BY_ACTOR.containsKey(actor.getUUID())
                || MedicationService.isActorAdministering(actor.getUUID())) {
            actor.displayClientMessage(
                    Component.translatable("message.superficialtrauma.treatment.actor_busy"),
                    true
            );
            return false;
        }

        UUID existingActor = ACTOR_BY_PATIENT.get(patient.getUUID());
        if (existingActor != null || MedicationService.isPatientReceiving(patient.getUUID())) {
            actor.displayClientMessage(
                    Component.translatable("message.superficialtrauma.treatment.patient_busy"),
                    true
            );
            return false;
        }

        Optional<BodyState> bodyState = BodyStateCapability.get(patient);
        Optional<WoundInstance> wound = bodyState.flatMap(state -> state.wound(woundId));
        if (wound.isEmpty() || !procedure.isApplicable(wound.get(), action)) {
            actor.displayClientMessage(
                    Component.translatable("message.superficialtrauma.treatment.wound_changed"),
                    true
            );
            return false;
        }
        if (!hasRequiredSkill(actor, procedure, action)) {
            actor.displayClientMessage(
                    Component.translatable("message.superficialtrauma.treatment.skill_missing"),
                    true
            );
            return false;
        }
        if (action.consumesItem() && !hasRequiredItems(actor, procedure)) {
            actor.displayClientMessage(
                    Component.translatable("message.superficialtrauma.treatment.item_missing"),
                    true
            );
            return false;
        }

        long gameTime = actor.serverLevel().getGameTime();
        TreatmentSession session = new TreatmentSession(
                actor.getUUID(),
                patient.getUUID(),
                woundId,
                procedure,
                action,
                gameTime,
                gameTime + procedure.durationTicks(),
                actor.position(),
                patient.position()
        );
        TreatmentPreparationSoundService.stop(actor);
        SESSION_BY_ACTOR.put(actor.getUUID(), session);
        if (procedure.isSkinGraft()) {
            NEXT_QTE_ROLL.put(actor.getUUID(), gameTime + MedicalTimingQte.ROLL_INTERVAL_TICKS);
        }
        ACTOR_BY_PATIENT.put(patient.getUUID(), actor.getUUID());
        ModNetworking.sendTreatmentStarted(actor, patient.getId(), session);
        sendPatientActionNotice(actor, patient, procedure);
        MedicalActionSound sound = medicalSoundFor(procedure);
        if (sound != null) {
            MedicalActionSoundService.start(
                    actor,
                    patient,
                    MedicalActionSoundChannel.TREATMENT,
                    sound
            );
        }
        return true;
    }

    public static void tick(ServerPlayer actor) {
        TreatmentSession session = SESSION_BY_ACTOR.get(actor.getUUID());
        if (session == null) {
            return;
        }

        ServerPlayer patient = player(actor, session.patientId());
        if (patient == null || !isEligibleActor(actor) || !isEligiblePatient(actor, patient)) {
            cancelActor(actor.getUUID(), TreatmentCancelReason.INVALID_TARGET);
            return;
        }
        if (TreatmentMovementRules.interrupts(
                session.isSelfTreatment(),
                actor.isSprinting(),
                session.actorStartPosition(),
                actor.position(),
                session.patientStartPosition(),
                patient.position()
        )) {
            cancelActor(
                    actor.getUUID(),
                    session.isSelfTreatment()
                            ? TreatmentCancelReason.SPRINTING
                            : TreatmentCancelReason.MOVEMENT
            );
            return;
        }
        if (actor.isUsingItem()) {
            cancelActor(actor.getUUID(), TreatmentCancelReason.ACTION);
            return;
        }

        Optional<WoundInstance> wound = BodyStateCapability.get(patient)
                .flatMap(state -> state.wound(session.woundId()));
        if (wound.isEmpty() || !session.procedure().isApplicable(wound.get(), session.action())) {
            cancelActor(actor.getUUID(), TreatmentCancelReason.WOUND_CHANGED);
            return;
        }
        if (!hasRequiredSkill(actor, session.procedure(), session.action())) {
            cancelActor(actor.getUUID(), TreatmentCancelReason.SKILL_MISSING);
            return;
        }
        if (session.action().consumesItem() && !hasRequiredItems(actor, session.procedure())) {
            cancelActor(actor.getUUID(), TreatmentCancelReason.ITEM_MISSING);
            return;
        }
        if (actor.serverLevel().getGameTime() % 10L == 0L) {
            sendPatientActionNotice(actor, patient, session.procedure());
        }
        if (actor.serverLevel().getGameTime() >= session.endsGameTime()) {
            complete(actor, patient, session);
        } else if (session.procedure().isSkinGraft()) {
            maybeStartSurgeryQte(actor, session);
        }
    }

    private static void maybeStartSurgeryQte(ServerPlayer actor, TreatmentSession expected) {
        long now = actor.serverLevel().getGameTime();
        if (TimingQteService.hasActive(actor)
                || now < NEXT_QTE_ROLL.getOrDefault(actor.getUUID(), Long.MAX_VALUE)
                || expected.endsGameTime() - now <= MedicalTimingQte.FINAL_BUFFER_TICKS) {
            return;
        }
        NEXT_QTE_ROLL.put(actor.getUUID(), now + MedicalTimingQte.ROLL_INTERVAL_TICKS);
        if (actor.getRandom().nextFloat() >= MedicalTimingQte.CHANCE_PER_SECOND) {
            return;
        }
        TimingQteService.start(actor, MedicalTimingQte.DEFINITION, (player, result) -> {
            if (SESSION_BY_ACTOR.get(player.getUUID()) != expected) {
                return;
            }
            // Revalidate movement, materials and wound before applying a QTE result.
            NEXT_QTE_ROLL.put(player.getUUID(), player.serverLevel().getGameTime() + MedicalTimingQte.COOLDOWN_TICKS);
            tick(player);
            if (SESSION_BY_ACTOR.get(player.getUUID()) != expected) {
                return;
            }
            long gameTime = player.serverLevel().getGameTime();
            TreatmentSession updated = expected.withDeadline(
                    MedicalTimingQte.adjustedDeadline(expected.endsGameTime(), gameTime, result));
            SESSION_BY_ACTOR.put(player.getUUID(), updated);
            NEXT_QTE_ROLL.put(player.getUUID(), gameTime + MedicalTimingQte.COOLDOWN_TICKS);
            ServerPlayer patient = player(player, updated.patientId());
            if (patient != null) {
                ModNetworking.sendTreatmentStarted(player, patient.getId(), updated);
            }
        });
    }

    public static void cancelSkinGraft(ServerPlayer actor) {
        TreatmentSession session = SESSION_BY_ACTOR.get(actor.getUUID());
        if (session != null && session.procedure().isSkinGraft()) {
            cancelActor(actor.getUUID(), TreatmentCancelReason.ACTION);
        }
    }

    public static boolean isActorTreating(UUID actorId) {
        return SESSION_BY_ACTOR.containsKey(actorId);
    }

    public static boolean isPatientBeingTreated(UUID patientId) {
        return ACTOR_BY_PATIENT.containsKey(patientId);
    }

    public static boolean isSelfTreating(UUID actorId) {
        TreatmentSession session = SESSION_BY_ACTOR.get(actorId);
        return session != null && session.isSelfTreatment();
    }

    public static void cancelInvolving(ServerPlayer player, TreatmentCancelReason reason) {
        cancelActor(player.getUUID(), reason);
        UUID actorId = ACTOR_BY_PATIENT.get(player.getUUID());
        if (actorId != null && !actorId.equals(player.getUUID())) {
            cancelActor(actorId, reason);
        }
    }

    public static void clearAll() {
        for (TreatmentSession session : SESSION_BY_ACTOR.values()) {
            if (session.procedure().isSkinGraft()) {
                TimingQteService.forgetPlayer(session.actorId());
            }
        }
        NEXT_QTE_ROLL.clear();
        SESSION_BY_ACTOR.clear();
        ACTOR_BY_PATIENT.clear();
    }

    private static void complete(ServerPlayer actor, ServerPlayer patient, TreatmentSession session) {
        Optional<BodyState> state = BodyStateCapability.get(patient);
        Optional<WoundInstance> wound = state.flatMap(bodyState -> bodyState.wound(session.woundId()));
        if (state.isEmpty()
                || wound.isEmpty()
                || !session.procedure().isApplicable(wound.get(), session.action())
                || !hasRequiredSkill(actor, session.procedure(), session.action())
                || (session.action().consumesItem() && !hasRequiredItems(actor, session.procedure()))) {
            cancelActor(actor.getUUID(), TreatmentCancelReason.WOUND_CHANGED);
            return;
        }

        long gameTime = actor.serverLevel().getGameTime();
        boolean changed;
        if (session.procedure().isDebridement()) {
            changed = session.action() == TreatmentAction.APPLY
                    && state.get().debrideWound(session.woundId());
        } else if (session.procedure().isSkinGraft()) {
            changed = state.get().skinGraftWound(session.woundId(), gameTime);
        } else if (session.procedure().isDisinfection()) {
            changed = session.action() == TreatmentAction.APPLY
                    && state.get().disinfectWound(
                    session.woundId(),
                    session.procedure().disinfectant(),
                    gameTime
            );
        } else if (session.procedure().isIcePack()) {
            changed = session.action() == TreatmentAction.APPLY
                    && state.get().applyIcePack(session.woundId(), gameTime);
        } else if (session.procedure().isTourniquet()) {
            changed = session.action() == TreatmentAction.APPLY
                    ? state.get().applyTourniquet(session.woundId(), gameTime)
                    : state.get().removeTourniquet(session.woundId(), gameTime);
        } else if (session.procedure().isWoundPacking()) {
            changed = session.action() == TreatmentAction.APPLY
                    ? state.get().applyWoundPacking(session.woundId(), gameTime)
                    : state.get().removeWoundPacking(session.woundId(), gameTime);
        } else {
            changed = session.action() == TreatmentAction.APPLY
                    ? state.get().applyCovering(session.woundId(), session.procedure().covering(), gameTime)
                    : state.get().removeCovering(session.woundId(), session.procedure().covering(), gameTime);
        }
        if (!changed) {
            cancelActor(actor.getUUID(), TreatmentCancelReason.WOUND_CHANGED);
            return;
        }
        if (session.action() == TreatmentAction.APPLY && actor != patient && !state.get().canAct()) {
            state.get().recordResuscitationContributor(
                    actor.getUUID(),
                    actor.getGameProfile().getName()
            );
        }
        if (session.action().consumesItem()) {
            consumeRequiredItems(actor, session.procedure());
        }
        stopTreatmentSound(actor, patient, session);
        release(session);
        ModNetworking.sendTreatmentCompleted(actor, patient.getId(), session);
        ModNetworking.syncBodyState(patient);
        InspectionService.syncPatient(patient);
    }

    private static void cancelActor(UUID actorId, TreatmentCancelReason reason) {
        TreatmentSession session = SESSION_BY_ACTOR.get(actorId);
        if (session == null) {
            return;
        }
        ServerPlayer actor = findOnlinePlayer(session.actorId());
        ServerPlayer patient = actor == null ? null : player(actor, session.patientId());
        if (actor != null) {
            stopTreatmentSound(actor, patient, session);
        }
        release(session);
        if (actor != null) {
            ModNetworking.sendTreatmentCancelled(actor, session, reason);
        }
    }

    private static void release(TreatmentSession session) {
        NEXT_QTE_ROLL.remove(session.actorId());
        if (session.procedure().isSkinGraft()) {
            ServerPlayer actor = findOnlinePlayer(session.actorId());
            if (actor != null) {
                TimingQteService.cancel(actor);
            } else {
                TimingQteService.forgetPlayer(session.actorId());
            }
        }
        SESSION_BY_ACTOR.remove(session.actorId());
        ACTOR_BY_PATIENT.computeIfPresent(
                session.patientId(),
                (ignored, currentActor) -> currentActor.equals(session.actorId()) ? null : currentActor
        );
    }

    private static void stopTreatmentSound(
            ServerPlayer actor,
            ServerPlayer patient,
            TreatmentSession session
    ) {
        if (medicalSoundFor(session.procedure()) != null) {
            MedicalActionSoundService.stop(
                    actor,
                    patient,
                    MedicalActionSoundChannel.TREATMENT
            );
        }
    }

    private static MedicalActionSound medicalSoundFor(TreatmentProcedure procedure) {
        if (procedure == null) {
            return null;
        }
        if (procedure.isDebridement()) {
            return MedicalActionSound.LIQUID_POUCH;
        }
        if (procedure.isSkinGraft()) {
            return MedicalActionSound.CLOTH_WRAPPING;
        }
        if (procedure.isDisinfection()) {
            return MedicalActionSound.PLASTIC_CONTAINER;
        }
        if (procedure.isWoundPacking()) {
            return MedicalActionSound.PACKING;
        }
        if (procedure.isTourniquet()) {
            return MedicalActionSound.TOURNIQUET;
        }
        if (procedure.isIcePack()) {
            return MedicalActionSound.ICE_BAG;
        }
        return procedure.covering() == null ? null : MedicalActionSound.CLOTH_WRAPPING;
    }

    private static void sendPatientActionNotice(
            ServerPlayer actor,
            ServerPlayer patient,
            TreatmentProcedure procedure
    ) {
        if (actor == patient || procedure == null) {
            return;
        }
        ModNetworking.sendMedicalInspectionNotice(
                patient,
                actor,
                procedure.removalAnchor().requiredItem().getDescription()
        );
    }

    private static boolean isEligibleActor(ServerPlayer actor) {
        return actor.isAlive()
                && !actor.isRemoved()
                && !actor.isSpectator()
                && BodyStateCapability.get(actor).map(BodyState::canAct).orElse(false);
    }

    private static boolean isEligiblePatient(ServerPlayer actor, ServerPlayer patient) {
        if (!patient.isAlive() || patient.isRemoved() || actor.serverLevel() != patient.serverLevel()) {
            return false;
        }
        return actor == patient
                || (actor.distanceToSqr(patient) <= MAX_TREATMENT_DISTANCE_SQUARED
                && actor.hasLineOfSight(patient)
                && InspectionService.isInspecting(actor, patient.getId()));
    }

    private static boolean hasRequiredItems(ServerPlayer actor, TreatmentProcedure procedure) {
        for (TreatmentIngredient ingredient : procedure.ingredients()) {
            if (countItem(actor, ingredient.type()) < ingredient.count()) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasRequiredSkill(
            ServerPlayer actor,
            TreatmentProcedure procedure,
            TreatmentAction action
    ) {
        if (action == TreatmentAction.REMOVE) {
            return true;
        }
        Optional<BodyState> bodyState = BodyStateCapability.get(actor);
        if (bodyState.isEmpty()) {
            return false;
        }
        return (!procedure.requiresSurgerySkill() || bodyState.get().hasSurgerySkill())
                && (!procedure.requiresFirstAidSkill() || bodyState.get().hasFirstAidSkill());
    }

    private static int countItem(ServerPlayer actor, TreatmentType treatmentType) {
        int count = 0;
        Inventory inventory = actor.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(treatmentType.requiredItem())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void consumeRequiredItems(ServerPlayer actor, TreatmentProcedure procedure) {
        for (TreatmentIngredient ingredient : procedure.ingredients()) {
            consumeItem(actor, ingredient.type(), ingredient.count());
        }
        actor.getInventory().setChanged();
        actor.inventoryMenu.broadcastChanges();
    }

    private static void consumeItem(ServerPlayer actor, TreatmentType treatmentType, int requiredCount) {
        int remaining = requiredCount;
        Inventory inventory = actor.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(treatmentType.requiredItem())) {
                continue;
            }
            int consumed = Math.min(remaining, stack.getCount());
            stack.shrink(consumed);
            remaining -= consumed;
        }
    }

    private static ServerPlayer player(ServerPlayer reference, UUID playerId) {
        return reference.getServer() == null
                ? null
                : reference.getServer().getPlayerList().getPlayer(playerId);
    }

    private static ServerPlayer findOnlinePlayer(UUID actorId) {
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }
        return server.getPlayerList().getPlayer(actorId);
    }
}
