package com.swampd.superficialtrauma.common.loot;

import com.swampd.superficialtrauma.common.init.ModMenus;
import com.swampd.superficialtrauma.common.entity.CorpseEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class LootTargetMenu extends AbstractContainerMenu {
    public static final int TARGET_SLOT_COUNT = 41;
    public static final int TAKE_ALL_BUTTON_ID = 0;

    private static final int PLAYER_SLOT_COUNT = 36;
    private static final int PLAYER_SLOT_START = TARGET_SLOT_COUNT;
    private static final int PLAYER_SLOT_END = PLAYER_SLOT_START + PLAYER_SLOT_COUNT;

    private final Container targetInventory;
    private final int targetEntityId;
    private final Entity targetEntity;
    private final UUID targetId;
    private final boolean corpseTarget;

    public static LootTargetMenu fromNetwork(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        int targetEntityId = buffer.readVarInt();
        boolean corpseTarget = buffer.readBoolean();
        return new LootTargetMenu(
                containerId,
                playerInventory,
                new SimpleContainer(TARGET_SLOT_COUNT),
                targetEntityId,
                null,
                corpseTarget
        );
    }

    public LootTargetMenu(int containerId, Inventory playerInventory, ServerPlayer targetPlayer) {
        this(
                containerId,
                playerInventory,
                targetPlayer.getInventory(),
                targetPlayer.getId(),
                targetPlayer,
                false
        );
    }

    public LootTargetMenu(int containerId, Inventory playerInventory, CorpseEntity corpse) {
        this(
                containerId,
                playerInventory,
                corpse,
                corpse.getId(),
                corpse,
                true
        );
    }

    private LootTargetMenu(
            int containerId,
            Inventory playerInventory,
            Container targetInventory,
            int targetEntityId,
            Entity targetEntity,
            boolean corpseTarget
    ) {
        super(ModMenus.LOOT_TARGET.get(), containerId);
        checkContainerSize(targetInventory, TARGET_SLOT_COUNT);
        this.targetInventory = targetInventory;
        this.targetEntityId = targetEntityId;
        this.targetEntity = targetEntity;
        this.targetId = targetEntity == null ? null : targetEntity.getUUID();
        this.corpseTarget = corpseTarget;

        addTargetSlots(targetInventory);
        addPlayerSlots(playerInventory);
    }

    private void addTargetSlots(Container inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int containerSlot = column + row * 9 + 9;
                addSlot(new LootTargetSlot(inventory, containerSlot, 8 + column * 18, 22 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new LootTargetSlot(inventory, column, 8 + column * 18, 82));
        }
        for (int row = 0; row < 4; row++) {
            int armorContainerSlot = 39 - row;
            addSlot(new LootTargetSlot(inventory, armorContainerSlot, 178, 22 + row * 18));
        }
        addSlot(new LootTargetSlot(inventory, 40, 178, 94));
    }

    private void addPlayerSlots(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int containerSlot = column + row * 9 + 9;
                addSlot(new Slot(inventory, containerSlot, 8 + column * 18, 144 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 202));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return targetEntity == null || LootingService.canContinueLooting(player, targetEntity);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int menuSlotIndex) {
        if (menuSlotIndex < 0 || menuSlotIndex >= slots.size() || !stillValid(player)) {
            return ItemStack.EMPTY;
        }

        Slot slot = slots.get(menuSlotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = slot.getItem();
        ItemStack originalStack = sourceStack.copy();
        if (menuSlotIndex < TARGET_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, PLAYER_SLOT_START, PLAYER_SLOT_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (sourceStack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (sourceStack.getCount() == originalStack.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, sourceStack);
        syncTargetOwnerInventory();
        return originalStack;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId != TAKE_ALL_BUTTON_ID || !stillValid(player)) {
            return false;
        }

        if (targetEntity instanceof CorpseEntity && player instanceof ServerPlayer serverPlayer) {
            CorpseEquipmentTransfer.equipUpgrades(serverPlayer, targetInventory);
        }
        for (int targetSlot = 0; targetSlot < TARGET_SLOT_COUNT; targetSlot++) {
            quickMoveStack(player, targetSlot);
        }
        broadcastChanges();
        syncTargetOwnerInventory();
        return true;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        syncTargetOwnerInventory();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (targetId != null) {
            LootingService.release(targetId, player.getUUID());
        }
    }

    public boolean hasLootableItems() {
        for (int menuSlot = 0; menuSlot < TARGET_SLOT_COUNT; menuSlot++) {
            if (slots.get(menuSlot).hasItem()) {
                return true;
            }
        }
        return false;
    }

    public int targetEntityId() {
        return targetEntityId;
    }

    public UUID targetId() {
        return targetId;
    }

    public boolean isCorpseTarget() {
        return corpseTarget;
    }

    private void syncTargetOwnerInventory() {
        targetInventory.setChanged();
        if (targetEntity instanceof ServerPlayer targetPlayer) {
            targetPlayer.inventoryMenu.broadcastChanges();
        }
    }
}
