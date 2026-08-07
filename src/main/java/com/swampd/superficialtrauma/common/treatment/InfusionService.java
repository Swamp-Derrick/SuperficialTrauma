package com.swampd.superficialtrauma.common.treatment;

import com.swampd.superficialtrauma.common.body.BodyState;
import com.swampd.superficialtrauma.common.body.BodyStateCapability;
import com.swampd.superficialtrauma.common.body.InfusionType;
import com.swampd.superficialtrauma.common.init.ModItems;
import com.swampd.superficialtrauma.common.sound.MedicalActionSound;
import com.swampd.superficialtrauma.common.sound.MedicalActionSoundService;
import com.swampd.superficialtrauma.network.ModNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class InfusionService {
    private static final double MAX_DISTANCE_SQUARED = 4.5D * 4.5D;

    private InfusionService() {
    }

    public static boolean start(ServerPlayer actor, int patientEntityId, InfusionType type) {
        if (type == null || type == InfusionType.NONE
                || !(actor.serverLevel().getEntity(patientEntityId) instanceof ServerPlayer patient)
                || !canStart(actor, patient)) {
            actor.displayClientMessage(
                    Component.translatable("message.superficialtrauma.infusion.invalid_target"),
                    true
            );
            return false;
        }

        Item requiredItem = requiredItem(type);
        if (requiredItem == null || !hasItem(actor, requiredItem)) {
            actor.displayClientMessage(
                    Component.translatable("message.superficialtrauma.infusion.item_missing"),
                    true
            );
            return false;
        }

        BodyState state = BodyStateCapability.get(patient).orElse(null);
        if (state == null || state.hasActiveInfusion()) {
            actor.displayClientMessage(
                    Component.translatable("message.superficialtrauma.infusion.already_active"),
                    true
            );
            return false;
        }
        long gameTime = actor.serverLevel().getGameTime();
        if (!state.startInfusion(type, gameTime)) {
            actor.displayClientMessage(
                    Component.translatable("message.superficialtrauma.infusion.invalid_target"),
                    true
            );
            return false;
        }

        consumeOne(actor, requiredItem);
        state.recordResuscitationContributor(
                actor.getUUID(),
                actor.getGameProfile().getName()
        );
        ModNetworking.syncBodyState(patient);
        InspectionService.syncPatient(patient);
        MedicalActionSoundService.playOnce(actor, patient, MedicalActionSound.LIQUID_POUCH);
        actor.displayClientMessage(
                Component.translatable("message.superficialtrauma.infusion.started", Component.translatable(type.translationKey())),
                true
        );
        return true;
    }

    private static boolean canStart(ServerPlayer actor, ServerPlayer patient) {
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
        boolean actorCanAct = BodyStateCapability.get(actor).map(BodyState::canAct).orElse(false);
        boolean patientDowned = BodyStateCapability.get(patient)
                .map(state -> !state.canAct())
                .orElse(false);
        return actorCanAct && patientDowned;
    }

    private static Item requiredItem(InfusionType type) {
        return switch (type) {
            case BLOOD_BAG -> ModItems.BLOOD_BAG.get();
            case SALINE -> ModItems.SALINE_SOLUTION.get();
            case NONE -> null;
        };
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
                inventory.setChanged();
                actor.inventoryMenu.broadcastChanges();
                return;
            }
        }
    }
}
