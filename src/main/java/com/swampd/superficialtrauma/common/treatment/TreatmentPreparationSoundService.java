package com.swampd.superficialtrauma.common.treatment;

import com.swampd.superficialtrauma.common.body.BodyState;
import com.swampd.superficialtrauma.common.body.BodyStateCapability;
import com.swampd.superficialtrauma.common.init.ModItems;
import com.swampd.superficialtrauma.common.sound.MedicalActionSoundChannel;
import com.swampd.superficialtrauma.common.sound.MedicalActionSoundService;
import com.swampd.superficialtrauma.common.wound.WoundInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TreatmentPreparationSoundService {
    private static final double MAX_DISTANCE_SQUARED = 4.5D * 4.5D;
    private static final Map<UUID, Session> SESSION_BY_ACTOR = new HashMap<>();

    private TreatmentPreparationSoundService() {
    }

    public static void set(
            ServerPlayer actor,
            int patientEntityId,
            UUID woundId,
            TreatmentPreparationType type,
            boolean active
    ) {
        if (!active) {
            stop(actor);
            return;
        }
        Entity entity = actor.serverLevel().getEntity(patientEntityId);
        if (!(entity instanceof ServerPlayer patient) || !canPrepare(actor, patient, woundId, type)) {
            stop(actor);
            return;
        }
        stop(actor);
        Session session = new Session(
                patient.getUUID(),
                patient.getId(),
                woundId,
                type,
                patient.position()
        );
        SESSION_BY_ACTOR.put(actor.getUUID(), session);
        MedicalActionSoundService.start(
                actor,
                patient,
                MedicalActionSoundChannel.PREPARATION,
                type.sound()
        );
    }

    public static void tick(ServerPlayer actor) {
        Session session = SESSION_BY_ACTOR.get(actor.getUUID());
        if (session == null) {
            return;
        }
        ServerPlayer patient = player(actor, session.patientId());
        if (patient == null
                || patient.position().distanceToSqr(session.patientStartPosition())
                > TreatmentMovementRules.MOVEMENT_TOLERANCE_SQUARED
                || !canPrepare(actor, patient, session.woundId(), session.type())) {
            stop(actor);
        }
    }

    public static void stop(ServerPlayer actor) {
        Session session = SESSION_BY_ACTOR.remove(actor.getUUID());
        if (session == null) {
            return;
        }
        MedicalActionSoundService.stop(
                actor,
                player(actor, session.patientId()),
                MedicalActionSoundChannel.PREPARATION
        );
    }

    public static void cancelInvolving(ServerPlayer player) {
        stop(player);
        for (UUID actorId : new ArrayList<>(SESSION_BY_ACTOR.keySet())) {
            Session session = SESSION_BY_ACTOR.get(actorId);
            if (session != null && session.patientId().equals(player.getUUID())) {
                ServerPlayer actor = player(player, actorId);
                if (actor != null) {
                    stop(actor);
                }
            }
        }
    }

    public static void clearAll() {
        SESSION_BY_ACTOR.clear();
    }

    private static boolean canPrepare(
            ServerPlayer actor,
            ServerPlayer patient,
            UUID woundId,
            TreatmentPreparationType type
    ) {
        if (type == null
                || !actor.isAlive()
                || actor.isRemoved()
                || actor.isSpectator()
                || !patient.isAlive()
                || patient.isRemoved()
                || actor.serverLevel() != patient.serverLevel()
                || (actor != patient && (actor.distanceToSqr(patient) > MAX_DISTANCE_SQUARED
                || !actor.hasLineOfSight(patient)))
                || BodyStateCapability.get(actor).map(BodyState::canAct).orElse(false) == false) {
            return false;
        }
        WoundInstance wound = BodyStateCapability.get(patient)
                .flatMap(state -> state.wound(woundId))
                .orElse(null);
        if (wound == null) {
            return false;
        }
        return switch (type) {
            case BANDAGE -> !wound.covering().isApplied()
                    && hasItem(actor, ModItems.BANDAGE.get())
                    && (hasItem(actor, ModItems.MEDICAL_TAPE.get())
                    || hasItem(actor, ModItems.SELF_ADHESIVE_BANDAGE.get()));
            case DEBRIDEMENT -> BodyStateCapability.get(actor).map(BodyState::hasSurgerySkill).orElse(false)
                    && hasItem(actor, ModItems.SURGICAL_KIT.get())
                    && ((TreatmentProcedure.DEBRIDEMENT.isApplicable(wound, TreatmentAction.APPLY)
                    && hasItem(actor, ModItems.SALINE_SOLUTION.get()))
                    || (TreatmentProcedure.SKIN_GRAFT.isApplicable(wound, TreatmentAction.APPLY)
                    && hasItem(actor, ModItems.ARTIFICIAL_DERMIS.get())));
        };
    }

    private static boolean hasItem(ServerPlayer player, Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    private static ServerPlayer player(ServerPlayer reference, UUID playerId) {
        return reference.getServer() == null
                ? null
                : reference.getServer().getPlayerList().getPlayer(playerId);
    }

    private record Session(
            UUID patientId,
            int patientEntityId,
            UUID woundId,
            TreatmentPreparationType type,
            Vec3 patientStartPosition
    ) {
    }
}
