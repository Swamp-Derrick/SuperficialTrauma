package com.swampd.superficialtrauma.common.block;

import com.swampd.superficialtrauma.common.init.ModBlockEntities;
import com.swampd.superficialtrauma.common.init.ModItems;
import com.swampd.superficialtrauma.common.item.DefibrillatorItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.Containers;

import javax.annotation.Nullable;

public final class DefibrillatorStationBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int SLOT_COUNT = 2;
    public static final int ENERGY_PER_SECOND = 3;
    private static final int CHARGE_INTERVAL_TICKS = 20;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int chargeTicks;
    @Nullable
    private Component customName;

    public DefibrillatorStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DEFIBRILLATOR_STATION.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            DefibrillatorStationBlockEntity station
    ) {
        station.chargeTicks++;
        if (station.chargeTicks < CHARGE_INTERVAL_TICKS) {
            return;
        }
        station.chargeTicks = 0;

        boolean changed = false;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = station.items.get(slot);
            if (!stack.is(ModItems.DEFIBRILLATOR.get())) {
                continue;
            }
            int currentEnergy = DefibrillatorItem.getEnergy(stack);
            if (currentEnergy >= DefibrillatorItem.MAX_ENERGY) {
                continue;
            }
            DefibrillatorItem.setEnergy(
                    stack,
                    Math.min(DefibrillatorItem.MAX_ENERGY, currentEnergy + ENERGY_PER_SECOND)
            );
            changed = true;
        }
        if (changed) {
            station.setChanged();
        }
    }

    @Override
    public Component getDisplayName() {
        return customName != null
                ? customName
                : Component.translatable("container.superficialtrauma.defibrillator_station");
    }

    public void setCustomName(Component customName) {
        this.customName = customName;
        setChanged();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new DefibrillatorStationMenu(containerId, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("ChargeTicks", chargeTicks);
        if (customName != null) {
            tag.putString("CustomName", Component.Serializer.toJson(customName, registries));
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        chargeTicks = Math.max(0, Math.min(CHARGE_INTERVAL_TICKS - 1, tag.getInt("ChargeTicks")));
        if (tag.contains("CustomName", Tag.TAG_STRING)) {
            customName = Component.Serializer.fromJson(tag.getString("CustomName"), registries);
        } else {
            customName = null;
        }
    }

    public void dropContents() {
        if (level != null) {
            Containers.dropContents(level, worldPosition, this);
            clearContent();
        }
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.is(ModItems.DEFIBRILLATOR.get());
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D
        ) <= 64.0D;
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }
}
