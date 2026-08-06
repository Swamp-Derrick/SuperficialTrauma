package com.swampd.superficialtrauma.common.treatment;

import com.swampd.superficialtrauma.common.body.BodyLifeState;
import com.swampd.superficialtrauma.common.body.BodyState;
import com.swampd.superficialtrauma.common.body.BodyStateCapability;
import com.swampd.superficialtrauma.common.body.CollapseReason;
import com.swampd.superficialtrauma.common.body.DefibrillationEnergy;
import com.swampd.superficialtrauma.common.body.DefibrillationResult;
import com.swampd.superficialtrauma.common.body.DownedHitbox;
import com.swampd.superficialtrauma.common.body.DownedPoseCapture;
import com.swampd.superficialtrauma.common.damage.DamageDowning;
import com.swampd.superficialtrauma.common.init.ModItems;
import com.swampd.superficialtrauma.common.item.DefibrillatorItem;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DefibrillationService {
    public static final long CHARGE_DURATION_TICKS = 2L * 20L;
    private static final double MAX_DISTANCE_SQUARED = 2.5D * 2.5D;
    private static final double BYSTANDER_SHOCK_DISTANCE_SQUARED = 1.0D;
    private static final float BYSTANDER_DAMAGE = 6.0F;
    private static final Map<UUID, DefibrillationSession> SESSION_BY_ACTOR = new HashMap<>();
    private static final Map<UUID, UUID> ACTOR_BY_PATIENT = new HashMap<>();

    private DefibrillationService() {
    }

    public static boolean start(
            ServerPlayer actor,
            int patientEntityId,
            DefibrillationEnergy energy
    ) {
        if (!(actor.serverLevel().getEntity(patientEntityId) instanceof ServerPlayer patient)
                || energy == null
                || !canTreat(actor, patient)) {
            actor.displayClientMessage(Component.translatable(
                    "message.superficialtrauma.defibrillation.invalid_target"
            ), true);
            return false;
        }
        UUID existingActor = ACTOR_BY_PATIENT.get(patient.getUUID());
        if (existingActor != null && !existingActor.equals(actor.getUUID())) {
            actor.displayClientMessage(Component.translatable(
                    "message.superficialtrauma.defibrillation.patient_busy"
            ), true);
            return false;
        }

        int defibrillatorSlot = findChargedDefibrillator(actor, energy.joules());
        if (defibrillatorSlot < 0) {
            actor.displayClientMessage(Component.translatable(
                    "message.superficialtrauma.defibrillation.energy_missing",
                    energy.joules()
            ), true);
            return false;
        }

        TreatmentService.cancelInvolving(actor, TreatmentCancelReason.ACTION);
        AirwayService.cancelInvolving(actor);
        CprService.cancelInvolving(actor);
        cancelActor(actor.getUUID());

        Inventory inventory = actor.getInventory();
        int selectedSlot = inventory.selected;
        if (defibrillatorSlot != selectedSlot) {
            ItemStack selectedStack = inventory.getItem(selectedSlot);
            inventory.setItem(selectedSlot, inventory.getItem(defibrillatorSlot));
            inventory.setItem(defibrillatorSlot, selectedStack);
        }
        actor.inventoryMenu.broadcastChanges();

        long gameTime = actor.serverLevel().getGameTime();
        DefibrillationSession session = new DefibrillationSession(
                actor.getUUID(),
                patient.getUUID(),
                energy,
                gameTime + CHARGE_DURATION_TICKS,
                actor.position(),
                patient.position(),
                selectedSlot,
                defibrillatorSlot
        );
        SESSION_BY_ACTOR.put(actor.getUUID(), session);
        ACTOR_BY_PATIENT.put(patient.getUUID(), actor.getUUID());
        actor.displayClientMessage(Component.translatable(
                "message.superficialtrauma.defibrillation.charging",
                energy.joules()
        ), true);
        return true;
    }

    public static void release(ServerPlayer actor, int patientEntityId) {
        DefibrillationSession session = SESSION_BY_ACTOR.get(actor.getUUID());
        if (session == null) {
            return;
        }
        ServerPlayer patient = player(actor, session.patientId);
        if (patient == null || patient.getId() != patientEntityId) {
            cancelActor(actor.getUUID());
            return;
        }
        long gameTime = actor.serverLevel().getGameTime();
        if (gameTime < session.readyGameTime) {
            cancelActor(actor.getUUID());
            actor.displayClientMessage(Component.translatable(
                    "message.superficialtrauma.defibrillation.charge_incomplete"
            ), true);
            return;
        }
        if (!isSessionValid(actor, patient, session)) {
            cancelActor(actor.getUUID());
            actor.displayClientMessage(Component.translatable(
                    "message.superficialtrauma.defibrillation.cancelled"
            ), true);
            return;
        }

        ItemStack defibrillator = actor.getInventory().getItem(session.selectedSlot);
        if (!defibrillator.is(ModItems.DEFIBRILLATOR.get())
                || DefibrillatorItem.getEnergy(defibrillator) < session.energy.joules()) {
            cancelActor(actor.getUUID());
            actor.displayClientMessage(Component.translatable(
                    "message.superficialtrauma.defibrillation.energy_missing",
                    session.energy.joules()
            ), true);
            return;
        }
        discharge(actor, patient, session, defibrillator);
    }

    public static void cancel(ServerPlayer actor, int patientEntityId) {
        DefibrillationSession session = SESSION_BY_ACTOR.get(actor.getUUID());
        if (session == null) {
            return;
        }
        ServerPlayer patient = player(actor, session.patientId);
        if (patient == null || patient.getId() == patientEntityId) {
            cancelActor(actor.getUUID());
        }
    }

    public static void tick(ServerPlayer actor) {
        DefibrillationSession session = SESSION_BY_ACTOR.get(actor.getUUID());
        if (session == null) {
            return;
        }
        ServerPlayer patient = player(actor, session.patientId);
        if (patient == null || !isSessionValid(actor, patient, session)) {
            cancelActor(actor.getUUID());
            actor.displayClientMessage(Component.translatable(
                    "message.superficialtrauma.defibrillation.cancelled"
            ), true);
            return;
        }

        ItemStack defibrillator = actor.getInventory().getItem(session.selectedSlot);
        if (!defibrillator.is(ModItems.DEFIBRILLATOR.get())
                || DefibrillatorItem.getEnergy(defibrillator) < session.energy.joules()) {
            cancelActor(actor.getUUID());
            actor.displayClientMessage(Component.translatable(
                    "message.superficialtrauma.defibrillation.energy_missing",
                    session.energy.joules()
            ), true);
            return;
        }
        if (actor.serverLevel().getGameTime() >= session.readyGameTime && !session.readyNotified) {
            session.readyNotified = true;
            actor.displayClientMessage(Component.translatable(
                    "message.superficialtrauma.defibrillation.ready"
            ), true);
        }
    }

    public static void cancelInvolving(ServerPlayer player) {
        cancelActor(player.getUUID());
        UUID actorId = ACTOR_BY_PATIENT.get(player.getUUID());
        if (actorId != null) {
            cancelActor(actorId);
        }
    }

    public static void clearAll() {
        for (DefibrillationSession session : SESSION_BY_ACTOR.values().toArray(DefibrillationSession[]::new)) {
            ServerPlayer actor = findOnlinePlayer(session.actorId);
            if (actor != null) {
                restoreEquipment(actor, session);
            }
        }
        SESSION_BY_ACTOR.clear();
        ACTOR_BY_PATIENT.clear();
    }

    private static void discharge(
            ServerPlayer actor,
            ServerPlayer patient,
            DefibrillationSession session,
            ItemStack defibrillator
    ) {
        BodyState state = BodyStateCapability.get(patient).orElse(null);
        long gameTime = actor.serverLevel().getGameTime();
        if (state == null
                || state.ventricularFibrillationRemainingTicks(gameTime) <= 0L
                || state.downedDangerRemainingTicks(gameTime) <= 0L
                || !DefibrillatorItem.consumeEnergy(defibrillator, session.energy.joules())) {
            cancelActor(actor.getUUID());
            return;
        }

        DefibrillationResult result = state.applyDefibrillation(
                session.energy,
                actor.getRandom().nextDouble(),
                gameTime
        );
        if (!result.valid()) {
            cancelActor(actor.getUUID());
            return;
        }

        shockBystanders(actor, patient, gameTime);
        actor.serverLevel().playSound(
                null,
                patient.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_IMPACT,
                SoundSource.PLAYERS,
                0.55F,
                1.8F
        );
        actor.serverLevel().sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                patient.getX(), patient.getY() + 0.45D, patient.getZ(),
                14,
                0.35D, 0.25D, 0.35D,
                0.04D
        );

        if (result.succeeded()) {
            state.recordResuscitationContributor(actor.getUUID(), actor.getGameProfile().getName());
        }
        String messageKey = switch (result.status()) {
            case RESTORED_CIRCULATION -> "message.superficialtrauma.defibrillation.success";
            case FAILED -> "message.superficialtrauma.defibrillation.failed";
            case UNSAFE_FAILURE_BRAIN_DEATH ->
                    "message.superficialtrauma.defibrillation.unsafe_brain_death";
            case INVALID -> "message.superficialtrauma.defibrillation.invalid_target";
        };
        actor.displayClientMessage(Component.translatable(messageKey), true);
        release(actor, session);
        ModNetworking.syncBodyState(patient);
        InspectionService.syncPatient(patient);
    }

    private static void shockBystanders(ServerPlayer actor, ServerPlayer patient, long gameTime) {
        for (ServerPlayer bystander : actor.serverLevel().players()) {
            if (bystander == actor
                    || bystander == patient
                    || !bystander.isAlive()
                    || bystander.isSpectator()
                    || bystander.getAbilities().invulnerable
                    || bystander.distanceToSqr(patient) > BYSTANDER_SHOCK_DISTANCE_SQUARED) {
                continue;
            }
            BodyStateCapability.get(bystander).ifPresent(state -> {
                if (!state.canAct()) {
                    state.applyDownedDamage(BYSTANDER_DAMAGE, gameTime);
                } else {
                    state.applyDefibrillatorShockBurn(gameTime);
                    float currentHealth = bystander.getHealth();
                    boolean lethal = DamageDowning.wouldBeFatal(currentHealth, BYSTANDER_DAMAGE);
                    float applied = lethal
                            ? DamageDowning.clampToPreserveLife(currentHealth, BYSTANDER_DAMAGE)
                            : Math.min(BYSTANDER_DAMAGE, currentHealth);
                    bystander.setHealth(currentHealth - applied);
                    if (lethal && state.incapacitate(CollapseReason.HEMORRHAGIC_SHOCK, gameTime)) {
                        state.captureDownedPose(DownedPoseCapture.capture(bystander, null, gameTime));
                        DownedHitbox.update(bystander, state);
                        ModNetworking.syncDownedPose(bystander);
                    }
                }
                ModNetworking.syncBodyState(bystander);
                InspectionService.syncPatient(bystander);
            });
        }
    }

    private static boolean canTreat(ServerPlayer actor, ServerPlayer patient) {
        if (actor == patient
                || !actor.isAlive()
                || actor.isRemoved()
                || actor.isSpectator()
                || !patient.isAlive()
                || patient.isRemoved()
                || actor.serverLevel() != patient.serverLevel()
                || actor.distanceToSqr(patient) > MAX_DISTANCE_SQUARED
                || !actor.hasLineOfSight(patient)
                || !InspectionService.isInspecting(actor, patient.getId())) {
            return false;
        }
        return BodyStateCapability.get(actor)
                .map(state -> state.canAct() && state.hasFirstAidSkill())
                .orElse(false)
                && BodyStateCapability.get(patient)
                .map(BodyState::lifeState)
                .orElse(BodyLifeState.ACTIVE) == BodyLifeState.VENTRICULAR_FIBRILLATION;
    }

    private static boolean isSessionValid(
            ServerPlayer actor,
            ServerPlayer patient,
            DefibrillationSession session
    ) {
        return canTreat(actor, patient)
                && actor.getInventory().selected == session.selectedSlot
                && !TreatmentMovementRules.interrupts(
                false,
                false,
                session.actorStart,
                actor.position(),
                session.patientStart,
                patient.position()
        );
    }

    private static int findChargedDefibrillator(ServerPlayer actor, int requiredEnergy) {
        Inventory inventory = actor.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModItems.DEFIBRILLATOR.get())
                    && DefibrillatorItem.getEnergy(stack) >= requiredEnergy) {
                return slot;
            }
        }
        return -1;
    }

    private static void cancelActor(UUID actorId) {
        DefibrillationSession session = SESSION_BY_ACTOR.get(actorId);
        if (session == null) {
            return;
        }
        ServerPlayer actor = findOnlinePlayer(actorId);
        if (actor != null) {
            release(actor, session);
        } else {
            SESSION_BY_ACTOR.remove(actorId);
            ACTOR_BY_PATIENT.remove(session.patientId, actorId);
        }
    }

    private static void release(ServerPlayer actor, DefibrillationSession session) {
        SESSION_BY_ACTOR.remove(session.actorId);
        ACTOR_BY_PATIENT.remove(session.patientId, session.actorId);
        restoreEquipment(actor, session);
    }

    private static void restoreEquipment(ServerPlayer actor, DefibrillationSession session) {
        Inventory inventory = actor.getInventory();
        if (session.defibrillatorSourceSlot != session.selectedSlot) {
            ItemStack selectedStack = inventory.getItem(session.selectedSlot);
            inventory.setItem(session.selectedSlot, inventory.getItem(session.defibrillatorSourceSlot));
            inventory.setItem(session.defibrillatorSourceSlot, selectedStack);
        }
        inventory.selected = session.selectedSlot;
        actor.inventoryMenu.broadcastChanges();
    }

    private static ServerPlayer player(ServerPlayer reference, UUID playerId) {
        return reference.getServer() == null
                ? null
                : reference.getServer().getPlayerList().getPlayer(playerId);
    }

    private static ServerPlayer findOnlinePlayer(UUID playerId) {
        return net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer() == null
                ? null
                : net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer()
                .getPlayerList().getPlayer(playerId);
    }

    private static final class DefibrillationSession {
        private final UUID actorId;
        private final UUID patientId;
        private final DefibrillationEnergy energy;
        private final long readyGameTime;
        private final Vec3 actorStart;
        private final Vec3 patientStart;
        private final int selectedSlot;
        private final int defibrillatorSourceSlot;
        private boolean readyNotified;

        private DefibrillationSession(
                UUID actorId,
                UUID patientId,
                DefibrillationEnergy energy,
                long readyGameTime,
                Vec3 actorStart,
                Vec3 patientStart,
                int selectedSlot,
                int defibrillatorSourceSlot
        ) {
            this.actorId = actorId;
            this.patientId = patientId;
            this.energy = energy;
            this.readyGameTime = readyGameTime;
            this.actorStart = actorStart;
            this.patientStart = patientStart;
            this.selectedSlot = selectedSlot;
            this.defibrillatorSourceSlot = defibrillatorSourceSlot;
        }
    }
}
