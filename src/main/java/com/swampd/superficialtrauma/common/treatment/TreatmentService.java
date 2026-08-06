package com.swampd.superficialtrauma.common.treatment;

import com.swampd.superficialtrauma.common.body.BodyState;
import com.swampd.superficialtrauma.common.body.BodyStateCapability;
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

        if (SESSION_BY_ACTOR.containsKey(actor.getUUID())) {
            actor.displayClientMessage(
                    Component.translatable("message.superficialtrauma.treatment.actor_busy"),
                    true
            );
            return false;
        }

        UUID existingActor = ACTOR_BY_PATIENT.get(patient.getUUID());
        if (existingActor != null) {
            actor.displayClientMessage(
                    Component.translatable("message.superficialtrauma.treatment.patient_busy"),
                    true
            );
            return false;
        }

        Optional<BodyState> bodyState = BodyStateCapability.get(patient).resolve();
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
        SESSION_BY_ACTOR.put(actor.getUUID(), session);
        ACTOR_BY_PATIENT.put(patient.getUUID(), actor.getUUID());
        ModNetworking.sendTreatmentStarted(actor, patient.getId(), session);
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
                .resolve()
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
        if (actor.serverLevel().getGameTime() >= session.endsGameTime()) {
            complete(actor, patient, session);
        }
    }

    public static boolean isActorTreating(UUID actorId) {
        return SESSION_BY_ACTOR.containsKey(actorId);
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
        SESSION_BY_ACTOR.clear();
        ACTOR_BY_PATIENT.clear();
    }

    private static void complete(ServerPlayer actor, ServerPlayer patient, TreatmentSession session) {
        Optional<BodyState> state = BodyStateCapability.get(patient).resolve();
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
        } else if (session.procedure().isIcePack()) {
            changed = session.action() == TreatmentAction.APPLY
                    && state.get().applyIcePack(session.woundId());
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
        if (session.action().consumesItem()) {
            consumeRequiredItems(actor, session.procedure());
        }
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
        release(session);
        if (actor != null) {
            ModNetworking.sendTreatmentCancelled(actor, session, reason);
        }
    }

    private static void release(TreatmentSession session) {
        SESSION_BY_ACTOR.remove(session.actorId());
        ACTOR_BY_PATIENT.computeIfPresent(
                session.patientId(),
                (ignored, currentActor) -> currentActor.equals(session.actorId()) ? null : currentActor
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
                && actor.hasLineOfSight(patient));
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
        Optional<BodyState> bodyState = BodyStateCapability.get(actor).resolve();
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
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }
        return server.getPlayerList().getPlayer(actorId);
    }
}
