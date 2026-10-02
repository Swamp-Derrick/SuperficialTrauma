package com.swampd.superficialtrauma.common.block;

import com.swampd.superficialtrauma.common.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public final class MedicalWorkbenchBlock extends MedicalEquipmentBlock implements EntityBlock {
    public static final com.mojang.serialization.MapCodec<MedicalWorkbenchBlock> CODEC = simpleCodec(MedicalWorkbenchBlock::new);
    @Override
    protected com.mojang.serialization.MapCodec<MedicalWorkbenchBlock> codec() { return CODEC; }
    public MedicalWorkbenchBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MedicalWorkbenchBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> blockEntityType
    ) {
        if (level.isClientSide || blockEntityType != ModBlockEntities.MEDICAL_WORKBENCH.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) ->
                MedicalWorkbenchBlockEntity.serverTick(
                        tickerLevel,
                        pos,
                        tickerState,
                        (MedicalWorkbenchBlockEntity) blockEntity
                );
    }

    @Override
    public InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MedicalWorkbenchBlockEntity workbench) {
                serverPlayer.openMenu(workbench, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MedicalWorkbenchBlockEntity workbench) {
                workbench.setCustomName(stack.getHoverName());
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!level.isClientSide && blockEntity instanceof MedicalWorkbenchBlockEntity workbench) {
                workbench.dropContentsAndRefunds();
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
