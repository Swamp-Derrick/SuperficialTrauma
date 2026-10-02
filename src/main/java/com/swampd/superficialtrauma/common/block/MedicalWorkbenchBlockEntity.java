package com.swampd.superficialtrauma.common.block;

import com.swampd.superficialtrauma.common.crafting.MedicalWorkbenchIngredient;
import com.swampd.superficialtrauma.common.crafting.MedicalWorkbenchRecipe;
import com.swampd.superficialtrauma.common.crafting.MedicalWorkbenchRecipes;
import com.swampd.superficialtrauma.common.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MedicalWorkbenchBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int OUTPUT_SLOT_COUNT = 3;
    public static final int QUEUE_LIMIT = 4;
    public static final int QUEUE_DATA_STRIDE = 3;
    public static final int QUEUE_DATA_COUNT = QUEUE_LIMIT * QUEUE_DATA_STRIDE;

    private static final String TAG_QUEUE = "CraftingQueue";
    private static final String TAG_RECIPE = "Recipe";
    private static final String TAG_REMAINING = "RemainingTicks";
    private static final String TAG_REFUNDS = "Refunds";

    private NonNullList<ItemStack> outputs = NonNullList.withSize(OUTPUT_SLOT_COUNT, ItemStack.EMPTY);
    private final List<CraftingJob> queue = new ArrayList<>();
    @Nullable
    private Component customName;

    private final ContainerData queueData = new ContainerData() {
        @Override
        public int get(int index) {
            int queueIndex = index / QUEUE_DATA_STRIDE;
            int field = index % QUEUE_DATA_STRIDE;
            if (queueIndex < 0 || queueIndex >= queue.size()) {
                return field == 0 ? -1 : 0;
            }
            CraftingJob job = queue.get(queueIndex);
            return switch (field) {
                case 0 -> job.recipe.id();
                case 1 -> job.remainingTicks;
                case 2 -> job.recipe.craftTimeTicks();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return QUEUE_DATA_COUNT;
        }
    };

    public MedicalWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MEDICAL_WORKBENCH.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            MedicalWorkbenchBlockEntity workbench
    ) {
        if (workbench.queue.isEmpty()) {
            return;
        }

        CraftingJob first = workbench.queue.get(0);
        if (first.remainingTicks > 0) {
            first.remainingTicks--;
            if (first.remainingTicks > 0) {
                if (first.remainingTicks % 20 == 0) {
                    workbench.setChanged();
                }
                return;
            }
        }

        if (workbench.insertCompletedResult(first.recipe.resultStack())) {
            workbench.queue.remove(0);
            workbench.setChanged();
        }
    }

    public ContainerData queueData() {
        return queueData;
    }

    public boolean enqueueRecipe(Player player, MedicalWorkbenchRecipe recipe) {
        if (queue.size() >= QUEUE_LIMIT || recipe == null) {
            return false;
        }
        List<ItemStack> consumed = consumeIngredients(player.getInventory(), recipe);
        if (consumed == null) {
            return false;
        }
        queue.add(new CraftingJob(recipe, recipe.craftTimeTicks(), consumed));
        player.getInventory().setChanged();
        setChanged();
        return true;
    }

    public boolean cancelJob(int queueIndex, Player cancellingPlayer) {
        if (queueIndex < 0 || queueIndex >= queue.size()) {
            return false;
        }
        CraftingJob cancelled = queue.remove(queueIndex);
        for (ItemStack refund : cancelled.refunds) {
            giveOrDrop(cancellingPlayer, refund.copy());
        }
        cancellingPlayer.getInventory().setChanged();
        setChanged();
        return true;
    }

    public static boolean hasIngredients(Inventory inventory, MedicalWorkbenchRecipe recipe) {
        if (recipe == null) {
            return false;
        }
        Map<Item, Integer> required = aggregateRequirements(recipe);
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            Integer remaining = required.get(stack.getItem());
            if (remaining == null || remaining <= 0) {
                continue;
            }
            required.put(stack.getItem(), Math.max(0, remaining - stack.getCount()));
        }
        return required.values().stream().allMatch(count -> count <= 0);
    }

    @Override
    public Component getDisplayName() {
        return customName != null
                ? customName
                : Component.translatable("container.superficialtrauma.medical_workbench");
    }

    public void setCustomName(Component customName) {
        this.customName = customName;
        setChanged();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MedicalWorkbenchMenu(containerId, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, outputs, registries);
        ListTag queueTag = new ListTag();
        for (CraftingJob job : queue) {
            queueTag.add(job.save(registries));
        }
        tag.put(TAG_QUEUE, queueTag);
        if (customName != null) {
            tag.putString("CustomName", Component.Serializer.toJson(customName, registries));
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        outputs = NonNullList.withSize(OUTPUT_SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, outputs, registries);
        queue.clear();
        ListTag queueTag = tag.getList(TAG_QUEUE, Tag.TAG_COMPOUND);
        for (int index = 0; index < queueTag.size() && queue.size() < QUEUE_LIMIT; index++) {
            CraftingJob loaded = CraftingJob.load(queueTag.getCompound(index), registries);
            if (loaded != null) {
                queue.add(loaded);
            }
        }
        if (tag.contains("CustomName", Tag.TAG_STRING)) {
            customName = Component.Serializer.fromJson(tag.getString("CustomName"), registries);
        } else {
            customName = null;
        }
    }

    public void dropContentsAndRefunds() {
        if (level == null) {
            return;
        }
        Containers.dropContents(level, worldPosition, this);
        for (CraftingJob job : queue) {
            for (ItemStack refund : job.refunds) {
                Containers.dropItemStack(
                        level,
                        worldPosition.getX() + 0.5D,
                        worldPosition.getY() + 0.5D,
                        worldPosition.getZ() + 0.5D,
                        refund.copy()
                );
            }
        }
        outputs.clear();
        queue.clear();
        setChanged();
    }

    @Override
    public int getContainerSize() {
        return OUTPUT_SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return outputs.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return outputs.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(outputs, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(outputs, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        outputs.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
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
        outputs.clear();
        setChanged();
    }

    private boolean insertCompletedResult(ItemStack result) {
        int capacity = 0;
        for (ItemStack output : outputs) {
            if (output.isEmpty()) {
                capacity += result.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(output, result)) {
                capacity += Math.max(0, Math.min(output.getMaxStackSize(), getMaxStackSize()) - output.getCount());
            }
        }
        if (capacity < result.getCount()) {
            return false;
        }

        ItemStack remaining = result.copy();
        for (ItemStack output : outputs) {
            if (remaining.isEmpty()) {
                break;
            }
            if (!output.isEmpty() && ItemStack.isSameItemSameComponents(output, remaining)) {
                int moved = Math.min(
                        remaining.getCount(),
                        Math.min(output.getMaxStackSize(), getMaxStackSize()) - output.getCount()
                );
                if (moved > 0) {
                    output.grow(moved);
                    remaining.shrink(moved);
                }
            }
        }
        for (int slot = 0; slot < outputs.size() && !remaining.isEmpty(); slot++) {
            if (outputs.get(slot).isEmpty()) {
                int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                outputs.set(slot, remaining.copyWithCount(moved));
                remaining.shrink(moved);
            }
        }
        return remaining.isEmpty();
    }

    @Nullable
    private static List<ItemStack> consumeIngredients(Inventory inventory, MedicalWorkbenchRecipe recipe) {
        if (!hasIngredients(inventory, recipe)) {
            return null;
        }
        List<ItemStack> consumed = new ArrayList<>();
        for (MedicalWorkbenchIngredient ingredient : recipe.ingredients()) {
            int remaining = ingredient.count();
            for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (!stack.is(ingredient.item())) {
                    continue;
                }
                int taken = Math.min(remaining, stack.getCount());
                consumed.add(stack.copyWithCount(taken));
                stack.shrink(taken);
                remaining -= taken;
            }
        }
        return consumed;
    }

    private static Map<Item, Integer> aggregateRequirements(MedicalWorkbenchRecipe recipe) {
        Map<Item, Integer> required = new HashMap<>();
        for (MedicalWorkbenchIngredient ingredient : recipe.ingredients()) {
            required.merge(ingredient.item(), ingredient.count(), Integer::sum);
        }
        return required;
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        player.getInventory().add(stack);
        if (!stack.isEmpty()) {
            player.drop(stack, false);
        }
    }

    private static final class CraftingJob {
        private final MedicalWorkbenchRecipe recipe;
        private int remainingTicks;
        private final List<ItemStack> refunds;

        private CraftingJob(
                MedicalWorkbenchRecipe recipe,
                int remainingTicks,
                List<ItemStack> refunds
        ) {
            this.recipe = recipe;
            this.remainingTicks = Math.max(0, Math.min(recipe.craftTimeTicks(), remainingTicks));
            this.refunds = List.copyOf(refunds);
        }

        private CompoundTag save(net.minecraft.core.HolderLookup.Provider registries) {
            CompoundTag tag = new CompoundTag();
            tag.putString(TAG_RECIPE, recipe.key());
            tag.putInt(TAG_REMAINING, remainingTicks);
            ListTag refundTag = new ListTag();
            for (ItemStack refund : refunds) {
                refundTag.add(refund.save(registries));
            }
            tag.put(TAG_REFUNDS, refundTag);
            return tag;
        }

        @Nullable
        private static CraftingJob load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            MedicalWorkbenchRecipe recipe = tag.contains(TAG_RECIPE, Tag.TAG_STRING)
                    ? MedicalWorkbenchRecipes.byKey(tag.getString(TAG_RECIPE))
                    : MedicalWorkbenchRecipes.byId(tag.getInt(TAG_RECIPE));
            if (recipe == null) {
                return null;
            }
            List<ItemStack> refunds = new ArrayList<>();
            ListTag refundTag = tag.getList(TAG_REFUNDS, Tag.TAG_COMPOUND);
            for (int index = 0; index < refundTag.size(); index++) {
                ItemStack stack = ItemStack.parseOptional(registries, refundTag.getCompound(index));
                if (!stack.isEmpty()) {
                    refunds.add(stack);
                }
            }
            return new CraftingJob(recipe, tag.getInt(TAG_REMAINING), refunds);
        }
    }
}
