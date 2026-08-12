package com.swampd.superficialtrauma.common.block;

import com.swampd.superficialtrauma.common.crafting.MedicalWorkbenchRecipe;
import com.swampd.superficialtrauma.common.crafting.MedicalWorkbenchRecipes;
import com.swampd.superficialtrauma.common.init.ModBlocks;
import com.swampd.superficialtrauma.common.init.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class MedicalWorkbenchMenu extends AbstractContainerMenu {
    public static final int CRAFT_BUTTON_BASE = 100;
    public static final int CANCEL_BUTTON_BASE = 200;

    private static final int PLAYER_SLOT_START = MedicalWorkbenchBlockEntity.OUTPUT_SLOT_COUNT;
    private static final int PLAYER_SLOT_END = PLAYER_SLOT_START + 36;

    private final Container workbench;
    private final ContainerData queueData;
    private final ContainerLevelAccess access;

    public static MedicalWorkbenchMenu fromNetwork(
            int containerId,
            Inventory playerInventory,
            FriendlyByteBuf buffer
    ) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        Container workbench = blockEntity instanceof MedicalWorkbenchBlockEntity
                ? (Container) blockEntity
                : new SimpleContainer(MedicalWorkbenchBlockEntity.OUTPUT_SLOT_COUNT);
        SimpleContainerData queueData = new SimpleContainerData(MedicalWorkbenchBlockEntity.QUEUE_DATA_COUNT);
        for (int queueIndex = 0; queueIndex < MedicalWorkbenchBlockEntity.QUEUE_LIMIT; queueIndex++) {
            queueData.set(queueIndex * MedicalWorkbenchBlockEntity.QUEUE_DATA_STRIDE, -1);
        }
        return new MedicalWorkbenchMenu(
                containerId,
                playerInventory,
                workbench,
                queueData,
                ContainerLevelAccess.create(playerInventory.player.level(), pos)
        );
    }

    public MedicalWorkbenchMenu(
            int containerId,
            Inventory playerInventory,
            MedicalWorkbenchBlockEntity workbench
    ) {
        this(
                containerId,
                playerInventory,
                workbench,
                workbench.queueData(),
                ContainerLevelAccess.create(workbench.getLevel(), workbench.getBlockPos())
        );
    }

    private MedicalWorkbenchMenu(
            int containerId,
            Inventory playerInventory,
            Container workbench,
            ContainerData queueData,
            ContainerLevelAccess access
    ) {
        super(ModMenus.MEDICAL_WORKBENCH.get(), containerId);
        checkContainerSize(workbench, MedicalWorkbenchBlockEntity.OUTPUT_SLOT_COUNT);
        checkContainerDataCount(queueData, MedicalWorkbenchBlockEntity.QUEUE_DATA_COUNT);
        this.workbench = workbench;
        this.queueData = queueData;
        this.access = access;

        workbench.startOpen(playerInventory.player);
        addSlot(new OutputSlot(workbench, 0, 239, 22));
        addSlot(new OutputSlot(workbench, 1, 257, 22));
        addSlot(new OutputSlot(workbench, 2, 275, 22));
        addHiddenPlayerInventory(playerInventory);
        addDataSlots(queueData);
    }

    private void addHiddenPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, -1000, -1000));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, -1000, -1000));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.MEDICAL_WORKBENCH.get());
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (!(workbench instanceof MedicalWorkbenchBlockEntity blockEntity) || !stillValid(player)) {
            return false;
        }
        if (buttonId >= CRAFT_BUTTON_BASE
                && buttonId < CRAFT_BUTTON_BASE + MedicalWorkbenchRecipes.all().size()) {
            MedicalWorkbenchRecipe recipe = MedicalWorkbenchRecipes.byId(buttonId - CRAFT_BUTTON_BASE);
            boolean queued = blockEntity.enqueueRecipe(player, recipe);
            if (queued) {
                broadcastChanges();
            }
            return queued;
        }
        if (buttonId >= CANCEL_BUTTON_BASE
                && buttonId < CANCEL_BUTTON_BASE + MedicalWorkbenchBlockEntity.QUEUE_LIMIT) {
            boolean cancelled = blockEntity.cancelJob(buttonId - CANCEL_BUTTON_BASE, player);
            if (cancelled) {
                broadcastChanges();
            }
            return cancelled;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int menuSlotIndex) {
        if (menuSlotIndex < 0 || menuSlotIndex >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(menuSlotIndex);
        if (!slot.hasItem() || menuSlotIndex >= MedicalWorkbenchBlockEntity.OUTPUT_SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        ItemStack source = slot.getItem();
        ItemStack original = source.copy();
        if (!moveItemStackTo(source, PLAYER_SLOT_START, PLAYER_SLOT_END, true)) {
            return ItemStack.EMPTY;
        }
        if (source.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, source);
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        workbench.stopOpen(player);
    }

    public int queuedRecipeId(int queueIndex) {
        return queueData.get(queueIndex * MedicalWorkbenchBlockEntity.QUEUE_DATA_STRIDE);
    }

    public int queuedRemainingTicks(int queueIndex) {
        return queueData.get(queueIndex * MedicalWorkbenchBlockEntity.QUEUE_DATA_STRIDE + 1);
    }

    public int queuedTotalTicks(int queueIndex) {
        return queueData.get(queueIndex * MedicalWorkbenchBlockEntity.QUEUE_DATA_STRIDE + 2);
    }

    public boolean isQueueFull() {
        return queuedRecipeId(MedicalWorkbenchBlockEntity.QUEUE_LIMIT - 1) >= 0;
    }

    private static final class OutputSlot extends Slot {
        private OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
