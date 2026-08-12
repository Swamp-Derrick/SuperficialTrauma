package com.swampd.superficialtrauma.common.block;

import com.swampd.superficialtrauma.common.init.ModBlocks;
import com.swampd.superficialtrauma.common.init.ModItems;
import com.swampd.superficialtrauma.common.init.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class DefibrillatorStationMenu extends AbstractContainerMenu {
    public static final int STATION_SLOT_COUNT = 2;
    private static final int PLAYER_INVENTORY_START = STATION_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 36;

    private final Container station;
    private final ContainerLevelAccess access;

    public static DefibrillatorStationMenu fromNetwork(
            int containerId,
            Inventory playerInventory,
            FriendlyByteBuf buffer
    ) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        Container station = blockEntity instanceof DefibrillatorStationBlockEntity
                ? (Container) blockEntity
                : new SimpleContainer(STATION_SLOT_COUNT);
        return new DefibrillatorStationMenu(
                containerId,
                playerInventory,
                station,
                ContainerLevelAccess.create(playerInventory.player.level(), pos)
        );
    }

    public DefibrillatorStationMenu(
            int containerId,
            Inventory playerInventory,
            DefibrillatorStationBlockEntity station
    ) {
        this(
                containerId,
                playerInventory,
                station,
                ContainerLevelAccess.create(station.getLevel(), station.getBlockPos())
        );
    }

    private DefibrillatorStationMenu(
            int containerId,
            Inventory playerInventory,
            Container station,
            ContainerLevelAccess access
    ) {
        super(ModMenus.DEFIBRILLATOR_STATION.get(), containerId);
        checkContainerSize(station, STATION_SLOT_COUNT);
        this.station = station;
        this.access = access;

        station.startOpen(playerInventory.player);
        addSlot(new DefibrillatorSlot(station, 0, 53, 32));
        addSlot(new DefibrillatorSlot(station, 1, 107, 32));
        addPlayerInventory(playerInventory);
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 94 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 152));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.DEFIBRILLATOR_STATION.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int menuSlotIndex) {
        if (menuSlotIndex < 0 || menuSlotIndex >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(menuSlotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack source = slot.getItem();
        ItemStack original = source.copy();
        if (menuSlotIndex < STATION_SLOT_COUNT) {
            if (!moveItemStackTo(source, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!source.is(ModItems.DEFIBRILLATOR.get())
                    || !moveItemStackTo(source, 0, STATION_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (source.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (source.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, source);
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        station.stopOpen(player);
    }

    private static final class DefibrillatorSlot extends Slot {
        private DefibrillatorSlot(Container station, int slot, int x, int y) {
            super(station, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(ModItems.DEFIBRILLATOR.get());
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
